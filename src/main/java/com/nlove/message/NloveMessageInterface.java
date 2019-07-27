package com.nlove.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

public interface NloveMessageInterface {

	@JsonProperty(access = Access.WRITE_ONLY)
	public MessageTypeEnum getMessageType();
}
