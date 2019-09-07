package com.nlove.handler;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.nlove.config.NloveProfileManager;
import com.nlove.gui.ShowMatchDialog;
import com.nlove.message.NloveMessageConverter;
import com.nlove.message.NloveMessageInterface;
import com.nlove.message.NloveRequestUserProfileReplyMessage;
import com.nlove.message.NloveRequestUserProfileRequestMessage;

import jsmith.nknsdk.client.Identity;
import jsmith.nknsdk.client.NKNClient;
import jsmith.nknsdk.client.NKNClient.ReceivedMessage;
import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.client.NKNClientException.MessageAckTimeout;
import jsmith.nknsdk.client.NKNExplorer;
import jsmith.nknsdk.client.NKNExplorer.Subscription.Subscriber;
import jsmith.nknsdk.client.NKNExplorer.Subscription.SubscriptionDetail;
import jsmith.nknsdk.client.NKNExplorerException;
import jsmith.nknsdk.wallet.Wallet;
import jsmith.nknsdk.wallet.WalletException;

public class ClientCommandHandler {

    private NKNClient client;
    private Identity identity;
    private Wallet wallet;
    private static Integer previousHeight = 0;
    public static String LOBBY_TOPIC = "nlove-lobby5";
    private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("CLIENT");
    private Random rnd = new Random();
    private static final Logger LOG = LoggerFactory.getLogger(ClientCommandHandler.class);
    private ByteString sigHash;
    ScheduledExecutorService executorService;

    public void start() throws WalletException, NKNClientException, NKNExplorerException {

        File walletFile = new File("wallet.dat");

        if (!walletFile.exists())
            Wallet.createNew().save(walletFile, "");

        final Wallet wallet = Wallet.load(walletFile, "");
        this.wallet = wallet;

        this.identity = new Identity(NloveProfileManager.INSTANCE.getProfile().getUsername(), wallet);

        this.client = new NKNClient(this.identity);

        this.client.onNewMessageWithReply(msg -> {
            return this.handle(msg);
        }).start();

        this.subscribe();
        executorService = Executors.newScheduledThreadPool(1);

        this.resubscribeChecker();

    }

    public void stopClient() {

        try {
            String txID = this.wallet.tx().unsubscribe(LOBBY_TOPIC, this.identity.name, new BigDecimal(0));
            if (txID != null) {
                LOG.info("Unsubscribe successful, txID: " + txID);
            }
        } catch (WalletException e) {
            LOG.error("stopClient failed", e);
        }

        this.client.close();
    }

    public void subscribe() throws WalletException, NKNExplorerException {

        LOG.info("Subscribing to topic " + LOBBY_TOPIC);

        SubscriptionDetail subscriptionDetail = NKNExplorer.Subscription.getSubscriptionDetail(LOBBY_TOPIC, this.identity.getFullIdentifier());
        if (subscriptionDetail != null) {
            LOG.info("Already subscribed to topic {}, will not subscribe now! ", LOBBY_TOPIC);
            return;
        }

        String txID = this.wallet.tx().subscribe(LOBBY_TOPIC, 30, this.identity.name, (String) null);
        if (txID != null) {
            LOG.info("Subscription successful, txID: " + txID);
        }
    }

    /**
     * TODO: should be dynamic when new block arrives
     * 
     * @throws WalletException
     * @throws NKNExplorerException
     */
    private void resubscribeChecker() throws WalletException, NKNExplorerException {

        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    ByteString fetchedSigHash = client.getCurrentSigChainBlockHash();
                    if (sigHash == null) {
                        sigHash = fetchedSigHash;
                        return;
                    }
                    if (fetchedSigHash != null && sigHash != fetchedSigHash) {
                        sigHash = fetchedSigHash;
                        LOG.debug("New sigHash {}", Hex.toHexString(fetchedSigHash.toByteArray()));
                        subscribe();
                    }

                } catch (Exception e) {
                    LOG.error("error in resubscribeChecker", e);
                }
            }
        }, 0, 20, TimeUnit.SECONDS);

    }

    private String handle(ReceivedMessage receivedMessage) {
        NloveMessageInterface c = this.nloveMessageConverter.parseMsg(receivedMessage);

        if (c instanceof NloveRequestUserProfileRequestMessage) {
            NloveRequestUserProfileRequestMessage parsedMsg = (NloveRequestUserProfileRequestMessage) c;

            return this.nloveMessageConverter.toMsgString(new NloveRequestUserProfileReplyMessage() {
                {
                    setProfile(NloveProfileManager.INSTANCE.getProfile());
                }
            });
        }

        return null;
    }

    public void roll() throws WalletException, InterruptedException, NKNExplorerException {
        Boolean doTry = true;
        String msgString = this.nloveMessageConverter.toMsgString(new NloveRequestUserProfileRequestMessage());

        while (doTry) {
            int subscriberCount = NKNExplorer.Subscription.getSubscriberCount(LOBBY_TOPIC);
            int subscriberIndex = (subscriberCount > 1 ? rnd.nextInt(subscriberCount - 1) : 0);

            LinkedList<Subscriber> subscribers = new LinkedList<Subscriber>(
                    Arrays.asList(NKNExplorer.Subscription.getSubscribers(ClientCommandHandler.LOBBY_TOPIC, subscriberIndex, 1, false, true)));

            subscribers.removeIf(s -> s.fullClientIdentifier.equals(this.identity.getFullIdentifier()));

            if (subscriberCount == 0 || subscribers.size() == 0) {
                LOG.info("No profiles online currently, retrying in 1 second...");
                Thread.sleep(1000);
                continue;
            }

            Subscriber pickedSubscriber = subscribers.get(0);
            LOG.info("Picked random nlove user (subscriber #1): {}", subscriberIndex, pickedSubscriber.fullClientIdentifier);

            int tries = 0;

            do {
                tries++;
                try {
                    LOG.info("Trying to request user profile, please wait (Try #{}) ...", tries);
                    final CompletableFuture<NKNClient.ReceivedMessage> promise = this.client.sendTextMessageAsync(pickedSubscriber.fullClientIdentifier, msgString);

                    ReceivedMessage response = promise.join();

                    NloveRequestUserProfileReplyMessage replyMsg = (NloveRequestUserProfileReplyMessage) this.nloveMessageConverter.parseMsg(response);

                    ShowMatchDialog dialog = new ShowMatchDialog(replyMsg.getProfile(), null, true);
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.setVisible(true);

                    return;
                } catch (CompletionException e) {
                    if (e.getCause() instanceof MessageAckTimeout) {
                        doTry = true;
                        LOG.warn("User {} did not reply to profile request (offline or slow?) ...", pickedSubscriber.fullClientIdentifier);
                    } else {
                        doTry = false;
                        LOG.error("Error: {}", e);
                    }
                }
            } while (tries < 3);
        }
    }
}
