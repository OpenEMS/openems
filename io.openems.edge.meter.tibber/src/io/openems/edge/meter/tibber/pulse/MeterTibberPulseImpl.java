package io.openems.edge.meter.tibber.pulse;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.tibber.pulse.decoder.SmlDecoder;
import io.openems.edge.meter.tibber.pulse.decoder.SmlMeterData;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Tibber.Pulse", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE//
})
public class MeterTibberPulseImpl extends AbstractOpenemsComponent
		implements MeterTibberPulse, ElectricityMeter, OpenemsComponent, EventHandler, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(MeterTibberPulseImpl.class);

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL //
	)
	private volatile Timedata timedata;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private MeterType meterType = null;
	private String baseUrl;
	private String credentials;
	private String encodedAuth;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public MeterTibberPulseImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterTibberPulse.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		this.httpBridge = this.httpBridgeFactory.get();

		this.baseUrl = "http://" + config.ip();
		this.credentials = "admin:" + config.password();
		this.encodedAuth = Base64.getEncoder().encodeToString(this.credentials.getBytes());

		if (!this.isEnabled()) {
			return;
		}

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder();
		b.append(this.getActivePowerChannel().value().asString());
		return b.toString();
	}

	@Override
	public void handleEvent(Event event) {
		this.generateGetRequest();
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		}

	}

	private void generateGetRequest() {
		Map<String, String> properties = Map.of("Authorization", "Basic " + this.encodedAuth, "Content-Type",
				"application/x-www-form-urlencoded");
		BridgeHttp.Endpoint endpoint = new BridgeHttp.Endpoint(this.baseUrl + "/data.json?node_id=1", HttpMethod.GET,
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, null, properties);

		this.httpBridge.requestRaw(endpoint).thenAccept(payload -> {
			if (payload != null) {
				try {
					// Decode SML data from the payload
					SmlMeterData smlData = SmlDecoder.decode(payload, false);
					// Process the decoded SML data
					this.processSmlData(smlData);

				} catch (OpenemsNamedException | IOException ex) {
					this.logDebug(this.log, ex.getMessage());
				}
			}
		}).exceptionally(ex -> {
			this._setSlaveCommunicationFailed(true);
			this.log.error("HTTP request failed: " + ex.getMessage());
			return null;
		});
	}

	private void processSmlData(SmlMeterData smlData) throws OpenemsNamedException {
		if (smlData != null) {
			this._setSlaveCommunicationFailed(false);
			for (SmlMeterData.Reading reading : smlData.getReadings()) {

				Number value = reading.getValue();
				switch (reading.getObisCode()) {
				case "1-0:1.8.0*255": // energyImportTotal
					break;
				case "1-0:2.8.0*255": // energyExportTotal
					break;
				case "1-0:16.7.0*255": // powerTotal
					_setActivePower(value.intValue());
					break;
				case "1-0:36.7.0*255": // powerL1
					_setActivePowerL1(value.intValue());
					break;
				case "1-0:56.7.0*255": // powerL2
					_setActivePowerL2(value.intValue());
					break;
				case "1-0:76.7.0*255": // powerL3
					_setActivePowerL3(value.intValue());
					break;
				case "1-0:32.7.0*255": // voltageL1
					_setVoltageL1((int) value.doubleValue() * 1000);
					break;
				case "1-0:52.7.0*255": // voltageL2
					_setVoltageL2((int) value.doubleValue() * 1000);
					break;
				case "1-0:72.7.0*255": // voltageL3
					_setVoltageL3((int) value.doubleValue() * 1000);
					break;
				case "1-0:31.7.0*255": // currentL1
					_setCurrentL1((int) value.doubleValue() * 1000);
					break;
				case "1-0:51.7.0*255": // currentL2
					_setCurrentL2((int) value.doubleValue() * 1000);
					break;
				case "1-0:71.7.0*255": // currentL3
					_setCurrentL3((int) value.doubleValue() * 1000);
					break;
				case "1-0:14.7.0*255": // Frequency
					_setFrequency(value.intValue());
					break;
				}
			}
		} else {
			this._setSlaveCommunicationFailed(true);
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(-activePower);
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}