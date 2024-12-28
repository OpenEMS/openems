package io.openems.edge.bridge.mqtt.api.payloads;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.edge.common.channel.ChannelId;

public class GenericPayloadImpl extends AbstractPayload implements Payload {

	public GenericPayloadImpl(Map<ChannelId, String> payloadKeyToChannelIdMap) {
		super(payloadKeyToChannelIdMap, "timestamp", "yyyy-MM-dd'T'HH:mm:ss.SSSxxx", true);
	}

	@Override
	public void updatePayloadAfterCallback(String payload) {
		this.updatePayloadAfterCallback(JsonParser.parseString(payload).getAsJsonObject());
	}

	@Override
	public void updatePayloadAfterCallback(JsonObject payload) {
		payload.asMap().forEach((key, value) -> {
			if (value.isJsonPrimitive()) {
				this.payload.add(key, value);
			} else {
				this.payload.addProperty(key, value.getAsString());
			}
		});
	}

	@Override
	public void updatePayloadAfterCallback(Payload payload) {
		this.updatePayloadAfterCallback(payload.getPayloadMessage());
	}
}
