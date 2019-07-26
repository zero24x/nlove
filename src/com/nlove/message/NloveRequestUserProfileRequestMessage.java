package com.nlove.message;

public class NloveRequestUserProfileRequestMessage implements NloveMessageInterface {
	public MessageTypeEnum getMessageType() {
		return MessageTypeEnum.REQUEST_USER_PROFILE_REQUEST;
	}
}
