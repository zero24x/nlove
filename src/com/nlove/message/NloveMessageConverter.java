package com.nlove.message;

import org.bouncycastle.util.Arrays;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.protobuf.ByteString;
import com.nlove.util.ByteUtil;

import jsmith.nknsdk.client.NKNClient.ReceivedMessage;

public class NloveMessageConverter {

	public static String MAGIC_IDENTIFIER = "<<NLOV>>";
	private String name;
	private ObjectMapper mapper;

	public NloveMessageConverter(String name) {
		this.name = name;
		this.mapper = new ObjectMapper().registerModule(new JsonOrgModule());
	}

	public NloveMessageInterface parseMsg(ReceivedMessage msg) {

		if (!msg.isText) {
			return null;
		}

		JSONObject jsonMsg = new JSONObject(msg.textData.substring(MAGIC_IDENTIFIER.length()));

		String type = jsonMsg.getString("type");
		JSONObject payload = jsonMsg.getJSONObject("payload");

		if (type.equals(MessageTypeEnum.CHAT.toString())) {
			return mapper.convertValue(payload, NloveChatMessage.class);
		} else if (type.equals(MessageTypeEnum.SEARCH_REQUEST.toString())) {
			return mapper.convertValue(payload, NloveSearchRequestMessage.class);
		} else if (type.equals(MessageTypeEnum.SEARCH_REQUEST_REPLY.toString())) {
			return mapper.convertValue(payload, NloveSearchRequestReplyMessage.class);
		} else if (type.equals(MessageTypeEnum.DOWNLOAD_REQUEST.toString())) {
			return mapper.convertValue(payload, NloveDownloadRequestMessage.class);
		} else if (type.equals(MessageTypeEnum.DOWNLOAD_REQUEST_REPLY.toString())) {
			return mapper.convertValue(payload, NloveDownloadRequestReplyMessage.class);
		} else if (type.equals(MessageTypeEnum.DOWNLOAD_DATA.toString())) {
			return mapper.convertValue(payload, NloveDownloadDataMessage.class);
		} else if (type.equals(MessageTypeEnum.REVERSE_PROXY_CONNECT.toString())) {
			return mapper.convertValue(payload, NloveReverseProxyConnectMessage.class);
		}

		return null;
	}

	public String toMsgString(NloveMessageInterface m) {

		NloveMessageContainer c = new NloveMessageContainer();
		c.setPayload(m);
		c.setType(m.getMessageType());

		JSONObject msg = new JSONObject();

		try {
			return String.format("%s%s", MAGIC_IDENTIFIER, mapper.writeValueAsString(c));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public byte[] makeHeaderBytes(int clientPort) {
		NloveMessageHeader header = new NloveMessageHeader();
		header.setClientPort(clientPort);

		try {
			byte[] res = (this.mapper.writeValueAsString(header) + NloveMessageConverter.MAGIC_IDENTIFIER).getBytes();
			return res;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public NloveMessageHeader parseHeader(ByteString data) {
		byte[] fullData = data.toByteArray();
		int headerEndPos = ByteUtil.indexOf(fullData, NloveMessageConverter.MAGIC_IDENTIFIER.toString().getBytes());

		byte[] header = Arrays.copyOfRange(fullData, 0, headerEndPos);

		String headerString = new String(header);
		JSONObject headerJson = new JSONObject(headerString);

		NloveMessageHeader res = mapper.convertValue(headerJson, NloveMessageHeader.class);
		return res;
	}

}
