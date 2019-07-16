package com.nlove.download;

import java.nio.file.Path;

public class DownloadRequest {

	private String messageID;
	private String fileID;
	private long fileSizeBytes;
	private long bytesTransferred;
	private Path localPath;

	public String getMessageID() {
		return messageID;
	}

	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}

	public String getFileID() {
		return fileID;
	}

	public void setFileID(String fileID) {
		this.fileID = fileID;
	}

	public long getFileSizeBytes() {
		return fileSizeBytes;
	}

	public void setFileSizeBytes(long fileSizeBytes) {
		this.fileSizeBytes = fileSizeBytes;
	}

	public long getBytesTransferred() {
		return bytesTransferred;
	}

	public void setBytesTransferred(long bytesTransferred) {
		this.bytesTransferred = bytesTransferred;
	}

	public Path getLocalPath() {
		return localPath;
	}

	public void setLocalPath(Path localPath) {
		this.localPath = localPath;
	}

}
