package com.nlove.handler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
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
	private ConcurrentHashMap<String, Socket> clientConnections = new ConcurrentHashMap<String, Socket>();
	private ServerSocket reverseProxySocket;

	public void start() throws NKNClientException, WalletException, IOException {

		File walletFile = new File("walletForReverseProxyClient.dat");

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
	}

	public void connectToServiceProvider(String destinationFullIdentifier) throws IOException {

		if (reverseProxySocket == null || reverseProxySocket.isClosed()) {
			reverseProxySocket = new ServerSocket(222);
		}
		System.out.println(String.format("To connect to %s, connect to 127.0.0.1:222", destinationFullIdentifier));

		while (true) {
			final Socket reverseProxyClientSocket = reverseProxySocket.accept();

			new Thread() {
				public void run() {
					handleConnectToServiceProviderIncomingConnection(destinationFullIdentifier, reverseProxyClientSocket);
				}
			}.start();
		}
	}

	private void handleConnectToServiceProviderIncomingConnection(String destinationFullIdentifier, Socket reverseProxyClientSocket) {

		String clientConnectionKey = String.format("%s:%s", destinationFullIdentifier, reverseProxyClientSocket.getPort());

		Thread.currentThread().setName(String.format("connectToServiceProvider %s", clientConnectionKey));

		System.out.println(String.format("Accepted local client-to-provider connection 127.0.0.1:" + reverseProxyClientSocket.getPort()));
		CommandHandlerPackageFlowManager packageFlowManager = new CommandHandlerPackageFlowManager(reverseProxyClientSocket, clientConnectionKey, nknClient);
		packageFlowManagers.put(clientConnectionKey, packageFlowManager);
		packageFlowManager.start();

		clientConnections.put(clientConnectionKey, reverseProxyClientSocket);

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

			byte[] buffer = new byte[50000];
			ByteBuffer buf = ByteBuffer.allocateDirect(buffer.length + 100);

			try {
				while ((bytesRead = clientIn.read(buffer)) > 0) {

					int ackNum = packageFlowManager.getAckNum();
					int seqNum = packageFlowManager.getSeqNum();

					byte[] headerBytes = nloveMessageConverter.makeHeaderBytes(false, reverseProxyClientSocket.getPort(), false, ackNum, seqNum);

					buf.clear();
					buf.put(headerBytes);
					buf.put(buffer, 0, bytesRead);
					buf.flip();

					ByteString data = ByteString.copyFrom(buf);

					nknClient.sendBinaryMessageAsync(destinationFullIdentifier, data);
					packageFlowManager.getUnackedPackets().put(packageFlowManager.getSeqNum(),
							new HoldedObject<ReverseProxyReplyPacket>(new ReverseProxyReplyPacket(destinationFullIdentifier, data)));
					packageFlowManager.setSeqNum(seqNum + bytesRead);
					if (packageFlowManager.getUnackedPackets().size() >= CommandHandlerPackageFlowManager.MAX_UNACKED_PACKETS) {
						LOG.debug("Unacked packets too big, pausing");
						Thread.sleep(100);
					}
				}
				LOG.debug("Read reverse proxy client bytes: -1");
			} catch (SocketException e) {
				e.printStackTrace();
			}

			try {
				LOG.info("Done reading, sending client {} disconnection message", destinationFullIdentifier);

				byte[] payload = new byte[] { 1 };
				byte[] headerBytes = nloveMessageConverter.makeHeaderBytes(false, reverseProxyClientSocket.getPort(), true, packageFlowManager.getAckNum(),
						packageFlowManager.getSeqNum());
				ByteBuffer buf2 = ByteBuffer.allocate(headerBytes.length + payload.length);
				buf2.put(headerBytes);
				buf2.put(payload);
				buf2.flip();
				ByteString data = ByteString.copyFrom(buf2);

				nknClient.sendBinaryMessageAsync(destinationFullIdentifier, data);
				packageFlowManager.getUnackedPackets().put(packageFlowManager.getSeqNum(),
						new HoldedObject<ReverseProxyReplyPacket>(new ReverseProxyReplyPacket(destinationFullIdentifier, data)));
				packageFlowManager.addToSeqNum(payload.length);

				packageFlowManager.stop();
				packageFlowManagers.remove(clientConnectionKey);

				if (reverseProxyClientSocket.isClosed()) {
					clientIn.close();
				}

				nknClient.sendBinaryMessageAsync(destinationFullIdentifier,
						nloveMessageConverter.makeHeaderBytes(false, reverseProxyClientSocket.getPort(), true, packageFlowManager.getAckNum(), packageFlowManager.getSeqNum()));

			} catch (IOException e) {
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleClientClientMessage(ReceivedMessage receivedMessage) throws IOException {

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

			Socket reverseProxyClientSocket = clientConnections.get(clientConnectionKey);

			if (reverseProxyClientSocket != null) {
				packageFlowManager.forwardPackets(decodedMsg.getHeader().getSeqNum(),
						new HoldedObject<ReverseProxyDecodedPacket>(new ReverseProxyDecodedPacket(clientConnectionKey, receivedMessage.from.toString(), decodedMsg)));
			}
		}

	}
}
