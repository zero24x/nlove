package com.nlove.message;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

import jsmith.nknsdk.client.NKNClient.ReceivedMessage;

public class NloveMessageParser {

	public static String MAGIC_IDENTIFIER = "<<NLOV>>";
	private String name;

	public NloveMessageParser(String name) {
		this.name = name;
	}

	public NloveMessageInterface parse(ReceivedMessage msg) {

		if (!msg.isText) {
			return null;
		}

		ObjectMapper mapper = new ObjectMapper().registerModule(new JsonOrgModule());
		JSONObject jsonMsg = new JSONObject(msg.textData.substring(MAGIC_IDENTIFIER.length()));

		String type = jsonMsg.getString("type");
		JSONObject payload = jsonMsg.getJSONObject("payload");

		if (type.equals(MessageTypeEnum.CHAT.toString())) {
			return mapper.convertValue(payload, NloveChatMessage.class);
		}

		return null;
	}

	public String toMsgString(NloveMessageInterface m) {

		NloveMessageContainer c = new NloveMessageContainer();
		c.setPayload(m);
		c.setType(m.getMessageType());

		ObjectMapper mapper = new ObjectMapper().registerModule(new JsonOrgModule());
		JSONObject msg = new JSONObject();

		try {
			return String.format("%s%s", MAGIC_IDENTIFIER, mapper.writeValueAsString(c));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

}
