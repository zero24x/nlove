package com.nlove.message;

public class NloveDownloadRequestMessage implements NloveMessageInterface {

	private String fileId;

	public MessageTypeEnum getMessageType() {
		return MessageTypeEnum.DOWNLOAD_REQUEST;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

}
