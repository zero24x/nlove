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
	private static final Logger LOG = LoggerFactory.getLogger(ReverseProxyCommandHandler.class);
	HashMap<String, Socket> connections = new HashMap<String, Socket>();
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
		this.reverseProxyClientSocket = reverseProxySocket.accept();

		InputStream clientIn = reverseProxyClientSocket.getInputStream();
		this.clientClient.sendBinaryMessageAsync(destinationFullIdentifier, new byte[] { 1 });

		int bytesRead = 0;

		while (bytesRead != -1) {

			byte[] buffer = new byte[256];
			bytesRead = clientIn.read(buffer, 0, buffer.length);

			if (bytesRead != -1) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bos.write(buffer, 0, bytesRead);
				LOG.debug("Read reverse proxy client bytes: " + bytesRead);
				this.clientClient.sendBinaryMessageAsync(destinationFullIdentifier, bos.toByteArray());
			}

		}
	}

	private void handleProviderClientMessage(ReceivedMessage receivedMessage) throws IOException {

		if (!this.connections.containsKey(receivedMessage.from)) {
			Socket serviceSocket = new Socket("localhost", 21);
			this.connections.put(receivedMessage.from, serviceSocket);

			InputStream serviceSocketInputStream = serviceSocket.getInputStream();

			Executors.newSingleThreadExecutor().execute(new Runnable() {
				@Override
				public void run() {
					byte[] buffer = new byte[256];
					int bytesRead = 0;

					while (bytesRead != -1) {
						try {
							bytesRead = serviceSocketInputStream.read(buffer, 0, buffer.length);
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							bos.write(buffer, 0, bytesRead);
							providerClient.sendBinaryMessageAsync(receivedMessage.from, bos.toByteArray());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

				}
			});

		}
		Socket serviceSocket = this.connections.get(receivedMessage.from);
		serviceSocket.getOutputStream().write(receivedMessage.binaryData.toByteArray());
		serviceSocket.getOutputStream().flush();

	}

	private void handleClientClientMessage(ReceivedMessage receivedMessage) throws IOException {

		OutputStream reverseProxyClientSocketOutputStream = this.reverseProxyClientSocket.getOutputStream();
		reverseProxyClientSocketOutputStream.write(receivedMessage.binaryData.toByteArray());
		reverseProxyClientSocketOutputStream.flush();
	}

}
