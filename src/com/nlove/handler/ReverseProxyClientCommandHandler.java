package com.nlove.handler;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.message.DecodedNloveMessage;
import com.nlove.message.NloveMessageConverter;
import com.nlove.message.NloveMessageInterface;
import com.nlove.message.NloveReverseProxyConnectMessage;
import com.nlove.message.NloveReverseProxyMessageHeader;

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
	private ServerSocket reverseProxySocket;

	public void start() throws NKNClientException, WalletException, IOException {

		File walletFile = new File("walletForReverseProxy.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.clientIdentity = new Identity(ReverseProxyClientCommandHandler.CLIENT_IDENTIFIER, wallet);
		this.clientClient = new NKNClient(this.clientIdentity);
		LOG.info("Reverse proxy handler client ID: " + this.clientIdentity.getFullIdentifier());

		this.clientClient.setNoAutomaticACKs(true);
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

		new Thread() {
			public void run() {

				try {

					if (reverseProxySocket == null || reverseProxySocket.isClosed()) {
						reverseProxySocket = new ServerSocket(222);
					}

					while (true) {
						final Socket reverseProxyClientSocket = reverseProxySocket.accept();

						Thread.currentThread()
								.setName(String.format("connectToServiceProvider %s:%s", reverseProxyClientSocket.getInetAddress().toString(), reverseProxyClientSocket.getPort()));

						String clientConnectionKey = String.format("%s:%s", destinationFullIdentifier, reverseProxyClientSocket.getPort());
						clientConnections.put(clientConnectionKey, reverseProxyClientSocket);

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
									ByteArrayOutputStream bos = new ByteArrayOutputStream(headerBytes.length + buffer.length);
									bos.write(headerBytes);
									bos.write(buffer, 0, bytesRead);
									byte[] bytesToSend = bos.toByteArray();

									Boolean ack = false;
									int tries = 1;
									do {
										if (tries > 1) {
											LOG.info("Send retry: try {}", tries);
										}
										try {
											clientClient.sendBinaryMessageAsync(destinationFullIdentifier, bytesToSend);
											ack = true;
										} catch (CompletionException e) {
											tries++;
										}

									} while (!ack && tries <= 5);

								}
								LOG.debug("Read reverse proxy client bytes: -1");
							} catch (SocketException e) {
								e.printStackTrace();
								try {
									LOG.info("Error, sending client {} disconnection message", destinationFullIdentifier);
									clientIn.close();
									clientConnections.remove(clientConnectionKey, reverseProxyClientSocket);

									clientClient.sendBinaryMessageAsync(destinationFullIdentifier, nloveMessageConverter.makeHeaderBytes(reverseProxyClientSocket.getPort(), true));

								} catch (IOException e2) {
									e.printStackTrace();
								}
							}

							try {
								LOG.info("Done reading, sending client {} disconnection message", destinationFullIdentifier);
								clientIn.close();
								clientConnections.remove(clientConnectionKey, reverseProxyClientSocket);

								clientClient.sendBinaryMessageAsync(destinationFullIdentifier, nloveMessageConverter.makeHeaderBytes(reverseProxyClientSocket.getPort(), true));

							} catch (IOException e) {
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						reverseProxySocket.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	private void handleClientClientMessage(ReceivedMessage receivedMessage) throws IOException {

		if (receivedMessage.isBinary && receivedMessage.binaryData.size() > 0) {

			clientClient.sendBinaryMessageAsync(receivedMessage.from, receivedMessage.msgId, new byte[] {});

			DecodedNloveMessage decodedMsg = nloveMessageConverter.decodeNloveMessage(receivedMessage.binaryData);
			String clientConnectionKey = String.format("%s:%s", receivedMessage.from, decodedMsg.getHeader().getClientPort());

			Socket reverseProxyClientSocket = clientConnections.get(clientConnectionKey);
			if (reverseProxyClientSocket != null) {
				NloveReverseProxyMessageHeader header = decodedMsg.getHeader();

				try {

					if (header.getSocketClosed() && !reverseProxyClientSocket.isClosed()) {
						reverseProxyClientSocket.close();
					} else {
						reverseProxyClientSocket.getOutputStream().write(decodedMsg.getPayload());
						reverseProxyClientSocket.getOutputStream().flush();
					}

					if (reverseProxyClientSocket.isClosed()) {
						clientConnections.remove(clientConnectionKey);
					}

				} catch (IOException e1) {
					e1.printStackTrace();
				}

			}

		}
	}

}
