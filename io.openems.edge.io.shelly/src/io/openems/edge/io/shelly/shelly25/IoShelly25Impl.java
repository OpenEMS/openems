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
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();

		if (this.isEnabled()) {
			this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/status", (t, u) -> {
				try {
					processHttpResult(t, u);
				} catch (OpenemsNamedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
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
				valueText = valueOpt.get() ? "ON" : "OFF";
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

	/**
	 * Execute on Cycle Event "Before Process Image".
	 * 
	 * @param result The JSON element containing the result of the HTTP request.
	 * @param error  The throwable error, if any occurred during the HTTP request.
	 * @throws OpenemsNamedException
	 */
	private void processHttpResult(JsonElement result, Throwable error) throws OpenemsNamedException {
		this._setSlaveCommunicationFailed(result == null);
		Boolean relay1 = null;
		Boolean overtemp1 = null;
		Boolean overpower1 = null;
		Boolean relay2 = null;
		Boolean overtemp2 = null;
		Boolean overpower2 = null;

		try {
			JsonElement jsonElement = JsonUtils.getAsJsonElement(result);
			final var relays = JsonUtils.getAsJsonArray(jsonElement, "relays");
			for (int i = 0; i < relays.size(); i++) {
				final var relay = JsonUtils.getAsJsonObject(relays.get(i));
				final var relayIson = JsonUtils.getAsBoolean(relay, "ison");
				final var relayOverpower = JsonUtils.getAsBoolean(relay, "overpower");
				final var relayOvertemp = JsonUtils.getAsBoolean(relay, "overtemperature");

				if (i == 0) {
					relay1 = relayIson;
					overtemp1 = relayOvertemp;
					overpower1 = relayOverpower;
				} else if (i == 1) {
					relay2 = relayIson;
					overtemp2 = relayOvertemp;
					overpower2 = relayOverpower;
				}
			}
			this._setSlaveCommunicationFailed(false);
		} catch (OpenemsNamedException e) {
			this.logDebug(this.log, e.getMessage());
		}
		// Sets the Fault Channels accordingly
		this.setRelay1Overpower(overpower1);
		this.setRelay2Overpower(overpower2);
		this.setRelay1Overtemp(overtemp1);
		this.setRelay2Overtemp(overtemp2);

		this._setRelay1(relay1);
		this._setRelay2(relay2);

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
			if (e != null) {
				this.logError(this.log, "HTTP request failed: " + e.getMessage());
			}
		});
	}
}