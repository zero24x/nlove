package com.nlove.handler;

import java.io.File;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.config.NloveProfileManager;
import com.nlove.message.NloveMessageConverter;
import com.nlove.message.NloveMessageInterface;
import com.nlove.message.NloveRequestUserProfileReplyMessage;
import com.nlove.message.NloveRequestUserProfileRequestMessage;

import jsmith.nknsdk.client.Identity;
import jsmith.nknsdk.client.NKNClient;
import jsmith.nknsdk.client.NKNClient.ReceivedMessage;
import jsmith.nknsdk.client.NKNClientException;
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
	public static String LOBBY_TOPIC = "nlove-lobby";
	private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("CLIENT");
	private Random rnd = new Random();

	private static final Logger LOG = LoggerFactory.getLogger(ClientCommandHandler.class);

	public void start() throws WalletException, NKNClientException {

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

	public void subscribe() throws WalletException {
		try {
			LOG.info("Subscribing to topic " + LOBBY_TOPIC);

			int freeBucket = NKNExplorer.getFirstAvailableTopicBucket(LOBBY_TOPIC);

			String txID = this.wallet.tx().subscribe(LOBBY_TOPIC, freeBucket, 4320 * 30 /* 1m */, this.identity.getFullIdentifier(), (String) null);
			LOG.info("Subscribe transaction successful: " + txID);

		} catch (NknHttpApiException $e) {
			if ($e.getErrorCode() != -45021) {
				throw new RuntimeException($e);
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

			if (blocksPassed >= 3600 /* 1h */) {
				try {
					this.subscribe();
					previousHeight = newHeight;
				} catch (WalletException e) {
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

		return "";
	}

	public void roll() throws WalletException, InterruptedException {
		Boolean failed = false;

		do {
			int bucketCnt = NKNExplorer.getTopicBucketsCount(LOBBY_TOPIC);

			final NKNExplorer.Subscriber[] subscribers = NKNExplorer.getSubscribers(ClientCommandHandler.LOBBY_TOPIC, bucketCnt > 0 ? rnd.nextInt(bucketCnt) : 0);

			Subscriber rndSub = subscribers[rnd.nextInt(subscribers.length - 1)];

			try {

				LOG.info("Picked random nlove user: {}", rndSub.fullClientIdentifier);
				LOG.info("Trying to request user profile, please wait ...");

				final CompletableFuture<NKNClient.ReceivedMessage> promise = this.client.sendTextMessageAsync(rndSub.fullClientIdentifier,
						this.nloveMessageConverter.toMsgString(new NloveRequestUserProfileRequestMessage()));

				ReceivedMessage response = promise.join();
				String channel = String.format("#%s%s", response.from, this.identity.getFullIdentifier());

				NloveRequestUserProfileReplyMessage replyMsg = (NloveRequestUserProfileReplyMessage) this.nloveMessageConverter.parseMsg(response);

				LOG.info(
						"Congrats, you have been matched with user {}!\r\n Read the user profile to see if this person"
								+ "is of interest to you:\r\n ====== {} ====== \r\n. To get into contact, join channel {} in the D-Chat DApp!",
						response.from, replyMsg.getProfile(), channel);
			} catch (CompletionException e) {
				LOG.warn("User profile request for user {} failed, trying next one ...: {}", rndSub, e.toString());
				failed = true;
			}
		} while (failed);

	}
}
