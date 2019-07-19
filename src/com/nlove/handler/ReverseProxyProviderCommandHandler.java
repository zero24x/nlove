package com.nlove.handler;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

public class ReverseProxyProviderCommandHandler {

	private NKNClient providerClient;
	private Identity providerIdentity;

	static String PROVIDER_IDENTIFIER = "nlove-reverseproxy-provider3";
	private Wallet wallet;
	private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("REVERSE_PROXY");
	private static final Logger LOG = LoggerFactory.getLogger(ReverseProxyProviderCommandHandler.class);
	ConcurrentHashMap<String, Socket> clientConnections = new ConcurrentHashMap<String, Socket>();
	Socket reverseProxyClientSocket;
	private AtomicInteger numConnections = new AtomicInteger();
	private static int MAX_CONNECTIONS = 100;

	public void start() throws NKNClientException, WalletException, IOException {

		File walletFile = new File("walletForReverseProxy.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.providerIdentity = new Identity(ReverseProxyProviderCommandHandler.PROVIDER_IDENTIFIER, wallet);
		this.providerClient = new NKNClient(this.providerIdentity);
		LOG.info("Reverse proxy provider handler provider ID:" + this.providerIdentity.getFullIdentifier());

		this.providerClient.setNoAutomaticACKs(true);
		this.providerClient.onNewMessage(msg -> {
			try {
				this.handleProviderClientMessage(msg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				providerClient.close();
			}
		});

	}

	private void handleProviderClientMessage(ReceivedMessage receivedMessage) throws IOException {

		new Thread() {
			public void run() {

				if (receivedMessage.isBinary && receivedMessage.binaryData.size() > 0) {
					providerClient.sendBinaryMessageAsync(receivedMessage.from, receivedMessage.msgId, new byte[] {});
				}

				if (receivedMessage.isText) {
					NloveMessageInterface c = nloveMessageConverter.parseMsg(receivedMessage);
					if (c instanceof NloveReverseProxyConnectMessage) {
						int clientPort = ((NloveReverseProxyConnectMessage) c).getClientPort();
						String clientConnectionKey = String.format("%s:%s", receivedMessage.from, clientPort);

						if (numConnections.get() >= MAX_CONNECTIONS) {
							LOG.info("Not accepting new client connection , max. connections of {} reached", MAX_CONNECTIONS);
							return;
						}

						if (!clientConnections.containsKey(clientConnectionKey)) {

							Thread.currentThread().setName(String.format("handleProviderClientMessage %s", receivedMessage.from));

							try {
								Socket serviceSocket = new Socket("localhost", 80);
								clientConnections.put(clientConnectionKey, serviceSocket);
								int numConnectionsNow = numConnections.incrementAndGet();
								LOG.info("Num connections = {}", numConnectionsNow);

								InputStream serviceSocketInputStream = new BufferedInputStream(serviceSocket.getInputStream());

								int bytesRead = 0;

								byte[] buffer = new byte[8192];

								byte[] headerBytes = nloveMessageConverter.makeHeaderBytes(clientPort, false);

								try {
									while ((bytesRead = serviceSocketInputStream.read(buffer)) != -1) {
										ByteArrayOutputStream bos = new ByteArrayOutputStream(headerBytes.length + buffer.length);

										bos.write(headerBytes);
										bos.write(buffer, 0, bytesRead);
										bos.flush();
										byte[] bytesToSend = bos.toByteArray();

										Boolean ack = false;
										int tries = 1;
										do {
											if (tries > 1) {
												LOG.info("Send retry: try {}", tries);
											}
											try {
												providerClient.sendBinaryMessageAsync(receivedMessage.from, bytesToSend);
												ack = true;
											} catch (CompletionException e) {
												tries++;
											}

										} while (!ack && tries <= 3);

									}
								} catch (Exception e) {
									e.printStackTrace();
								}

								try {
									serviceSocketInputStream.close();
									clientConnections.remove(clientConnectionKey);
									numConnectionsNow = numConnections.decrementAndGet();
									LOG.info("Num connections = {}", numConnectionsNow);

									providerClient.sendBinaryMessageAsync(receivedMessage.from, nloveMessageConverter.makeHeaderBytes(clientPort, true));
								} catch (IOException e) {
									e.printStackTrace();
								}

							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						}
					}
				}

				if (receivedMessage.isBinary && receivedMessage.binaryData.size() > 0) {
					DecodedNloveMessage decodedMsg = nloveMessageConverter.decodeNloveMessage(receivedMessage.binaryData);
					String clientConnectionKey = String.format("%s:%s", receivedMessage.from, decodedMsg.getHeader().getClientPort());

					Socket serviceSocket = clientConnections.get(clientConnectionKey);
					if (serviceSocket != null && !serviceSocket.isClosed()) {
						if (decodedMsg.getHeader().getSocketClosed()) {
							try {
								serviceSocket.close();
								clientConnections.remove(clientConnectionKey);
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							try {
								serviceSocket.getOutputStream().write(decodedMsg.getPayload());
								serviceSocket.getOutputStream().flush();
							} catch (IOException e) {
								e.printStackTrace();
								try {
									serviceSocket.close();
									clientConnections.remove(clientConnectionKey);
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								clientConnections.remove(clientConnectionKey);
							}

						}

					}
				}
			}
		}.start();

	}
}
