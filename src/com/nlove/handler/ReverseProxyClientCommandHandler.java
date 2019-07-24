package com.nlove.handler;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.message.DecodedNloveMessage;
import com.nlove.message.NloveMessageConverter;
import com.nlove.message.NloveMessageInterface;
import com.nlove.message.NloveReverseProxyConnectMessage;
import com.nlove.provider.HoldedObject;
import com.nlove.provider.ReverseProxyDecodedPacket;
import com.nlove.provider.ReverseProxyReplyPacket;

import jsmith.nknsdk.client.Identity;
import jsmith.nknsdk.client.NKNClient;
import jsmith.nknsdk.client.NKNClient.ReceivedMessage;
import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.wallet.Wallet;
import jsmith.nknsdk.wallet.WalletException;

public class ReverseProxyClientCommandHandler {

	private NKNClient nknClient;
	private Identity clientIdentity;
	static String CLIENT_IDENTIFIER = "nlove-reverseproxy-client3";
	private Wallet wallet;
	private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("REVERSE_PROXY");
	private static final Logger LOG = LoggerFactory.getLogger(ReverseProxyClientCommandHandler.class);
	private ConcurrentHashMap<String, CommandHandlerPackageFlowManager> packageFlowManagers = new ConcurrentHashMap<String, CommandHandlerPackageFlowManager>();

	private ServerSocket reverseProxySocket;
	private Phaser unackedPacketsPhaser = new Phaser(2);

	public void start() throws NKNClientException, WalletException, IOException {

		File walletFile = new File("walletForReverseProxy.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.clientIdentity = new Identity(ReverseProxyClientCommandHandler.CLIENT_IDENTIFIER, wallet);
		this.nknClient = new NKNClient(this.clientIdentity);
		LOG.info("Reverse proxy handler client ID: " + this.clientIdentity.getFullIdentifier());

		this.nknClient.setNoAutomaticACKs(true);
		this.nknClient.onNewMessage(msg -> {
			try {
				this.handleClientClientMessage(msg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				nknClient.close();
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
					System.out.println(String.format("To connect to %s, connect to 127.0.0.1:222", destinationFullIdentifier));

					while (true) {
						final Socket reverseProxyClientSocket = reverseProxySocket.accept();

						String clientConnectionKey = String.format("%s:%s", destinationFullIdentifier, reverseProxyClientSocket.getPort());

						Thread.currentThread().setName(String.format("connectToServiceProvider %s", clientConnectionKey));

						System.out.println(String.format("Accepted local client-to-provider connection 127.0.0.1:" + reverseProxyClientSocket.getPort()));
						CommandHandlerPackageFlowManager packageFlowManager = new CommandHandlerPackageFlowManager(clientConnectionKey, nknClient);
						packageFlowManagers.put(clientConnectionKey, packageFlowManager);
						packageFlowManager.start();

						packageFlowManager.getClientConnections().put(clientConnectionKey, reverseProxyClientSocket);

						try {
							BufferedInputStream clientIn = new BufferedInputStream(reverseProxyClientSocket.getInputStream());

							NloveMessageInterface connectMsg = new NloveReverseProxyConnectMessage() {
								{
									setClientPort(reverseProxyClientSocket.getPort());
								}
							};
							nknClient.sendTextMessageAsync(destinationFullIdentifier, nloveMessageConverter.toMsgString(connectMsg));

							int bytesRead = 0;

							packageFlowManager.getHoldedIncomingPackets().clear();

							byte[] buffer = new byte[8192];

							try {

								while ((bytesRead = clientIn.read(buffer)) != -1) {
									packageFlowManager.addToSeqNum(bytesRead);
									byte[] headerBytes = nloveMessageConverter.makeHeaderBytes(false, reverseProxyClientSocket.getPort(), false, packageFlowManager.getAckNum(),
											packageFlowManager.getSeqNum());

									ByteArrayOutputStream bos = new ByteArrayOutputStream(headerBytes.length + buffer.length);
									bos.write(headerBytes);
									bos.write(buffer, 0, bytesRead);
									byte[] bytesToSend = bos.toByteArray();

									nknClient.sendBinaryMessageAsync(destinationFullIdentifier, bytesToSend);
									packageFlowManager.getUnackedPackets().put(packageFlowManager.getSeqNum(),
											new HoldedObject<ReverseProxyReplyPacket>(new ReverseProxyReplyPacket(destinationFullIdentifier, bytesToSend)));

									unackedPacketsPhaser.arriveAndAwaitAdvance();
									if (packageFlowManager.getUnackedPackets().size() >= 1) {
										LOG.debug("Unacked packets too big, pausing");
									}
								}
								LOG.debug("Read reverse proxy client bytes: -1");
							} catch (SocketException e) {
								e.printStackTrace();
								try {
									LOG.info("Error {}, sending client {} disconnection message", e.toString(), destinationFullIdentifier);

									clientIn.close();
									packageFlowManager.stop();
									packageFlowManagers.remove(clientConnectionKey);

									nknClient.sendBinaryMessageAsync(destinationFullIdentifier, nloveMessageConverter.makeHeaderBytes(false, reverseProxyClientSocket.getPort(),
											true, packageFlowManager.getAckNum(), packageFlowManager.getSeqNum()));

								} catch (IOException e2) {
									e.printStackTrace();
								}
							}

							try {
								LOG.info("Done reading, sending client {} disconnection message", destinationFullIdentifier);

								clientIn.close();
								packageFlowManager.stop();
								packageFlowManagers.remove(clientConnectionKey);

								nknClient.sendBinaryMessageAsync(destinationFullIdentifier, nloveMessageConverter.makeHeaderBytes(false, reverseProxyClientSocket.getPort(), true,
										packageFlowManager.getAckNum(), packageFlowManager.getSeqNum()));

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

	private synchronized void handleClientClientMessage(ReceivedMessage receivedMessage) throws IOException {

		if (receivedMessage.isBinary && receivedMessage.binaryData.size() > 0) {
			DecodedNloveMessage decodedMsg = nloveMessageConverter.decodeNloveMessage(receivedMessage.binaryData);
			String clientConnectionKey = String.format("%s:%s", receivedMessage.from, decodedMsg.getHeader().getClientPort());
			CommandHandlerPackageFlowManager packageFlowManager = this.packageFlowManagers.get(clientConnectionKey);

			if (!decodedMsg.getHeader().isAck()) {
				byte[] ackHeaderBytes = nloveMessageConverter.makeHeaderBytes(true, decodedMsg.getHeader().getClientPort(), false, decodedMsg.getHeader().getAckNum(),
						decodedMsg.getHeader().getSeqNum());
				nknClient.sendBinaryMessageAsync(receivedMessage.from, ackHeaderBytes);
			} else {
				if (packageFlowManager != null) {
					packageFlowManager.getUnackedPackets().remove(decodedMsg.getHeader().getSeqNum());
				}
			}

			if (packageFlowManager == null) {
				LOG.debug("No packageFlowManager found for conn {}", clientConnectionKey);
				return;
			}

			Socket reverseProxyClientSocket = packageFlowManager.getClientConnections().get(clientConnectionKey);

			if (reverseProxyClientSocket != null) {
				packageFlowManager.forwardPackets(decodedMsg.getHeader().getSeqNum(),
						new HoldedObject<ReverseProxyDecodedPacket>(new ReverseProxyDecodedPacket(clientConnectionKey, receivedMessage.from.toString(), decodedMsg)));
			}
		}

	}
}
