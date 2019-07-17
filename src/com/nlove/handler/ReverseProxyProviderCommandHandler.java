package com.nlove.handler;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.Executors;

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

	static String PROVIDER_IDENTIFIER = "nlove-reverseproxy-provider";
	private Wallet wallet;
	private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("REVERSE_PROXY");
	private static final Logger LOG = LoggerFactory.getLogger(ReverseProxyProviderCommandHandler.class);
	HashMap<String, Socket> clientConnections = new HashMap<String, Socket>();
	Socket reverseProxyClientSocket;

	public void start() throws NKNClientException, WalletException, IOException {

		File walletFile = new File("walletForReverseProxy.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.providerIdentity = new Identity(ReverseProxyProviderCommandHandler.PROVIDER_IDENTIFIER, wallet);
		this.providerClient = new NKNClient(this.providerIdentity);
		LOG.info("Reverse proxy provider handler provider ID:" + this.providerIdentity.getFullIdentifier());

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

	}

	private void handleProviderClientMessage(ReceivedMessage receivedMessage) throws IOException {

		if (receivedMessage.isText) {
			NloveMessageInterface c = this.nloveMessageConverter.parseMsg(receivedMessage);
			if (c instanceof NloveReverseProxyConnectMessage) {
				int clientPort = ((NloveReverseProxyConnectMessage) c).getClientPort();
				String clientConnectionKey = String.format("%s:%s", receivedMessage.from, clientPort);

				if (!this.clientConnections.containsKey(clientConnectionKey)) {
					Socket serviceSocket = new Socket("localhost", 80);
					this.clientConnections.put(clientConnectionKey, serviceSocket);

					Executors.newSingleThreadExecutor().execute(new Runnable() {
						@Override
						public void run() {
							Thread.currentThread()
									.setName(String.format("handleProviderClientMessage %s", receivedMessage.from));

							try {
								int x = serviceSocket.getReceiveBufferSize();
								InputStream serviceSocketInputStream = new BufferedInputStream(
										serviceSocket.getInputStream());

								int bytesRead = 0;
								byte[] buffer = new byte[8192];
								byte[] headerBytes = nloveMessageConverter.makeHeaderBytes(clientPort, false);

								try {
									while ((bytesRead = serviceSocketInputStream.read(buffer)) != -1) {

										ByteArrayOutputStream bos = new ByteArrayOutputStream(
												headerBytes.length + buffer.length);

										bos.write(headerBytes);
										bos.write(buffer);
										bos.flush();

										providerClient.sendBinaryMessageAsync(receivedMessage.from, bos.toByteArray());

									}
								} catch (IOException e) {
								}

								try {
									serviceSocketInputStream.close();
									clientConnections.remove(clientConnectionKey);
									providerClient.sendBinaryMessageAsync(receivedMessage.from,
											nloveMessageConverter.makeHeaderBytes(clientPort, true));
								} catch (IOException e) {
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

		if (receivedMessage.isBinary)

		{
			DecodedNloveMessage decodedMsg = this.nloveMessageConverter.decodeNloveMessage(receivedMessage.binaryData);
			String clientConnectionKey = String.format("%s:%s", receivedMessage.from,
					decodedMsg.getHeader().getClientPort());

			Socket serviceSocket = clientConnections.get(clientConnectionKey);
			if (serviceSocket != null && !serviceSocket.isClosed()) {
				if (decodedMsg.getHeader().getSocketClosed()) {
					serviceSocket.close();
				} else {
					try {
						serviceSocket.getOutputStream().write(decodedMsg.getPayload());
					} catch (SocketException e) {

					}

				}

			}
		}
	}
}
