package io.openems.edge.ess.samsung.gridmeter;

import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static java.lang.Math.round;

import org.osgi.service.cm.ConfigurationAdmin;
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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Samsung.ESS.Grid-Meter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)

@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class SamsungEssGridMeterImpl extends AbstractOpenemsComponent
		implements SamsungEssGridMeter, ElectricityMeter, OpenemsComponent, EventHandler, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(SamsungEssGridMeterImpl.class);
	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	private String currentGridStatus = "Unknown";
	private String baseUrl;
	private Config config;

	public SamsungEssGridMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SamsungEssGridMeter.ChannelId.values() //
		);
		ElectricityMeter.calculatePhasesFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();
		this.config = config;

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/R3EMSAPP_REAL.ems?file=ESSRealtimeStatus.json",
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

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateEnergy(); // Call the calculateEnergy method here
			break;
		}
	}

	private void fetchAndUpdateEssRealtimeStatus(HttpResponse<JsonElement> result, HttpError error) {
		Integer gridPw = null;
		String currentGridStatus = "Unknown";

		if (error != null) {
			this.logDebug(this.log, error.getMessage());
		} else {
			try {

				var response = getAsJsonObject(result.data());
				var essRealtimeStatus = getAsJsonObject(response, "ESSRealtimeStatus");

				gridPw = round(getAsFloat(essRealtimeStatus, "GridPw") * 1000);
				var gridStatusCode = getAsInt(essRealtimeStatus, "GridStusCd");

				switch (gridStatusCode) {
				case 0:
					// Buy from Grid is positive
					currentGridStatus = "Buy from Grid";
					break;

				case 1:
					// Sell to Grid is negative
					gridPw = -gridPw;
					currentGridStatus = "Sell to Grid";

					break;
				default:
					// Handle unknown status codes if needed
					currentGridStatus = "Unknown";
					gridPw = 0;

				}
			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
			}
		}

		this._setActivePower(gridPw);

		this.currentGridStatus = currentGridStatus;
	}

	private void calculateEnergy() {
		Integer activePower = this.getActivePower().orElse(null);
		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Grid
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			// Sell-To-Grid
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(-activePower);
		}
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString() //
				+ " |Status: " + this.currentGridStatus;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

}