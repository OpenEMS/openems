package io.openems.edge.io.shelly.shelly25;

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
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
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
		var b = new StringBuilder();
		var i = 1;
		for (BooleanWriteChannel channel : this.digitalOutputChannels) {
			String valueText;
			var valueOpt = channel.value().asOptional();
			if (valueOpt.isPresent()) {
				valueText = valueOpt.get() ? "x" : "-";
			} else {
				valueText = "Unknown";
			}
			b.append(valueText);
			if (i < this.digitalOutputChannels.length) {
				b.append("|");
			}
			i++;
		}
		return b.toString();
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

	record RelayState(Boolean relayIsOn, Boolean overtemp, Boolean overpower) {
	}

	private static RelayState parseRelay(JsonElement relay) throws OpenemsNamedException {
		Boolean relayIsOn = JsonUtils.getAsBoolean(relay, "ison");
		Boolean overtemp = JsonUtils.getAsBoolean(relay, "overtemperature");
		Boolean overpower = JsonUtils.getAsBoolean(relay, "overpower");
		return new RelayState(relayIsOn, overtemp, overpower);
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
	private void processHttpResult(JsonElement result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);
		RelayState relay1State = null;
		RelayState relay2State = null;

		try {
			JsonElement jsonElement = JsonUtils.getAsJsonElement(result);
			final var relays = JsonUtils.getAsJsonArray(jsonElement, "relays");

			for (int i = 0; i < 2; i++) {
				if (i == 0) {
					relay1State = parseRelay(JsonUtils.getAsJsonObject(relays.get(0)));
				} else if (i == 1) {
					relay2State = parseRelay(JsonUtils.getAsJsonObject(relays.get(1)));
				}
			}
			this._setSlaveCommunicationFailed(false);
		} catch (OpenemsNamedException e) {
			this.logDebug(this.log, e.getMessage());
		}

		this._setRelay1(relay1State.relayIsOn());
		this._setRelay2(relay2State.relayIsOn());

		this._setRelay1Overtemp(relay1State.overtemp());
		this._setRelay2Overtemp(relay2State.overtemp());

		this._setRelay1Overpower(relay1State.overpower());
		this._setRelay2Overpower(relay2State.overpower());

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
