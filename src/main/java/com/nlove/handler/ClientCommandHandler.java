package com.nlove.handler;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.swing.JDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import jsmith.nknsdk.client.NKNExplorer.Subscriber;
import jsmith.nknsdk.network.NknHttpApiException;
import jsmith.nknsdk.wallet.Wallet;
import jsmith.nknsdk.wallet.WalletException;

public class ClientCommandHandler {

    private NKNClient client;
    private Identity identity;
    private Wallet wallet;
    private static Integer previousHeight = 0;
    private static Duration subscribeDuration = Duration.ofHours(24);
    public static String LOBBY_TOPIC = "nlove-lobby5";
    private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("CLIENT");
    private Random rnd = new Random();

    private static final Logger LOG = LoggerFactory.getLogger(ClientCommandHandler.class);

    public void start() throws WalletException, NKNClientException, NknHttpApiException {

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
        this.resubscribeChecker();
    }

    public void subscribe() throws WalletException, NknHttpApiException {
        try {
            LOG.info("Subscribing to topic " + LOBBY_TOPIC);

            int freeBucket = NKNExplorer.getFirstAvailableTopicBucket(LOBBY_TOPIC);

            String txID = this.wallet.tx().subscribe(LOBBY_TOPIC, freeBucket, 30, this.identity.name, (String) null);
            if (txID != null) {
                LOG.info("Subscription successful, txID: " + txID);
            }

        } catch (NknHttpApiException e) {
            if (e.getErrorCode() != -45021) {
                throw e;
            }
            LOG.info("Already subscribed");
        }
    }

    private void resubscribeChecker() {
        this.client.onCurrentHeightChanged(newHeight -> {
            if (previousHeight == 0) {
                previousHeight = newHeight;
                return;
            }

            Integer blocksPassed = newHeight - previousHeight;

            if (blocksPassed >= 3) {
                try {
                    this.subscribe();
                    previousHeight = newHeight;
                } catch (WalletException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NknHttpApiException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        });
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

    public void roll() throws WalletException, InterruptedException {
        Boolean doTry = true;
        String msgString = this.nloveMessageConverter.toMsgString(new NloveRequestUserProfileRequestMessage());

        while (doTry) {
            int bucketCnt = NKNExplorer.getTopicBucketsCount(LOBBY_TOPIC);

            final List<Subscriber> subscribers = new LinkedList<Subscriber>(
                    Arrays.asList(NKNExplorer.getSubscribers(ClientCommandHandler.LOBBY_TOPIC, bucketCnt > 0 ? rnd.nextInt(bucketCnt) : 0)));

            subscribers.removeIf(s -> s.fullClientIdentifier.equals(this.identity.getFullIdentifier()));

            if (subscribers.isEmpty()) {
                LOG.info("No profiles online currently, retrying in 1 second...");
                Thread.sleep(1000);
                continue;
            }

            Subscriber rndSub = subscribers.get(rnd.nextInt(subscribers.size()));
            LOG.info("Picked random nlove user: {}", rndSub.fullClientIdentifier);

            int tries = 0;

            do {
                tries++;
                try {
                    LOG.info("Trying to request user profile, please wait (Try #{}) ...", tries);
                    final CompletableFuture<NKNClient.ReceivedMessage> promise = this.client.sendTextMessageAsync(rndSub.fullClientIdentifier, msgString);

                    ReceivedMessage response = promise.join();
                    String channel = String.format("#%s%s", response.from, this.identity.getFullIdentifier());

                    NloveRequestUserProfileReplyMessage replyMsg = (NloveRequestUserProfileReplyMessage) this.nloveMessageConverter.parseMsg(response);

                    ShowMatchDialog dialog = new ShowMatchDialog(replyMsg.getProfile());
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.setVisible(true);

                    return;
                } catch (CompletionException e) {
                    if (e.getCause() instanceof MessageAckTimeout) {
                        doTry = true;
                        LOG.warn("User {} did not reply to profile request (offline or slow?) ...", rndSub.fullClientIdentifier);
                    } else {
                        doTry = false;
                        LOG.error("Error: {}", e);
                    }
                }
            } while (tries < 3);
        }
    }
}
