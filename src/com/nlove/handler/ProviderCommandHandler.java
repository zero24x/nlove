package com.nlove.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.Scanner;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.message.NloveDownloadDataMessage;
import com.nlove.message.NloveDownloadRequestMessage;
import com.nlove.message.NloveDownloadRequestReplyMessage;
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
	static String CLIENT_IDENTIFIER = "nlove-provider3";
	private Wallet wallet;
	private static Integer previousHeight = 0;
	private static Integer subcribeDurationBlocks = 1000;
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

		this.client.onNewMessageWithReply(msg -> {
			return this.handle(msg);
		});

		this.client.start();
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
		NloveMessageInterface c = this.nloveMessageConverter.parseMsg(receivedMessage);

		if (c instanceof NloveSearchRequestMessage) {
			return this.handleSearch(((NloveSearchRequestMessage) c).getTerm());
		} else if (c instanceof NloveDownloadRequestMessage) {
			this.handleDownload((NloveDownloadRequestMessage) c, receivedMessage);
			return null;
		}

		return null;
	}

	private String handleSearch(String searchTerm) {

		if (this.isBlacklistedTerm(searchTerm)) {
			return "Blacklisted search term!";
		}

		String result = new Searcher().searchFor(searchTerm, this.identity);

		String resMsg = this.nloveMessageConverter.toMsgString(new NloveSearchRequestReplyMessage() {
			{
				setResult(result);
			}
		});

		return resMsg;
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

	private void handleDownload(NloveDownloadRequestMessage m, ReceivedMessage receivedMessage) {

		String fileId = m.getFileId();

		if (File.separator.equals("\\")) {
			fileId = fileId.replace("/", "\\");
		}

		File destFile = Paths.get(Searcher.SHARE_DIR_PATH.toString(), fileId).toFile();
		NloveDownloadRequestReplyMessage replyM = new NloveDownloadRequestReplyMessage();

		if (!destFile.isFile() || !destFile.exists() || !destFile.canRead()) {
			replyM.setError("File " + fileId + " not found or inaccessible!");
			this.client.sendTextMessageAsync(receivedMessage.from, this.nloveMessageConverter.toMsgString(replyM));
			return;
		}

		RandomAccessFile aFile;
		long fLen = 0;

		try {
			aFile = new RandomAccessFile(destFile, "r");
			fLen = aFile.length();
			replyM.setFileSizeBytes(fLen);
		} catch (IOException e1) {
			replyM.setError("Error reading file " + fileId + " :" + e1.toString());
			this.client.sendTextMessageAsync(receivedMessage.from, this.nloveMessageConverter.toMsgString(replyM));
			return;
		}

		final long detectedFlen = new Long(fLen);

		this.client.sendTextMessageAsync(receivedMessage.from, receivedMessage.msgId,
				this.nloveMessageConverter.toMsgString(replyM)).whenComplete((response, error) -> {
					if (error == null) {
						FileChannel inChannel = aFile.getChannel();
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						long offset = 0;

						long totalBytesRead = 0;

						while (totalBytesRead < detectedFlen) {
							try {
								totalBytesRead += inChannel.read(buffer);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							buffer.flip();
							byte[] data = null;
							buffer.get(data);

							NloveDownloadDataMessage d = new NloveDownloadDataMessage() {
								{
									setFileID(m.getFileId());
									setOffset(offset);
									setData(data);
								}
							};

							this.client.sendTextMessageAsync(receivedMessage.from,
									this.nloveMessageConverter.toMsgString(d));

							buffer.clear();
						}

						try {
							inChannel.close();
							aFile.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					} else {
						error.printStackTrace();
					}
				});

	}
}
