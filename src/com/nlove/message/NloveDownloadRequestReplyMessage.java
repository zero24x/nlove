package com.nlove.message;

public class NloveDownloadRequestReplyMessage implements NloveMessageInterface {

	private long fileSizeBytes;
	private String error;

	public MessageTypeEnum getMessageType() {
		return MessageTypeEnum.DOWNLOAD_REQUEST_REPLY;
	}

	public long getFileSizeBytes() {
		return fileSizeBytes;
	}

	public void setFileSizeBytes(long fileSizeBytes) {
		this.fileSizeBytes = fileSizeBytes;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
}
