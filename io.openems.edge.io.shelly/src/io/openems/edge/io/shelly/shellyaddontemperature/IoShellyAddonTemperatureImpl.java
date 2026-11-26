package io.openems.edge.io.shelly.shellyaddontemperature;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static java.lang.Math.round;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.thermometer.api.Thermometer;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.AddonTemperature", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class IoShellyAddonTemperatureImpl extends AbstractOpenemsComponent
		implements IoShellyAddonTemperature, Thermometer, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(IoShellyAddonTemperatureImpl.class);

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;
	private BridgeHttp httpBridge;

	private String sensorKey;

	public IoShellyAddonTemperatureImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Thermometer.ChannelId.values(), //
				IoShellyAddonTemperature.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!this.isEnabled()) {
			return;
		}

		this.sensorKey = "temperature:" + config.sensor_id();
		this.httpBridge = this.httpBridgeFactory.get();
		final var cycleService = this.httpBridge.createService(this.httpBridgeCycleServiceDefinition);
		cycleService.subscribeJsonEveryCycle("http://" + config.ip() + "/rpc/Shelly.GetStatus",
				this::processHttpResult);
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

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		Integer temperature = null;
		Boolean restartRequired = null;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());

		} else {
			try {
				final var jsonResponse = getAsJsonObject(result.data());
				final var sensor = getAsJsonObject(jsonResponse, this.sensorKey);
				temperature = round(getAsFloat(sensor, "tC") * 10 /* convert to decidegree */);

				final var sys = getAsJsonObject(jsonResponse, "sys");
				restartRequired = getAsBoolean(sys, "restart_required");

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
				temperature = null;
				restartRequired = null;
			}
		}

		this._setTemperature(temperature);
		this._setSlaveCommunicationFailed(temperature == null);
		setValue(this, IoShellyAddonTemperature.ChannelId.NEEDS_RESTART, restartRequired);
	}

	@Override
	public String debugLog() {
		return this.getTemperature().asString();
	}

}
