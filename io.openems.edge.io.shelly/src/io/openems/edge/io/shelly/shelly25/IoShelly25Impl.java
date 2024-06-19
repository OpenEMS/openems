package io.openems.edge.io.shelly.shelly25;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.io.api.ShellyUtils.generateDebugLog;

import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.25", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})

public class IoShelly25Impl extends AbstractOpenemsComponent
		implements IoShelly25, DigitalOutput, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(IoShelly25Impl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private String baseUrl;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShelly25Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShelly25.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShelly25.ChannelId.RELAY_1), //
				this.channel(IoShelly25.ChannelId.RELAY_2), //
		};
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();

		if (this.isEnabled()) {
			this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/status", this::processHttpResult);
		}
	}

	@Deactivate
	protected void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		super.deactivate();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this.digitalOutputChannels);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
			-> this.eventExecuteWrite();
		}
	}

	private record RelayState(Boolean relayIsOn, Boolean overtemp, Boolean overpower) {
		private static RelayState from(JsonElement relay) throws OpenemsNamedException {
			var relayIsOn = getAsBoolean(relay, "ison");
			var overtemp = getAsBoolean(relay, "overtemperature");
			var overpower = getAsBoolean(relay, "overpower");
			return new RelayState(relayIsOn, overtemp, overpower);
		}

		private void applyChannels(IoShelly25 component, IoShelly25.ChannelId relayChannel,
				IoShelly25.ChannelId overtempChannel, IoShelly25.ChannelId overpowerChannel) {
			component.channel(relayChannel).setNextValue(this.relayIsOn);
			component.channel(overtempChannel).setNextValue(this.overtemp);
			component.channel(overpowerChannel).setNextValue(this.overpower);
		}
	}

	/**
	 * Execute on Cycle Event "Before Process Image".
	 * 
	 * @param result The JSON element containing the result of the HTTP request.
	 * @param error  The throwable error, if any occurred during the HTTP request.
	 * @throws OpenemsNamedException if the processing of the HTTP result fails or
	 *                               communication with the slave device is
	 *                               unsuccessful.
	 */
	private void processHttpResult(HttpResponse<JsonElement> result, HttpError error) {
		var slaveCommunicationFailed = result == null;
		var relay1State = new RelayState(null, null, null);
		var relay2State = new RelayState(null, null, null);

		if (error != null) {
			this.logDebug(this.log, error.getMessage());

		} else {
			try {
				final var relays = getAsJsonArray(result.data(), "relays");
				relay1State = RelayState.from(getAsJsonObject(relays.get(0)));
				relay2State = RelayState.from(getAsJsonObject(relays.get(1)));

			} catch (OpenemsNamedException | IndexOutOfBoundsException e) {
				this.logDebug(this.log, e.getMessage());
				slaveCommunicationFailed = true;
			}
		}

		this._setSlaveCommunicationFailed(slaveCommunicationFailed);
		relay1State.applyChannels(this, IoShelly25.ChannelId.RELAY_1, //
				IoShelly25.ChannelId.RELAY_1_OVERTEMP, IoShelly25.ChannelId.RELAY_1_OVERPOWER);
		relay2State.applyChannels(this, IoShelly25.ChannelId.RELAY_2, //
				IoShelly25.ChannelId.RELAY_2_OVERTEMP, IoShelly25.ChannelId.RELAY_2_OVERPOWER);
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 */
	private void eventExecuteWrite() {
		for (int i = 0; i < this.digitalOutputChannels.length; i++) {
			this.executeWrite(this.digitalOutputChannels[i], i);
		}
	}

	private void executeWrite(BooleanWriteChannel channel, int index) {
		var readValue = channel.value().get();
		var writeValue = channel.getNextWriteValueAndReset();
		if (writeValue.isEmpty()) {
			return;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			return;
		}
		final String url = this.baseUrl + "/relay/" + index + "?turn=" + (writeValue.get() ? "on" : "off");
		this.httpBridge.get(url).whenComplete((t, e) -> {
			this._setSlaveCommunicationFailed(e != null);
			if (e == null) {
				this.logInfo(this.log, "Executed write successfully for URL: " + url);
			} else {
				this.logError(this.log, "Failed to execute write for URL: " + url + "; Error: " + e.getMessage());
			}
		});
	}
}