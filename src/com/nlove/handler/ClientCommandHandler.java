package com.nlove.handler;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.download.FileDownloadManager;
import com.nlove.message.NloveChatMessage;
import com.nlove.message.NloveDownloadDataMessage;
import com.nlove.message.NloveDownloadRequestMessage;
import com.nlove.message.NloveDownloadRequestReplyMessage;
import com.nlove.message.NloveMessageConverter;
import com.nlove.message.NloveMessageInterface;
import com.nlove.message.NloveSearchRequestMessage;
import com.nlove.message.NloveSearchRequestReplyMessage;

import jsmith.nknsdk.client.Identity;
import jsmith.nknsdk.client.NKNClient;
import jsmith.nknsdk.client.NKNClient.ReceivedMessage;
import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.network.NknHttpApiException;
import jsmith.nknsdk.wallet.Wallet;
import jsmith.nknsdk.wallet.WalletException;

public class ClientCommandHandler {

	private NKNClient client;
	private Identity identity;
	static String CLIENT_IDENTIFIER = "nlove-client";
	private Wallet wallet;
	private static Integer previousHeight = 0;
	private static Integer subcribeDurationBlocks = 1000;
	private static String lobbyTopic = "nlove-lobby";
	private static String providerTopic = "nlove-providers";
	private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("CLIENT");
	private FileDownloadManager fileDownloadManager;

	private static final Logger LOG = LoggerFactory.getLogger(ClientCommandHandler.class);

	public void start() throws WalletException, NKNClientException {

		File walletFile = new File("walletForClient.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.identity = new Identity(ClientCommandHandler.CLIENT_IDENTIFIER, wallet);
		this.fileDownloadManager = new FileDownloadManager();

		this.client = new NKNClient(this.identity);

		this.client.onNewMessage(msg -> {
			this.handle(msg);
		}).start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				client.close();
			}
		});
		this.subscribe();
		this.resubscribeChecker();
	}

	public void subscribe() throws WalletException {
		try {
			System.out.println("Subscribing to topic '" + lobbyTopic + "' using " + CLIENT_IDENTIFIER + (CLIENT_IDENTIFIER == null || CLIENT_IDENTIFIER.isEmpty() ? "" : ".")
					+ Hex.toHexString(this.wallet.getPublicKey()));

			String txID = this.wallet.tx().subscribe(lobbyTopic, 0, subcribeDurationBlocks, CLIENT_IDENTIFIER, (String) null);
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

		NloveMessageInterface c = this.nloveMessageConverter.parseMsg(receivedMessage);

		if (c instanceof NloveChatMessage) {
			LOG.info(String.format("(Chat) <%s>: %s", receivedMessage.from, ((NloveChatMessage) c).getText()));
		} else if (c instanceof NloveDownloadRequestReplyMessage) {
			NloveDownloadRequestReplyMessage r = (NloveDownloadRequestReplyMessage) this.nloveMessageConverter.parseMsg(receivedMessage);
			this.fileDownloadManager.handleDownloadRequestReplyMessage(receivedMessage.msgId.toString(), r);
		} else if (c instanceof NloveDownloadDataMessage) {
			this.fileDownloadManager.handleDownloadRequestDataMessage(receivedMessage.msgId.toString(), ((NloveDownloadDataMessage) c));
		}

	}

	public void search(String term) throws WalletException {
		NloveSearchRequestMessage m = new NloveSearchRequestMessage();
		m.setTerm(term);

		final List<CompletableFuture<NKNClient.ReceivedMessage>> promises = this.client.publishTextMessageAsync(ClientCommandHandler.providerTopic, 0,
				this.nloveMessageConverter.toMsgString(m));

		for (CompletableFuture<ReceivedMessage> promise : promises) {
			promise.whenComplete((response, error) -> {
				if (error == null) {
					NloveSearchRequestReplyMessage resM = (NloveSearchRequestReplyMessage) this.nloveMessageConverter.parseMsg(response);

					LOG.info(String.format("\nSearch result from %s:\n========= \n%s\n ========= ", response.from, resM.getResult()));
				} else {
					LOG.info(String.format("Search response error %s from %s: %s", error.toString(), response.from, response.textData));
				}
			});
		}

	}

	public void download(String fileId) {
		String[] idParts = fileId.split("/");

		String destination = idParts[0];
		fileId = fileId.substring(fileId.indexOf("/") + 1);

		final NloveDownloadRequestMessage m = new NloveDownloadRequestMessage();
		m.setFileId(fileId);

		this.client.sendTextMessageAsync(destination, this.nloveMessageConverter.toMsgString(m));

	}

	public void chat(String text) throws WalletException {
		NloveChatMessage c = new NloveChatMessage();
		c.setText(text);

		this.client.publishTextMessageAsync(ClientCommandHandler.lobbyTopic, 0, this.nloveMessageConverter.toMsgString(c));
	}

}
