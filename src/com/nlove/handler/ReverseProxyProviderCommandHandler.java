package com.nlove.handler;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

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

public class ReverseProxyProviderCommandHandler {

	private NKNClient nknClient;
	private Identity providerIdentity;

	static String PROVIDER_IDENTIFIER = "nlove-reverseproxy-provider3";
	private Wallet wallet;
	private NloveMessageConverter nloveMessageConverter = new NloveMessageConverter("REVERSE_PROXY");
	private static final Logger LOG = LoggerFactory.getLogger(ReverseProxyProviderCommandHandler.class);
	private ConcurrentHashMap<String, CommandHandlerPackageFlowManager> packageFlowManagers = new ConcurrentHashMap<String, CommandHandlerPackageFlowManager>();

	private AtomicInteger numConnections = new AtomicInteger();
	private static int MAX_CONNECTIONS = 100;

	private Thread packetResenderThread;

	public void start() throws NKNClientException, WalletException, IOException {

		File walletFile = new File("walletForReverseProxy.dat");

		if (!walletFile.exists())
			Wallet.createNew().save(walletFile, "");

		final Wallet wallet = Wallet.load(walletFile, "");
		this.wallet = wallet;

		this.providerIdentity = new Identity(ReverseProxyProviderCommandHandler.PROVIDER_IDENTIFIER, wallet);
		this.nknClient = new NKNClient(this.providerIdentity);
		LOG.info("Reverse proxy provider handler provider ID:" + this.providerIdentity.getFullIdentifier());

		this.nknClient.setNoAutomaticACKs(true);
		this.nknClient.onNewMessage(msg -> {
			try {
				this.handleProviderClientMessage(msg);
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

	Phaser unackedPacketsPhaser = new Phaser(2);

	private void handleProviderClientMessage(ReceivedMessage receivedMessage) throws IOException {

		new Thread() {
			public void run() {

				if (receivedMessage.isText) {
					NloveMessageInterface c = nloveMessageConverter.parseMsg(receivedMessage);
					if (c instanceof NloveReverseProxyConnectMessage) {
						int clientPort = ((NloveReverseProxyConnectMessage) c).getClientPort();
						String clientConnectionKey = String.format("%s:%s", receivedMessage.from, clientPort);

						if (numConnections.get() >= MAX_CONNECTIONS) {
							LOG.info("Not accepting new client connection , max. connections of {} reached", MAX_CONNECTIONS);
							return;
						}

						Thread.currentThread().setName(String.format("handleProviderClientMessage %s", receivedMessage.from));

						try {
							Socket serviceSocket = new Socket("localhost", 80);

							int numConnectionsNow = numConnections.incrementAndGet();
							LOG.info("Num connections = {}", numConnectionsNow);
							CommandHandlerPackageFlowManager packageFlowManager = new CommandHandlerPackageFlowManager(clientConnectionKey, nknClient);
							packageFlowManager.getClientConnections().put(clientConnectionKey, serviceSocket);
							packageFlowManagers.put(clientConnectionKey, packageFlowManager);
							packageFlowManager.start();
							BufferedInputStream serviceSocketInputStream = new BufferedInputStream(serviceSocket.getInputStream());

							int bytesRead = 0;
							byte[] buffer = new byte[8192];
							int newSeqNum = 0;

							try {
								while ((bytesRead = serviceSocketInputStream.read(buffer)) != -1) {
									newSeqNum = newSeqNum + bytesRead;
									packageFlowManager.setSeqNum(newSeqNum);

									byte[] headerBytes = nloveMessageConverter.makeHeaderBytes(false, clientPort, false, packageFlowManager.getAckNum(), newSeqNum);
									ByteArrayOutputStream bos = new ByteArrayOutputStream(headerBytes.length + buffer.length);
									bos.write(headerBytes);
									bos.write(buffer, 0, bytesRead);
									bos.flush();

									byte[] bytesToSend = bos.toByteArray();

									nknClient.sendBinaryMessageAsync(receivedMessage.from, bytesToSend);

									packageFlowManager.getUnackedPackets().put(newSeqNum,
											new HoldedObject<ReverseProxyReplyPacket>(new ReverseProxyReplyPacket(receivedMessage.from, bytesToSend)));

									unackedPacketsPhaser.arriveAndAwaitAdvance();
									if (packageFlowManager.getUnackedPackets().size() >= 1) {
										LOG.debug("Unacked packets too big, pausing");
									}

								}

							} catch (Exception e) {
								e.printStackTrace();
							}

							try {

								byte[] payload = new byte[] { 1 };
								newSeqNum += payload.length;
								packageFlowManager.setSeqNum(newSeqNum);
								byte[] headerBytes = nloveMessageConverter.makeHeaderBytes(false, clientPort, true, packageFlowManager.getAckNum(), newSeqNum);
								ByteArrayOutputStream bos = new ByteArrayOutputStream(headerBytes.length + payload.length);
								bos.write(headerBytes);
								bos.write(payload);
								bos.flush();

								byte[] bytesToSend = bos.toByteArray();

								nknClient.sendBinaryMessageAsync(receivedMessage.from, bytesToSend);
								packageFlowManager.getUnackedPackets().put(newSeqNum,
										new HoldedObject<ReverseProxyReplyPacket>(new ReverseProxyReplyPacket(receivedMessage.from, bytesToSend)));

								packageFlowManager.stop();
								packageFlowManagers.remove(clientConnectionKey);

								if (!serviceSocket.isClosed()) {
									serviceSocketInputStream.close();
								}

								numConnectionsNow = numConnections.decrementAndGet();
								LOG.info("Num connections = {}", numConnectionsNow);

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
		}.start();

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
					unackedPacketsPhaser.arriveAndAwaitAdvance();
				}
			}

			if (packageFlowManager == null) {
				LOG.debug("No packageFlowManager found for conn {}", clientConnectionKey);
				return;
			}

			Socket serviceSocket = packageFlowManager.getClientConnections().get(clientConnectionKey);

			if (serviceSocket != null) {
				packageFlowManager.forwardPackets(decodedMsg.getHeader().getSeqNum(),
						new HoldedObject<ReverseProxyDecodedPacket>(new ReverseProxyDecodedPacket(clientConnectionKey, receivedMessage.from.toString(), decodedMsg)));

			}

		}
	}
}
