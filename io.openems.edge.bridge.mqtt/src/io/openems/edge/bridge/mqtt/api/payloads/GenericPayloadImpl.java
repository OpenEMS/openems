package io.openems.edge.bridge.mqtt.api.payloads;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class GenericPayloadImpl extends AbstractPayload implements Payload {

	protected String deviceId;

	public GenericPayloadImpl(Map<ChannelId, String> payloadKeyToChannelIdMap, String deviceId) {
		super(payloadKeyToChannelIdMap, "timestamp", "yyyy-MM-dd'T'HH:mm:ss.SSSxxx", true);
		this.deviceId = deviceId;
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

	@Override
	public Payload build(OpenemsComponent component) {
		this.payload.addProperty("device", this.deviceId);
		return super.build(component);
	}
}
