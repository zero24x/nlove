package com.nlove.handler;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.message.DecodedNloveMessage;
import com.nlove.message.NloveMessageConverter;
import com.nlove.message.NloveMessageInterface;
import com.nlove.message.NloveReverseProxyConnectMessage;

import jsmith.nknsdk.client.Identity;
import jsmith.nknsdk.client.NKNClient;
import jsmith.nknsdk.client.NKNClient.ReceivedMessage;
import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.wallet.Wallet;
import jsmith.nknsdk.wallet.WalletException;

public class ReverseProxyClientCommandHandler {

	private NKNClient clientClient;
	private Identity clientIdentity;
	static String CLIENT_IDENTIFIER = "nlove-reverseproxy-client";
	private Wallet wallet;
	private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("REVERSE_PROXY");
	private static final Logger LOG = LoggerFactory.getLogger(ReverseProxyClientCommandHandler.class);
	HashMap<String, Socket> clientConnections = new HashMap<String, Socket>();

	public void start() throws NKNClientException, WalletException, IOException {

		File walletFile = new File("walletForReverseProxy.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.clientIdentity = new Identity(ReverseProxyClientCommandHandler.CLIENT_IDENTIFIER, wallet);
		this.clientClient = new NKNClient(this.clientIdentity);
		LOG.info("Reverse proxy handler client ID: " + this.clientIdentity.getFullIdentifier());

		this.clientClient.onNewMessage(msg -> {
			try {
				this.handleClientClientMessage(msg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				clientClient.close();
			}
		});

	}

	public void connectToServiceProvider(String destinationFullIdentifier) throws IOException {

		ServerSocket reverseProxySocket = new ServerSocket(2300);

		while (true) {

			final Socket reverseProxyClientSocket = reverseProxySocket.accept();
			String clientConnectionKey = String.format("%s:%s", destinationFullIdentifier, reverseProxyClientSocket.getPort());
			this.clientConnections.put(clientConnectionKey, reverseProxyClientSocket);

			new Thread() {
				public void run() {

					Thread.currentThread()
							.setName(String.format("connectToServiceProvider %s:%s", reverseProxyClientSocket.getInetAddress().toString(), reverseProxyClientSocket.getPort()));
					try {
						BufferedInputStream clientIn = new BufferedInputStream(reverseProxyClientSocket.getInputStream());

						NloveMessageInterface connectMsg = new NloveReverseProxyConnectMessage() {
							{
								setClientPort(reverseProxyClientSocket.getPort());
							}
						};
						clientClient.sendTextMessageAsync(destinationFullIdentifier, nloveMessageConverter.toMsgString(connectMsg));

						int bytesRead = 0;
						byte[] headerBytes = nloveMessageConverter.makeHeaderBytes(reverseProxyClientSocket.getPort(), false);
						byte[] buffer = new byte[8192];

						try {
							while ((bytesRead = clientIn.read(buffer)) != -1) {
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								bos.write(headerBytes);
								bos.write(buffer, 0, bytesRead);
								byte[] bytesToSend = bos.toByteArray();

								Boolean ack = false;
								do {
									try {
										CompletableFuture<ReceivedMessage> resp = clientClient.sendBinaryMessageAsync(destinationFullIdentifier, bytesToSend);
										ReceivedMessage res = resp.get();
										ack = true;
									} catch (CompletionException e) {
										LOG.warn("No ACK from {} because {}", clientConnectionKey, e.toString());
									}

								} while (!ack);
							}
						} catch (SocketException e) {

						}

						LOG.debug("Read reverse proxy client bytes: -1");

						try {
							clientIn.close();
							clientConnections.remove(clientConnectionKey, reverseProxyClientSocket);
							clientClient.sendBinaryMessageAsync(destinationFullIdentifier, nloveMessageConverter.makeHeaderBytes(reverseProxyClientSocket.getPort(), true));

						} catch (IOException e) {
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}

	}

	private void handleClientClientMessage(ReceivedMessage receivedMessage) throws IOException {

		if (receivedMessage.isBinary && receivedMessage.binaryData.size() > 0) {

			this.clientClient.sendBinaryMessageAsync(receivedMessage.from, receivedMessage.msgId, new byte[] {});

			DecodedNloveMessage decodedMsg = this.nloveMessageConverter.decodeNloveMessage(receivedMessage.binaryData);
			String clientConnectionKey = String.format("%s:%s", receivedMessage.from, decodedMsg.getHeader().getClientPort());

			Socket reverseProxyClientSocket = clientConnections.get(clientConnectionKey);
			if (reverseProxyClientSocket != null && !reverseProxyClientSocket.isClosed()) {

				if (decodedMsg.getHeader().getSocketClosed()) {
					try {
						reverseProxyClientSocket.close();
						clientConnections.remove(clientConnectionKey);
					} catch (IOException e) {

					}

				} else {
					reverseProxyClientSocket.getOutputStream().write(decodedMsg.getPayload());
					reverseProxyClientSocket.getOutputStream().flush();
				}

			}

		}

	}

}
