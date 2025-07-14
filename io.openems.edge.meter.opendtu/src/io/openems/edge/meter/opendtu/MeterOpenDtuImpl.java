package io.openems.edge.meter.opendtu;

import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

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

import com.google.gson.JsonElement;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

import static io.openems.common.utils.JsonUtils.getAsFloat;
import static java.lang.Math.round;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.OpenDTU", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class MeterOpenDtuImpl extends AbstractOpenemsComponent
		implements MeterOpenDtu, ElectricityMeter, SinglePhaseMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);


	private final Logger log = LoggerFactory.getLogger(MeterOpenDtuImpl.class);

	private String baseUrl;
	private Config config;

	@Reference(cardinality = MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	public MeterOpenDtuImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterOpenDtu.ChannelId.values() //
		);
		
		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);

	}

	@Activate
	protected void activate(ComponentContext context, Config config)  throws InvalidValueException, KeyManagementException, NoSuchAlgorithmException, OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		
		this.baseUrl = "http://" + config.ipAddress();
		this.httpBridge = this.httpBridgeFactory.get();

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/api/livedata/status?inv=" + config.serialNumber(), this::processHttpResult);

		/*
		 * this.worker = new ReadWorker(this, InetAddressUtils.parseOrError(config.ipAddress()), config.serialNumber());
		 */
		/*
		 * this.worker.activate(config.id());
		 */
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
	public String debugLog() {
		return this.getPhase() + ":" + this.getActivePower().asString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE: //
			this.calculateEnergy();
			break;
		}
	}

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		setValue(this, MeterOpenDtu.ChannelId.SLAVE_COMMUNICATION_FAILED, result == null);

		Integer power = null;
		Integer voltage = null;
		Integer current = null;
		//boolean restartRequired = false;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());

		} else {
			try {
				var jsonResponse = getAsJsonObject(result.data());
				var inverters = JsonUtils.getAsJsonArray(jsonResponse, "inverters");
				var inverter = inverters.get(0).getAsJsonObject();
				var ac = inverter.getAsJsonObject("AC");
				var ac0 = ac.getAsJsonObject("0");
				
				power = round(getAsFloat(ac0.getAsJsonObject("Power"), "v"));
				voltage = round(getAsFloat(ac0.getAsJsonObject("Voltage"), "v")  * 1000);
				current = round(getAsFloat(ac0.getAsJsonObject("Current"), "v")  * 1000);

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
			}
		}

		this._setActivePower(power);
		this._setCurrent(current);
		this._setVoltage(voltage);

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
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public SinglePhase getPhase() {
		return this.config.phase();
	}
}
