package com.nlove.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nlove.command.NloveCommand;

import jsmith.nknsdk.client.NKNClient;
import jsmith.nknsdk.client.NKNClient.ReceivedMessage;

public class ProviderCommandHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(NKNClient.class);
	   
	public void handle(ReceivedMessage receivedMessage) {
		
		if (!receivedMessage.isText ||  receivedMessage.textData.startsWith(NloveCommand.MAGIC_IDENTIFIER)) {
			return;
		}
		
		String msg = receivedMessage.textData.substring(0, 8);
		
		LOG.info("Received msg: " + msg);
		
			if (receivedMessage.isText) {
			
				if(msg.startsWith("SEARCH")) {
					this.handleSearch(msg);
				}
			}
	
	}
	
	private String handleSearch(String textData) {
		return "matching file: xyz.txt";
	}
}
