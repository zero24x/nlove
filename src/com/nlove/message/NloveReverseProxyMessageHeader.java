package com.nlove.message;

public class NloveReverseProxyMessageHeader {

	private int clientPort;
	private boolean ack;
	private boolean socketClosed;
	private int seqNum;
	private int ackNum;

	public int getClientPort() {
		return clientPort;
	}

	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}

	public void setIsAck(boolean isAck) {
		this.ack = isAck;
	}

	public Boolean getSocketClosed() {
		return socketClosed;
	}

	public void setSocketClosed(boolean socketClosed) {
		this.socketClosed = socketClosed;
	}

	public int getSeqNum() {
		return seqNum;
	}

	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}

	public int getAckNum() {
		return ackNum;
	}

	public void setAckNum(int ackNum) {
		this.ackNum = ackNum;
	}

	public boolean isAck() {
		return this.ack;
	}

}
