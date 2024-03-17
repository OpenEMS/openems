package io.openems.edge.ess.samsung.thermometer;

import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static java.lang.Math.round;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
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
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.thermometer.api.Thermometer;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Thermometer.Samsung", immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)

@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class SamsungEssThemometerImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, EventHandler, Thermometer {

	private String baseUrl;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	private final Logger log = LoggerFactory.getLogger(SamsungEssThemometerImpl.class);

	public SamsungEssThemometerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				Thermometer.ChannelId.values()//
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/R3EMSAPP_REAL.ems?file=Weather.json",
				this::fetchAndUpdateEssRealtimeStatus);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

	}

	private void fetchAndUpdateEssRealtimeStatus(JsonElement json, Throwable error) {

		Float temp = null;
		Integer humidity = null;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());
		} else {
			try {

				var response = getAsJsonObject(json);
				var essRealtimeStatus = getAsJsonObject(response, "WeatherInfo");

				temp = getAsFloat(essRealtimeStatus, "Temperature");
				humidity = round(getAsFloat(essRealtimeStatus, "Humidity"));

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());

			}

			this._setTemperature((int) (temp * 10));
			this.channel(Thermometer.ChannelId.HUMIDITY).setNextValue(humidity);

		}
	}

	@Override
	public String debugLog() {
		float tempCelsius = 0.0f;
		int humidityValue = 0;

		Value<Integer> tempValueWrapper = this.getTemperature();
		if (tempValueWrapper != null && tempValueWrapper.get() != null) {
			Integer tempValue = tempValueWrapper.get();
			tempCelsius = tempValue / 10.0f; // Convert deciCelsius to Celsius
		}

		Object humidityObj = this.channel(Thermometer.ChannelId.HUMIDITY).value().get();
		if (humidityObj instanceof Integer) {
			humidityValue = (Integer) humidityObj;
		}
		return String.format("%.1f°C | %d%%", tempCelsius, humidityValue);
	}

}