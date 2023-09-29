package io.openems.edge.bridge.mqtt.api.payloads;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This baseclass provides the ability to create a Basic Payload (Json) to be
 * able to send to the broker (publish). Can also handle subscribed Topic
 * Payloads, by mapping the values of a key to a Channel of a referenced
 * OpenEmsComponent. Implements the {@link Payload} interface. <br>
 * Example Payload: <br>
 * { <br>
 * "time":"2023-07-24T16:30:00+02:00", <br>
 * "electric_power_plus-1":169530, <br>
 * "electric_renergy_plus-1":1983418, <br>
 * } <br>
 * Electric Power plus Mapped to ActivePower <br>
 * Electric RenergyPlus-1 Mapped to ActiveProductionEnergy
 */
public abstract class AbstractPayload implements Payload {

	protected final Logger log = LoggerFactory.getLogger(AbstractPayload.class);

	protected JsonObject payload = new JsonObject();
	protected DateTimeFormatter formatter;
	protected String timeKey;
	protected Map<ChannelId, String> payloadKeyToChannelIdMap;

	// Sometimes a true / false cannot be processed correctly by e.g. an influx that is connected to the broker
	// -> therefor ability to change true/false to 0/1
	protected boolean boolConversion;

	protected AbstractPayload(Map<ChannelId, String> payloadKeyToChannelIdMap, String timeKey, String timePattern,
			boolean boolConversion) {
		this.payloadKeyToChannelIdMap = payloadKeyToChannelIdMap;
		this.timeKey = timeKey;
		this.formatter = DateTimeFormatter.ofPattern(timePattern);
		this.boolConversion = boolConversion;
	}

	@Override
	public Payload build(OpenemsComponent component) {
		var channels = this.getFilteredChannels(component);
		channels.forEach(channel -> {
			channel.value().ifPresent(value -> {
				JsonElement channelObj;
				var shouldConvertBool = this.boolConversion && channel.getType().equals(OpenemsType.BOOLEAN);
				if (shouldConvertBool) {
					boolean val = (Boolean) value;
					channelObj = new Gson().toJsonTree(val ? 1 : 0);
				} else {
					channelObj = new Gson().toJsonTree(value);
				}
				this.payload.add(this.payloadKeyToChannelIdMap.get(channel.channelId()), channelObj);
			});
		});
		return this;
	}

	protected List<Channel<?>> getFilteredChannels(OpenemsComponent component) {
		return component.channels().stream()
				.filter(channel -> this.payloadKeyToChannelIdMap.containsKey(channel.channelId())).toList();
	}

	@Override
	public String getPayloadMessage() {
		return this.payload.toString();
	}

	@Override
	public void handlePayloadToComponent(OpenemsComponent comp) {
		// jsonobject payload
		// map channelId, key
		var channels = this.getFilteredChannels(comp);
		channels.forEach(channel -> {
			var key = this.payloadKeyToChannelIdMap.get(channel.channelId());
			if (this.payload.has(key)) {
				var value = this.payload.get(key);
				if (value != null && !value.getAsString().equals("")) {
					if (channel instanceof WriteChannel<?> writeChannel) {
						try {
							writeChannel.setNextWriteValueFromObject(value.getAsString());
						} catch (OpenemsError.OpenemsNamedException e) {
							this.log.warn(this.getClass().getName() + ": Couldn't update Channel: " + channel
									+ " Cause: " + e.getCause());
						}
					} else {
						channel.setNextValue(value.getAsString());
					}
				}
			}
		});
	}

	@Override
	public void setTime(ZonedDateTime time) {
		this.payload.addProperty(this.timeKey, time.format(this.formatter));
	}
}
