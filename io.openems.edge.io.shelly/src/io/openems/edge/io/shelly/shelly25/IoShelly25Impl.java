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

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import com.google.gson.JsonElement;
import io.openems.common.utils.JsonUtils;

@Designate(ocd = Config.class, factory = true)
@Component(name = "IO.Shelly.25", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE })
public class IoShelly25Impl extends AbstractOpenemsComponent
		implements IoShelly25, DigitalOutput, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(IoShelly25Impl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;
	private String baseUrl;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShelly25Impl() {
		super(OpenemsComponent.ChannelId.values(), DigitalOutput.ChannelId.values(), IoShelly25.ChannelId.values());
		this.digitalOutputChannels = new BooleanWriteChannel[] { this.channel(IoShelly25.ChannelId.RELAY_1),
				this.channel(IoShelly25.ChannelId.RELAY_2), };
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();
	}

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
		var b = new StringBuilder();
		var i = 1;
		for (BooleanWriteChannel channel : this.digitalOutputChannels) {
			String valueText;
			var valueOpt = channel.value().asOptional();
			if (valueOpt.isPresent()) {
				valueText = valueOpt.get() ? "On" : "Off";
			} else {
				valueText = "Unknown";
			}
			b.append("Relay ").append(i).append(": ").append(valueText);
			if (i < this.digitalOutputChannels.length) {
				b.append(" | ");
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
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.eventBeforeProcessImage();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.eventExecuteWrite();
			break;
		}
	}

	/**
	 * Execute on Cycle Event "Before Process Image".
	 */
	private void eventBeforeProcessImage() {
		String url = this.baseUrl + "/status";
		this.httpBridge.get(url).whenComplete((result, error) -> {
			if (error != null) {
				this.logError(this.log, "HTTP request failed: " + error.getMessage());
				this._setSlaveCommunicationFailed(true);
			} else {
				try {
					// Convert the result string to JsonElement using JsonUtils
					JsonElement jsonElement = JsonUtils.getAsJsonElement(result);
					final var relays = JsonUtils.getAsJsonArray(jsonElement, "relays");
					for (int i = 0; i < relays.size(); i++) {
						final var relay = JsonUtils.getAsJsonObject(relays.get(i));
						final var relayIson = JsonUtils.getAsBoolean(relay, "ison");
						// Update the relay state based on the JSON response
						this.digitalOutputChannels[i].setNextWriteValue(relayIson);
					}
					this._setSlaveCommunicationFailed(false);
				} catch (Exception e) { // Catching generic exception for any parsing or processing errors
					this.logError(this.log, "Error processing JSON: " + e.getMessage());
					this._setSlaveCommunicationFailed(true);
				}
			}
		});
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 */
	private void eventExecuteWrite() {
		for (int i = 0; i < digitalOutputChannels.length; i++) {
			executeWrite(digitalOutputChannels[i], i);
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
				this._setSlaveCommunicationFailed(true);
			}
		});
	}
}
