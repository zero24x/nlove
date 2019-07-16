package com.nlove.message;

public class NloveDownloadDataMessage implements NloveMessageInterface {

	private String fileID;
	private long offset;
	private Boolean lastChunk;
	private byte[] data;

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public MessageTypeEnum getMessageType() {
		return MessageTypeEnum.DOWNLOAD_DATA;
	}

	public String getFileID() {
		return fileID;
	}

	public void setFileID(String fileID) {
		this.fileID = fileID;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public Boolean getLastChunk() {
		return lastChunk;
	}

	public void setLastChunk(Boolean lastChunk) {
		this.lastChunk = lastChunk;
	}

}
