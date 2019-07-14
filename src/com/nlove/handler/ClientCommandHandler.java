package com.nlove.handler;

import java.io.File;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.message.NloveMessage;

import jsmith.nknsdk.client.Identity;
import jsmith.nknsdk.client.NKNClient;
import jsmith.nknsdk.client.NKNClient.ReceivedMessage;
import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.network.NknHttpApiException;
import jsmith.nknsdk.wallet.Wallet;
import jsmith.nknsdk.wallet.WalletException;

public class ClientCommandHandler {

	private NKNClient client;
	private Identity pchIdentity;
	static String CLIENT_IDENTIFIER = "nlove-client";
	private Wallet wallet;
	private static Integer previousHeight = 0;
	private static Integer subcribeDurationBlocks = 1000;
	private static String lobbyTopic = "nlove-lobby2";
	private static String providerTopic = "nlove-providers";

	private static final Logger LOG = LoggerFactory.getLogger(ClientCommandHandler.class);

	public void start() throws WalletException, NKNClientException {

		File walletFile = new File("walletForClient.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.pchIdentity = new Identity(ClientCommandHandler.CLIENT_IDENTIFIER, wallet);
		this.client = new NKNClient(this.pchIdentity);

		this.client.onNewMessage(msg -> {
			this.handle(msg);
		});
		this.resubscribeChecker();
		this.client.start();
		this.subscribe();
	}

	public void subscribe() throws WalletException {
		try {

			Identity clientIdentity = new Identity(CLIENT_IDENTIFIER, Wallet.createNew());

			System.out.println("Subscribing to topic '" + lobbyTopic + "' using " + CLIENT_IDENTIFIER
					+ (CLIENT_IDENTIFIER == null || CLIENT_IDENTIFIER.isEmpty() ? "" : ".")
					+ Hex.toHexString(this.wallet.getPublicKey()));

			String txID = this.wallet.tx().subscribe(lobbyTopic, 0, subcribeDurationBlocks, CLIENT_IDENTIFIER,
					(String) null);
			LOG.info("CLIENT: Subscribe transaction successful: " + txID);

		} catch (NknHttpApiException $e) {
			if ($e.getErrorCode() != -45021) {
				throw new RuntimeException($e);
			}
			LOG.info("CLIENT: Already subscribed");
		}
	}

	private void resubscribeChecker() {
		this.client.onCurrentHeightChanged(newHeight -> {
			if (previousHeight == 0) {
				previousHeight = newHeight;
				return;
			}

			Integer blocksPassed = newHeight - previousHeight;

			if (blocksPassed >= subcribeDurationBlocks) {
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

	private void handle(ReceivedMessage receivedMessage) {

		String res = "UNKNOWN_COMMAND";

		if (!receivedMessage.isText && !receivedMessage.textData.startsWith(NloveMessage.MAGIC_IDENTIFIER)) {
			return;
		}

		String msg = receivedMessage.textData.substring(8);

		LOG.info("CLIENT: Received command " + msg);

		if (receivedMessage.isText) {
			if (msg.startsWith("CHAT")) {
				LOG.info(String.format("(Chat) <%s>: %s", receivedMessage.from, msg.substring(5)));
			}
		}

	}

	public void search(String term) throws WalletException {
		this.client.publishTextMessageAsync(providerTopic, 0, new NloveMessage().search(term));
	}

	public void download(String id) {
		String[] idParts = id.split("/");

		String destination = idParts[0];
		String fileId = idParts[1];

		this.client.sendTextMessageAsync(destination, new NloveMessage().download(fileId));
	}

	public void chat(String text) throws WalletException {
		this.client.publishTextMessageAsync(ClientCommandHandler.providerTopic, 0, new NloveMessage().chat(text));
	}

}
