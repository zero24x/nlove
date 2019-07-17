package com.nlove.handler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.message.NloveMessageConverter;
import com.nlove.message.NloveMessageHeader;
import com.nlove.message.NloveMessageInterface;
import com.nlove.message.NloveReverseProxyConnectMessage;

import jsmith.nknsdk.client.Identity;
import jsmith.nknsdk.client.NKNClient;
import jsmith.nknsdk.client.NKNClient.ReceivedMessage;
import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.wallet.Wallet;
import jsmith.nknsdk.wallet.WalletException;

public class ReverseProxyCommandHandler {

	private NKNClient clientClient;
	private NKNClient providerClient;
	private Identity clientIdentity;
	private Identity providerIdentity;
	static String CLIENT_IDENTIFIER = "nlove-reverseproxy-client";
	static String PROVIDER_IDENTIFIER = "nlove-reverseproxy-provider";
	private Wallet wallet;
	private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("REVERSE_PROXY");
	private static final Logger LOG = LoggerFactory.getLogger(ReverseProxyCommandHandler.class);
	HashMap<String, Socket> reverseProxyToClientConnections = new HashMap<String, Socket>();
	Socket reverseProxyClientSocket;

	public void start() throws NKNClientException, WalletException, IOException {

		File walletFile = new File("walletForReverseProxy.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.clientIdentity = new Identity(ReverseProxyCommandHandler.CLIENT_IDENTIFIER, wallet);
		this.providerIdentity = new Identity(ReverseProxyCommandHandler.PROVIDER_IDENTIFIER, wallet);
		this.clientClient = new NKNClient(this.clientIdentity);
		this.providerClient = new NKNClient(this.providerIdentity);
		LOG.info("Reverse proxy handler client ID: " + this.clientIdentity.getFullIdentifier());
		LOG.info("Reverse proxy handler provider ID:" + this.providerIdentity.getFullIdentifier());

		this.providerClient.onNewMessage(msg -> {
			try {
				this.handleProviderClientMessage(msg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		this.providerClient.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				providerClient.close();
			}
		});

		this.clientClient.onNewMessage(msg -> {
			try {
				this.handleClientClientMessage(msg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		this.clientClient.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				clientClient.close();
			}
		});

	}

	public void connectToServiceProvider(String destinationFullIdentifier) throws IOException {

		ServerSocket reverseProxySocket = new ServerSocket(111);

		while (true) {

			final Socket reverseProxyClientSocket = reverseProxySocket.accept();
			new Thread() {
				public void run() {

					Thread.currentThread().setName(String.format("connectToServiceProvider %s:%s",
							reverseProxyClientSocket.getInetAddress().toString(), reverseProxyClientSocket.getPort()));
					try {
						InputStream clientIn = reverseProxyClientSocket.getInputStream();
						NloveMessageInterface connectMsg = new NloveReverseProxyConnectMessage() {
							{
								setClientPort(reverseProxyClientSocket.getPort());
							}
						};
						clientClient.sendTextMessageAsync(destinationFullIdentifier,
								nloveMessageConverter.toMsgString(connectMsg));

						int bytesRead = 0;

						while (bytesRead != -1) {

							byte[] buffer = new byte[256];
							bytesRead = clientIn.read(buffer, 0, buffer.length);

							if (bytesRead > 0) {
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								byte[] headerBytes = nloveMessageConverter
										.makeHeaderBytes(reverseProxyClientSocket.getPort());
								bos.write(headerBytes);
								bos.write(buffer, 0, bytesRead);
								LOG.debug("Read reverse proxy client bytes: " + bytesRead);
								clientClient.sendBinaryMessageAsync(destinationFullIdentifier, bos.toByteArray());
							}
						}
					} catch (Exception e) {
						try {
							reverseProxyClientSocket.close();
							reverseProxySocket.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						e.printStackTrace();
					}
				}
			}.start();
		}

	}

	private void handleProviderClientMessage(ReceivedMessage receivedMessage) throws IOException {

		if (receivedMessage.isText) {
			NloveMessageInterface c = this.nloveMessageConverter.parseMsg(receivedMessage);
			if (c instanceof NloveReverseProxyConnectMessage) {
				String clientConnectionKey = String.format("%s:%s", receivedMessage.from,
						((NloveReverseProxyConnectMessage) c).getClientPort());

				if (!this.reverseProxyToClientConnections.containsKey(clientConnectionKey)) {
					Socket serviceSocket = new Socket("localhost", 21);
					this.reverseProxyToClientConnections.put(receivedMessage.from, serviceSocket);

					Executors.newSingleThreadExecutor().execute(new Runnable() {
						@Override
						public void run() {
							Thread.currentThread()
									.setName(String.format("handleProviderClientMessage %s", receivedMessage.from));

							try {
								InputStream serviceSocketInputStream = serviceSocket.getInputStream();

								byte[] buffer = new byte[256];
								int bytesRead = 0;
								while (bytesRead != -1) {
									try {
										bytesRead = serviceSocketInputStream.read(buffer, 0, buffer.length);
										if (bytesRead > 0) {
											ByteArrayOutputStream bos = new ByteArrayOutputStream();
											bos.write(buffer, 0, bytesRead);
											providerClient.sendBinaryMessageAsync(receivedMessage.from,
													bos.toByteArray());
										}

									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}

								}
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						}
					});

				}
			}
		}

		if (receivedMessage.isBinary) {
			NloveMessageHeader header = this.nloveMessageConverter.parseHeader(receivedMessage.binaryData);
			String clientConnectionKey = String.format("%s:%s", receivedMessage.from, header.getClientPort());

			Socket serviceSocket = reverseProxyToClientConnections.get(clientConnectionKey);
			if (serviceSocket != null) {

				serviceSocket.getOutputStream().write(receivedMessage.binaryData.toByteArray());
			}
		}
	}

	private void handleClientClientMessage(ReceivedMessage receivedMessage) throws IOException {

		OutputStream reverseProxyClientSocketOutputStream = this.reverseProxyClientSocket.getOutputStream();
		reverseProxyClientSocketOutputStream.write(receivedMessage.binaryData.toByteArray());
	}

}
