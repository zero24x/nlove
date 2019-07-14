package com.nlove.message;

public class NloveMessage {

	public static String MAGIC_IDENTIFIER = "<<NLOV>>";

	public String search(String term) {
		return MAGIC_IDENTIFIER + "SEARCH " + term;
	}

	public String chat(String text) {
		return MAGIC_IDENTIFIER + "CHAT " + text;
	}

	public String download(String fileId) {
		return MAGIC_IDENTIFIER + "DOWNLOAD " + fileId;
	}
}
