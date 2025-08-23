package io.openems.edge.io.shelly.shelly1;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.io.shelly.common.Utils.generateDebugLog;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.shelly.common.ShellyCommon;
import io.openems.edge.io.shelly.common.ShellyDeviceModels;
import io.openems.edge.io.shelly.common.Utils;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly1", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_EXECUTE_WRITE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShelly1Impl extends AbstractOpenemsComponent
		implements IoShelly1, DigitalOutput, OpenemsComponent, ShellyCommon, TimedataProvider, EventHandler {

	private final Logger log = LoggerFactory.getLogger(IoShelly1.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private String baseUrl;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference(cardinality = MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShelly1Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShelly1.ChannelId.values(), //
				ShellyCommon.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShelly1.ChannelId.RELAY) //
		};
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();

		if (!this.isEnabled()) {
			return;
		}

		// Subscribe to check auth status and model validation on activation
		Utils.subscribeAuthenticationCheck(this.baseUrl, this.httpBridge, this, this.log, ShellyDeviceModels.SHELLY1);
		
		this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/status", this::processHttpResult);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
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
		case TOPIC_CYCLE_EXECUTE_WRITE //
			-> this.executeWrite(this.getRelayChannel(), 0);
		}
	}

	private record RelayState(Boolean relayIsOn) {
		private static RelayState from(JsonElement relay) throws OpenemsNamedException {
			var relayIsOn = getAsBoolean(relay, "ison");
			return new RelayState(relayIsOn);
		}

		private void applyChannels(IoShelly1 component, IoShelly1.ChannelId relayChannel) {
			component.channel(relayChannel).setNextValue(this.relayIsOn);
		}
	}

	private void processHttpResult(HttpResponse<JsonElement> result, HttpError error) {
		var slaveCommunicationFailed = result == null;
		var relay1State = new RelayState(null);

		if (error != null) {
			this.logDebug(this.log, error.getMessage());

		} else {
			try {
				final var relays = getAsJsonArray(result.data(), "relays");
				relay1State = RelayState.from(getAsJsonObject(relays.get(0)));

			} catch (OpenemsNamedException | IndexOutOfBoundsException e) {
				this.logDebug(this.log, e.getMessage());
				slaveCommunicationFailed = true;
			}
		}

		this._setSlaveCommunicationFailed(slaveCommunicationFailed);
		relay1State.applyChannels(this, IoShelly1.ChannelId.RELAY);
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 * 
	 * @param channel write channel
	 * @param index   index
	 */
	private void executeWrite(BooleanWriteChannel channel, int index) {
		var readValue = channel.value().get();
		var writeValue = channel.getNextWriteValueAndReset();
		if (writeValue.isEmpty()) {
			// no write value
			return;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			// read value equals write value, so no need to write
			return;
		}
		final String url = this.baseUrl + "/relay/" + index + "?turn=" + (writeValue.get() ? "on" : "off");
		this.httpBridge.get(url).whenComplete((response, error) -> {
			if (error != null) {
				this.logError(this.log, "HTTP request failed: " + error.getMessage());
				this._setSlaveCommunicationFailed(true);
			} else {
				// Optionally log success or handle response
				this._setSlaveCommunicationFailed(false);
			}
		});
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}