package io.openems.edge.io.shelly.shellypro3;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;

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

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.3Pro", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})

public class IoShellyPro3Impl extends AbstractOpenemsComponent
		implements IoShellyPro3, DigitalOutput, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(IoShellyPro3Impl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;
	private String baseUrl;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShellyPro3Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShellyPro3.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShellyPro3.ChannelId.RELAY_1), //
				this.channel(IoShellyPro3.ChannelId.RELAY_2), //
				this.channel(IoShellyPro3.ChannelId.RELAY_3), //
		};
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();

		for (int i = 0; i < 3; i++) {
			final int relayIndex = i;
			String url = this.baseUrl + "/rpc/Switch.GetStatus?id=" + relayIndex;
			this.httpBridge.subscribeJsonEveryCycle(url, result -> {
				this.processHttpResult(result, relayIndex);
			}, error -> {
				logError(this.log, "HTTP request failed: " + error.getMessage());
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

	private void processHttpResult(HttpResponse<JsonElement> result, int relayIndex) {
		Boolean ison = null;

		if (result == null) {
			this._setSlaveCommunicationFailed(true);
			return;
		} else {
			try {
				var switchStatus = getAsJsonObject(result.data());
				ison = getAsBoolean(switchStatus, "output");

				this.digitalOutputChannels[relayIndex].setNextWriteValue(ison);

				this._setSlaveCommunicationFailed(false);

			} catch (Exception e) {
				this._setSlaveCommunicationFailed(true);
				this.logError(this.log, "Error processing HTTP response: " + e.getMessage());
				return;
			}
		}
		switch (relayIndex) {
		case 0:
			this._setRelay1(ison);
			break;
		case 1:
			this._setRelay2(ison);
			break;
		case 2:
			this._setRelay3(ison);
			break;
		}
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
				this._setSlaveCommunicationFailed(true);
			}
		});
	}

}
