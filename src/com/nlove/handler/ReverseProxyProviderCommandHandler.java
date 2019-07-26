package com.nlove.handler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.nlove.config.NloveConfigManager;
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

	private Socket serviceSocket;

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

	}

	private void handleProviderClientMessage(ReceivedMessage receivedMessage) throws IOException {

		new Thread() {
			public void run() {

				if (receivedMessage.isText) {
					NloveMessageInterface c = nloveMessageConverter.parseMsg(receivedMessage);
					if (c instanceof NloveReverseProxyConnectMessage) {
						int clientPort = ((NloveReverseProxyConnectMessage) c).getClientPort();
						String clientConnectionKey = String.format("%s:%s", receivedMessage.from, clientPort);
						LOG.info("Incoming reverse proxy connect message from {}", clientConnectionKey);
						if (numConnections.get() >= MAX_CONNECTIONS) {
							LOG.info("Not accepting new client connection , max. connections of {} reached", MAX_CONNECTIONS);
							return;
						}

						Thread.currentThread().setName(String.format("handleProviderClientMessage %s", receivedMessage.from));

						try {
							serviceSocket = new Socket("localhost", NloveConfigManager.INSTANCE.getConfig().getProviderPort());

							int numConnectionsNow = numConnections.incrementAndGet();
							LOG.info("Num connections = {}", numConnectionsNow);
							CommandHandlerPackageFlowManager packageFlowManager = new CommandHandlerPackageFlowManager(serviceSocket, clientConnectionKey, nknClient);

							packageFlowManagers.put(clientConnectionKey, packageFlowManager);
							packageFlowManager.start();
							BufferedInputStream serviceSocketInputStream = new BufferedInputStream(serviceSocket.getInputStream());

							int bytesRead = 0;
							byte[] buffer = new byte[50000];
							ByteBuffer buf = ByteBuffer.allocateDirect(buffer.length + 100);

							try {
								while ((bytesRead = serviceSocketInputStream.read(buffer)) > 0) {

									int ackNum = packageFlowManager.getAckNum();
									int seqNum = packageFlowManager.getSeqNum();

									byte[] headerBytes = nloveMessageConverter.makeHeaderBytes(false, clientPort, false, ackNum, seqNum);

									buf.clear();
									buf.put(headerBytes);
									buf.put(buffer, 0, bytesRead);
									buf.flip();

									ByteString data = ByteString.copyFrom(buf);

									nknClient.sendBinaryMessageAsync(receivedMessage.from, data);

									packageFlowManager.getUnackedPackets().put(seqNum,
											new HoldedObject<ReverseProxyReplyPacket>(new ReverseProxyReplyPacket(receivedMessage.from, data)));
									packageFlowManager.setSeqNum(seqNum + bytesRead);
									if (packageFlowManager.getUnackedPackets().size() >= CommandHandlerPackageFlowManager.MAX_UNACKED_PACKETS) {
										LOG.debug("Unacked packets too big, pausing");
										Thread.sleep(100);
									}

								}

							} catch (Exception e) {
								e.printStackTrace();
							}

							try {

								LOG.info("Done with reading, sending socket closed msg to {}", clientConnectionKey);
								byte[] payload = new byte[] { 1 };
								byte[] headerBytes = nloveMessageConverter.makeHeaderBytes(false, clientPort, true, packageFlowManager.getAckNum(), packageFlowManager.getSeqNum());
								ByteBuffer buf2 = ByteBuffer.allocateDirect(headerBytes.length + payload.length);
								buf2.put(headerBytes);
								buf2.put(payload);
								buf2.flip();
								ByteString data = ByteString.copyFrom(buf2);

								nknClient.sendBinaryMessageAsync(receivedMessage.from, data);
								packageFlowManager.getUnackedPackets().put(packageFlowManager.getSeqNum(),
										new HoldedObject<ReverseProxyReplyPacket>(new ReverseProxyReplyPacket(receivedMessage.from, data)));
								packageFlowManager.addToSeqNum(payload.length);
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
					int seqNum = decodedMsg.getHeader().getSeqNum();
					packageFlowManager.getUnackedPackets().remove(seqNum);
				}
			}

			if (packageFlowManager == null) {
				LOG.debug("No packageFlowManager found for conn {}", clientConnectionKey);
				return;
			}

			if (serviceSocket != null) {
				packageFlowManager.forwardPackets(decodedMsg.getHeader().getSeqNum(),
						new HoldedObject<ReverseProxyDecodedPacket>(new ReverseProxyDecodedPacket(clientConnectionKey, receivedMessage.from.toString(), decodedMsg)));

			}

		}
	}
}
