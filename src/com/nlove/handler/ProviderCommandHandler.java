package com.nlove.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Scanner;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
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
	private static Integer subcribeDurationBlocks = 1000;
	private static String providerTopic = "nlove-providers";

	private static final Logger LOG = LoggerFactory.getLogger(ProviderCommandHandler.class);

	public void start() throws NKNClientException, WalletException {
		File walletFile = new File("walletForProvider.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.identity = new Identity(ProviderCommandHandler.CLIENT_IDENTIFIER, wallet);
		this.client = new NKNClient(this.identity);

		this.client.onNewMessageWithReply(msg -> {
			return this.handle(msg);
		});
		this.resubscribeChecker();
		this.client.start();
		this.subscribe();
	}

	public void subscribe() throws WalletException {
		try {
			System.out.println("Subscribing to topic '" + providerTopic + "' using " + CLIENT_IDENTIFIER
					+ (CLIENT_IDENTIFIER == null || CLIENT_IDENTIFIER.isEmpty() ? "" : ".")
					+ Hex.toHexString(this.wallet.getPublicKey()));

			String txID = this.wallet.tx().subscribe(providerTopic, 0, subcribeDurationBlocks, CLIENT_IDENTIFIER,
					(String) null);
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

		String res = "UNKNOWN_COMMAND";

		/*
		 * if (!receivedMessage.isText ||
		 * !receivedMessage.textData.startsWith(NloveMessage.MAGIC_IDENTIFIER)) { return
		 * res; }
		 */

		String msg = receivedMessage.textData.substring(8);

		LOG.info("PROVIDER: Received command " + msg);

		if (receivedMessage.isText) {

			if (msg.startsWith("SEARCH")) {
				res = this.handleSearch(msg);
			}
			if (msg.startsWith("DOWNLOAD")) {
				res = this.handleDownload(msg.substring(9), receivedMessage);
			}
		}

		LOG.info("PROVIDER: Replying to command " + msg + " with: \n" + res);
		return res;
	}

	private String handleSearch(String textData) {
		String searchTerm = textData.substring(7);

		if (this.isBlacklistedTerm(searchTerm)) {
			return "Blacklisted search term!";
		}

		Searcher se = new Searcher();
		String res = se.searchFor(searchTerm, this.identity);

		return res;
	}

	private Boolean isBlacklistedTerm(String term) {
		File blacklistFile = Paths.get(System.getProperty("user.dir"), File.separator.toString(), "config",
				File.separator.toString(), "blacklist.txt").toFile();
		Scanner sc = null;

		try {
			sc = new Scanner(blacklistFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while (sc.hasNextLine()) {
			if (sc.nextLine().equals(term)) {
				return true;
			}
		}
		return false;
	}

	private String handleDownload(String file, ReceivedMessage msg) {
		if (File.separator.equals("\\")) {
			file = file.replace("/", "\\");
		}

		File destFile = Paths.get(Searcher.SHARE_DIR_PATH.toString(), file).toFile();

		if (!destFile.isFile() || !destFile.exists() || !destFile.canRead()) {
			this.client.sendTextMessageAsync(msg.from, "File " + file + " not found or inaccessible!");
			return "FILE_NOT_FOUND";
		}

		InputStream initialStream;

		try {
			initialStream = new FileInputStream(destFile);
			this.client.sendBinaryMessageAsync(msg.from, ByteString.readFrom(initialStream));
			return "FILE_ON_THE_WAY";
		} catch (Exception e) {
			this.client.sendTextMessageAsync(msg.from, "File " + file + " could not be sent, error: " + e.toString());
			return "FILE_SEND_ERROR";
		}

	}

}
