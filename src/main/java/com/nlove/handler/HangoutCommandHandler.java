package com.nlove.handler;

import java.awt.EventQueue;
import java.io.IOException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import javax.swing.JTextArea;

import org.apache.commons.lang3.StringUtils;
import org.iota.jota.IotaAPI;
import org.iota.jota.IotaAccount;
import org.iota.jota.builder.AccountBuilder;
import org.iota.jota.dto.response.GetNodeInfoResponse;
import org.iota.jota.error.ArgumentException;
import org.iota.jota.model.Transaction;
import org.iota.jota.pow.pearldiver.PearlDiverLocalPoW;
import org.iota.jota.utils.TrytesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlove.config.NloveConfig;
import com.nlove.config.NloveConfigManager;
import com.nlove.config.NloveProfileManager;
import com.nlove.hangout.message.NloveHangoutMessage;
import com.nlove.iota.util.IotaNodeHelper;
import com.nlove.iota.util.IotaSeedGenerator;
import com.nlove.rsa.RsaUtil;

public class HangoutCommandHandler {

    private static final String NLOVE_HANGOUT_TAG = "NLOVEHANGOUT999999999999999";
    private static final String NLOVE_HANGOUT_ADDRESS = "BKWXI9RULFEZZHCPP9BUITKSJZQLFRHCIMYXS9HVUHWJVNBKDGTGHEDCJOPMRXBWPHOAEKXNUHDLCVUIC9AEUCLSPZ";
    private IotaAPI iotaApi;
    private IotaAccount iotaAccount;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final Logger LOG = LoggerFactory.getLogger(HangoutCommandHandler.class);
    private static final int DEVNET_MWM = 9;
    private static final int MAINNET_MWM = 14;
    private JTextArea textAreaMsg;
    private JTextArea textAreaMsgs;
    private AccountBuilder accountBuilder;
    private IotaNodeHelper iotaNodeHelper;
    private Thread zmqSubThread;
    private Socket zmqSocket;
    private Context zmqContext;
    private NloveConfigManager configManager;
    private ObjectMapper mapper = new ObjectMapper();
    private MessageDigest messageDigestMd5;

    public JTextArea getTextAreaMsgs() {
        return textAreaMsgs;
    }

    public void setTextAreaMsg(JTextArea textAreaMsgs) {
        this.textAreaMsgs = textAreaMsgs;
    }

    public void setTextAreaMsgs(JTextArea textAreaMsgs) {
        this.textAreaMsgs = textAreaMsgs;
    }

    public void start() throws Exception {

        this.messageDigestMd5 = MessageDigest.getInstance("MD5");

        this.configManager = new NloveConfigManager();
        if (this.configManager.isEmpty()) {
            NloveConfig config = new NloveConfig();
            KeyPair kp = RsaUtil.generateKeyPair();
            config.setRsaPrivateKeyBytes(kp.getPrivate().getEncoded());
            config.setRsaPublicKeyBytes(kp.getPublic().getEncoded());
            this.configManager.setConfig(config);
            this.configManager.save();
        }

        this.iotaNodeHelper = new IotaNodeHelper();

        String iotaApiNode = this.iotaNodeHelper.getConfig().getApiHttpsHostname();
        this.iotaApi = new IotaAPI.Builder().protocol("https").host(iotaApiNode).port(443).localPoW(new PearlDiverLocalPoW()).build();
        GetNodeInfoResponse response;
        try {
            response = iotaApi.getNodeInfo();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not connect to IOTA API node %s, error: ", iotaApiNode, e.toString()));
        }

        if (Math.abs(response.getLatestMilestoneIndex() - response.getLatestSolidSubtangleMilestoneIndex()) > 3) {
            throw new RuntimeException("IOTA node seems out of sync!");
        }

        accountBuilder = new IotaAccount.Builder(IotaSeedGenerator.generateSeed()).api(iotaApi).mwm(MAINNET_MWM);
        this.iotaAccount = accountBuilder.build();

        this.displayMessages();

    }

    public void stop() {
        if (this.zmqSocket != null) {
            zmqContext.close();
            try {
                zmqSubThread.interrupt();
                zmqSubThread.join();
            } catch (InterruptedException e) {
            }
        }
        this.zmqSubThread.interrupt();
        this.iotaAccount.shutdown();
        this.iotaAccount = null;
        this.iotaApi = null;
    }

