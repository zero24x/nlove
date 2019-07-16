package com.nlove.download;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.nlove.message.NloveDownloadDataMessage;
import com.nlove.message.NloveDownloadRequestReplyMessage;

import jsmith.nknsdk.client.NKNClient.ReceivedMessage;

public class FileDownloadManager {

	private List<DownloadRequest> currentDownloadRequests = new ArrayList<DownloadRequest>();

	public void handleDownloadBinaryReceived(ReceivedMessage receivedMessage) {

		if (!currentDownloadRequests.stream()
				.anyMatch(x -> x.getMessageID().equals(receivedMessage.msgId.toString()))) {
			return;
		}
	}

	public void handleDownloadRequestReplyMessage(String messageId, NloveDownloadRequestReplyMessage m) {
		Optional<DownloadRequest> req = this.currentDownloadRequests.stream()
				.filter(r -> r.getFileID().equals(messageId) && r.getMessageID().equals(messageId)).findFirst();
		req.get().setFileSizeBytes(m.getFileSizeBytes());

		String fileName = FileSystems.getDefault().getPath(req.get().getFileID()).getFileName().toString();

		long unixTimestamp = Instant.now().getEpochSecond();

		Path localPath = Paths.get(System.getProperty("user.dir"), File.separator.toString(), "download",
				File.separator.toString(), unixTimestamp + fileName);

		req.get().setLocalPath(localPath);

		if (!req.isPresent()) {
			return;
		}

	}

	public void handleDownloadRequestDataMessage(String messageId, NloveDownloadDataMessage m) {
		Optional<DownloadRequest> req = this.currentDownloadRequests.stream()
				.filter(r -> r.getFileID().equals(messageId) && r.getMessageID().equals(messageId)).findFirst();

		if (!req.isPresent()) {
			return;
		}

		try {
			Files.write(req.get().getLocalPath(), m.getData(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
