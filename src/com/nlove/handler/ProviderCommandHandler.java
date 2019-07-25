package com.nlove.handler;

import java.io.File;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.message.NloveMessageConverter;
import com.nlove.message.NloveMessageInterface;
import com.nlove.message.NloveSearchRequestMessage;
import com.nlove.message.NloveSearchRequestReplyMessage;
import com.nlove.searcher.Searcher;

import jsmith.nknsdk.client.Identity;
import jsmith.nknsdk.client.NKNClient;
import jsmith.nknsdk.client.NKNClient.ReceivedMessage;
import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.network.NknHttpApiException;
import jsmith.nknsdk.wallet.Wallet;
import jsmith.nknsdk.wallet.WalletException;

public class ProviderCommandHandler {

	private NKNClient client;
	private Identity identity;
	static String CLIENT_IDENTIFIER = "nlove-provider";
	private Wallet wallet;
	private static Integer previousHeight = 0;
	private static Integer subcribeDurationBlocks = 4320; // blocks in 1 day
	private static String providerTopic = "nlove-providers";
	private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("PROVIDER");

	private static final Logger LOG = LoggerFactory.getLogger(ProviderCommandHandler.class);

	public void start() throws NKNClientException, WalletException {

		File walletFile = new File("walletForProvider.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.identity = new Identity(ProviderCommandHandler.CLIENT_IDENTIFIER, wallet);
		this.client = new NKNClient(this.identity);
		LOG.info("Provider ID: " + this.identity.getFullIdentifier());

		this.client.onNewMessageWithReply(msg -> {
			return this.handle(msg);
		}).start();

		this.subscribe();
		this.resubscribeChecker();
	}

	public void subscribe() throws WalletException {
		try {
			System.out.println("Subscribing to topic '" + providerTopic + "' using " + CLIENT_IDENTIFIER + (CLIENT_IDENTIFIER == null || CLIENT_IDENTIFIER.isEmpty() ? "" : ".")
					+ Hex.toHexString(this.wallet.getPublicKey()));

			String txID = this.wallet.tx().subscribe(providerTopic, 0, subcribeDurationBlocks, CLIENT_IDENTIFIER, (String) null);
			LOG.info("PROVIDER: Subscribe transaction successful: " + txID);

		} catch (NknHttpApiException $e) {
			if ($e.getErrorCode() != -45021) {
				throw new RuntimeException($e);
			}
			LOG.info("PROVIDER: Already subscribed");
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

	private String handle(ReceivedMessage receivedMessage) {
		NloveMessageInterface c = this.nloveMessageConverter.parseMsg(receivedMessage);

		if (c instanceof NloveSearchRequestMessage) {
			return this.handleSearch(((NloveSearchRequestMessage) c).getTerm());
		}

		return null;
	}

	private String handleSearch(String searchTerm) {

		String result = new Searcher().searchFor(searchTerm, this.identity);

		String resMsg = this.nloveMessageConverter.toMsgString(new NloveSearchRequestReplyMessage() {
			{
				setResult(result);
			}
		});

		return resMsg;
	}
}