    public void sendMessage(String text) {

        try {
            NloveHangoutMessage hangoutMessage = new NloveHangoutMessage();
            hangoutMessage.setUsername(NloveProfileManager.INSTANCE.getProfile().getUsername());
            hangoutMessage.setPublicKeyBytes(this.configManager.getConfig().getRsaPublicKeyBytes());
            hangoutMessage.setMsg(text);
            hangoutMessage.setSignature(RsaUtil.sign(hangoutMessage.getSigningString(), RsaUtil.privateKeyFromBytes(this.configManager.getConfig().getRsaPrivateKeyBytes())));
            String msgJson = this.mapper.writeValueAsString(hangoutMessage);
            this.iotaAccount.sendZeroValue(msgJson, NLOVE_HANGOUT_TAG, NLOVE_HANGOUT_ADDRESS);

            textAreaMsgs.append(String.format("Sent message \"%s\" to IOTA network, please wait for confirmation...%s", text, System.getProperty("line.separator")));

        } catch (Exception e) {
            // TODO Auto-generated catch block
            textAreaMsgs.append("Failed to send message: " + e.toString());
            LOG.error("Failed to send message", e);
        }
    }

    private void displayMessages() {
        List<Transaction> txes = iotaApi.findTransactionObjectsByAddresses(new String[] { NLOVE_HANGOUT_ADDRESS });

        txes = txes.stream().sorted((t1, t2) -> Long.compare(t1.getTimestamp(), t2.getTimestamp())).collect(Collectors.toList());

        StringBuilder resString = new StringBuilder();

        if (txes.isEmpty()) {
            resString.append("No messages yet!");
        }

        for (Transaction transaction : txes) {
            if (!transaction.getTag().equals(NLOVE_HANGOUT_TAG)) {
                continue;
            }

            String txLine = getTxLine(transaction);
            if (txLine == null) {
                continue;
            }

            resString.append(txLine);
            resString.append(System.getProperty("line.separator"));
        }

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                textAreaMsgs.setText(resString.toString());
            }
        });
        // live feed
        zmqContext = ZMQ.context(1);
        zmqSocket = zmqContext.socket(SocketType.SUB);
        String iotaZMQNode = this.iotaNodeHelper.getConfig().getZmqUrl();

        try {
            zmqSocket.connect(iotaZMQNode);
            zmqSocket.subscribe(NLOVE_HANGOUT_ADDRESS.substring(0, 81));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not connect to IOTA ZMQ node %s, error: %s", iotaZMQNode, e.toString()));
        }

        // Runs inside of the Swing UI thread
        this.zmqSubThread = new Thread() {
            public void run() {
                Thread.currentThread().setName("HangoutCommandHandler - ZMQSub");

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        byte[] recv = zmqSocket.recv(0);
                        String recvString = new String(recv);
                        String txId = recvString.split(" ")[1];
                        List<Transaction> txEs = iotaApi.findTransactionsObjectsByHashes(new String[] { txId });
                        String txLine = getTxLine(txEs.get(0));
                        if (txLine == null) {
                            continue;
                        }

                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                textAreaMsgs.setText(textAreaMsgs.getText() + txLine + System.getProperty("line.separator"));
                            }
                        });
                    } catch (ZMQException e) {
                        if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                            break;
                        }
                    }
                }

                zmqSocket.setLinger(0);
                zmqSocket.close();
            }

        };
        zmqSubThread.start();
    }

    private String getTxLine(Transaction transaction) {
        String msgText;

        try {
            msgText = TrytesConverter.trytesToAscii(StringUtils.stripEnd(transaction.getSignatureFragments(), "9"));
        } catch (ArgumentException e) {
            return null;
        }

        try {

            NloveHangoutMessage signedMsg = this.mapper.readValue(msgText, NloveHangoutMessage.class);
            if (signedMsg.getUsername() == null || signedMsg.getUsername().isEmpty()) {
                return null;
            }
            if (signedMsg.getMsg() == null || signedMsg.getMsg().isEmpty()) {
                return null;
            }

            String date = Instant.ofEpochSecond(transaction.getTimestamp()).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ");

            messageDigestMd5.update(signedMsg.getPublicKeyBytes());
            byte[] digest = messageDigestMd5.digest();
            StringBuffer sbFrom = new StringBuffer();
            for (byte b : digest) {
                sbFrom.append(String.format("%02x", b & 0xff));
            }

            String from = String.format("%s.%s", signedMsg.getUsername(), sbFrom.toString());

            try {
                Boolean verified = RsaUtil.verify(signedMsg.getSigningString(), signedMsg.getSignature(), RsaUtil.publicKeyFromBytes(signedMsg.getPublicKeyBytes()));
                if (!verified) {
                    return null;
                }

            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            } catch (InvalidKeySpecException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }

            return String.format("[%s] %s%s%s", date, from, System.getProperty("line.separator"), signedMsg.getMsg());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
