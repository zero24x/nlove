package com.nlove.handler;

import java.io.IOException;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.provider.HoldedObject;
import com.nlove.provider.ReverseProxyDecodedPacket;
import com.nlove.provider.ReverseProxyReplyPacket;

import jsmith.nknsdk.client.NKNClient;

public class CommandHandlerPackageFlowManager {

	private NKNClient client;
	private AtomicInteger ackNum = new AtomicInteger();
	private AtomicInteger seqNum = new AtomicInteger();
	private String name;
	private ConcurrentSkipListMap<Integer, HoldedObject<ReverseProxyReplyPacket>> unackedPackets = new ConcurrentSkipListMap<Integer, HoldedObject<ReverseProxyReplyPacket>>();
	private static final Logger LOG = LoggerFactory.getLogger(CommandHandlerPackageFlowManager.class);
	public static final int MAX_UNACKED_PACKETS = 100;

	private ConcurrentHashMap<Integer, HoldedObject<ReverseProxyDecodedPacket>> holdedIncomingPackets = new ConcurrentHashMap<Integer, HoldedObject<ReverseProxyDecodedPacket>>();
	private Thread packetResenderThread;
	private Thread reportHoldedPacketsThread;
	private Socket localClientSocket;

	public CommandHandlerPackageFlowManager(Socket localClientSocket, String name, NKNClient client) {
		this.localClientSocket = localClientSocket;
		this.name = name;
		this.client = client;
	}

	public void start() {

		this.ackNum = new AtomicInteger();
		this.seqNum = new AtomicInteger();

		this.reportHoldedPackets();
		this.packetResender();

	}

	public void stop() {

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.ackNum = new AtomicInteger();
		this.seqNum = new AtomicInteger();
		this.packetResenderThread.interrupt();
		this.reportHoldedPacketsThread.interrupt();
		this.holdedIncomingPackets.clear();
		this.unackedPackets.clear();

	}

	public int getAckNum() {
		return this.ackNum.get();
	}

	public void setAckNum(int ackNum) {
		this.ackNum.set(ackNum);
	}

	public void addToAckNum(int toAdd) {
		this.ackNum.addAndGet(toAdd);
	}

	public int getSeqNum() {
		return this.seqNum.get();
	}

	public void setSeqNum(int seqNum) {
		this.seqNum.set(seqNum);
	}

	public void addToSeqNum(int toAdd) {
		this.seqNum.addAndGet(toAdd);
	}

	public ConcurrentHashMap<Integer, HoldedObject<ReverseProxyDecodedPacket>> getHoldedIncomingPackets() {
		return holdedIncomingPackets;
	}

	public void setHoldedIncomingPackets(ConcurrentHashMap<Integer, HoldedObject<ReverseProxyDecodedPacket>> holdedIncomingPackets) {
		this.holdedIncomingPackets = holdedIncomingPackets;
	}

	private void reportHoldedPackets() {

		this.reportHoldedPacketsThread = new Thread() {
			@Override
			public void run() {
				try {
					while (true) {
						int holdedPacketSize = holdedIncomingPackets.size();
						if (holdedPacketSize > 0) {
							LOG.info("{} holdedPackets size: {}", name, holdedIncomingPackets.size());
						}
						Thread.sleep(1000);
					}

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt(); // propagate interrupt
				}
			}
		};
		reportHoldedPacketsThread.setName(String.format("%s - reportHoldedPacketsThread", this.name));
		this.reportHoldedPacketsThread.start();

	}

	public void forwardPackets(Integer seqNum, HoldedObject<ReverseProxyDecodedPacket> receivedPacket) {

		if (receivedPacket.getHoldedObject().getMessage().getHeader().isAck()) {
			return;
		}

		this.holdedIncomingPackets.put(seqNum, receivedPacket);
		if (holdedIncomingPackets.size() >= 1000) {
			LOG.debug("{} holdedPackets grew big!", this.name);
		}

		ReverseProxyDecodedPacket nextPacket = null;
		Boolean continueSearch = true;

		while (continueSearch) {

			continueSearch = false;

			for (Entry<Integer, HoldedObject<ReverseProxyDecodedPacket>> possibleNextPacket : holdedIncomingPackets.entrySet()) {

				int packetSeqNum = possibleNextPacket.getValue().getHoldedObject().getMessage().getHeader().getSeqNum();
				int packetPayloadLen = possibleNextPacket.getValue().getHoldedObject().getMessage().getPayload().length;

				Boolean isNextSequence = false;
				int ackNumNow = ackNum.get();

				isNextSequence = ackNumNow == packetSeqNum;

				if (!isNextSequence) {
					continue;
				}

				nextPacket = possibleNextPacket.getValue().getHoldedObject();

				try {
					if (nextPacket.getMessage().getHeader().getSocketClosed()) {
						LOG.info("{} Received socket closed message from provider", this.name);
						if (!localClientSocket.isClosed()) {
							localClientSocket.getOutputStream().flush();
						}
						try {
							localClientSocket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						holdedIncomingPackets.clear();
					} else {

						if (!localClientSocket.isClosed()) {
							localClientSocket.getOutputStream().write(nextPacket.getMessage().getPayload());
							localClientSocket.getOutputStream().flush();
						}

						if (localClientSocket.isClosed()) {
							this.stop();
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				this.holdedIncomingPackets.remove(possibleNextPacket.getKey());
				continueSearch = true;
				this.addToAckNum(packetPayloadLen);

			}

		}
	}

	public ConcurrentSkipListMap<Integer, HoldedObject<ReverseProxyReplyPacket>> getUnackedPackets() {
		return unackedPackets;
	}

	public void setUnackedPackets(ConcurrentSkipListMap<Integer, HoldedObject<ReverseProxyReplyPacket>> unackedPackets) {
		this.unackedPackets = unackedPackets;
	}

	private void packetResender() {
		this.packetResenderThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					// do stuff
					try {
						int unackedPacketsSize = unackedPackets.size();
						LOG.debug("{} unackedPackets size: {}", name, unackedPacketsSize);
						if (unackedPacketsSize > 0) {
							// resends
							for (Entry<Integer, HoldedObject<ReverseProxyReplyPacket>> packet : unackedPackets.entrySet()) {
								if (packet.getValue().getAge().getSeconds() >= 30) {
									LOG.debug("{} - Resending packet with seqNum={}, no ACK for 30 seconds!", name, packet.getKey());
									client.sendBinaryMessageAsync(packet.getValue().getHoldedObject().getDestination(), packet.getValue().getHoldedObject().getData());
									unackedPackets.put(packet.getKey(), new HoldedObject<ReverseProxyReplyPacket>(packet.getValue().getHoldedObject()));
									Thread.sleep(1000);
								}
							}
						}
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt(); // propagate interrupt
					}
				}
			}
		});
		packetResenderThread.setName(String.format("%s - packetResender", this.name));
		packetResenderThread.start();
	}

}
