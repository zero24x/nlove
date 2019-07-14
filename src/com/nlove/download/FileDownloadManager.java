package com.nlove.download;

import java.util.ArrayList;
import java.util.List;

public class FileDownloadManager {

	private List<DownloadRequest> currentDownloadRequests = new ArrayList<DownloadRequest>();

	public void handleDownloadBinaryReceived(String messageId) {

		if (!currentDownloadRequests.stream().anyMatch(x -> x.getMessageID().equals(messageId))) {
			return;
		}
	}

	public void registerFileDownloadRequest(String message) {

	}
}
