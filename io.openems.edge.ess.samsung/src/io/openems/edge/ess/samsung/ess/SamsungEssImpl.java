package io.openems.edge.ess.samsung.ess;

import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static java.lang.Math.round;

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
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Samsung.ESS", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class SamsungEssImpl extends AbstractOpenemsComponent
		implements SamsungEss, SymmetricEss, OpenemsComponent, EventHandler, TimedataProvider, HybridEss {

	private final Logger log = LoggerFactory.getLogger(SamsungEssImpl.class);
	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_DISCHARGE_ENERGY);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	private String baseUrl;
	private Integer latestGridPw = 0;
	private Integer latestPvPw = 0;
	private Integer latestPcsPw = 0;
	private Integer latestConsPw = 0;
	private Integer latestBatteryStatus = -1;
	private Integer latestGridStatus = -1;

	public SamsungEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				SamsungEss.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				HybridEss.ChannelId.values() //
		//
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();
		this._setCapacity(config.capacity());
		this._setGridMode(GridMode.ON_GRID); // Has no Backup function

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
			this.calculateEnergy();
			break;
		}
	}

	private void fetchAndUpdateEssRealtimeStatus(HttpResponse<JsonElement> result, HttpError error) {
		Integer pvPw = null;
		Integer pcsPw = null;
		Integer gridPw = null;
		Integer consPw = null;
		Integer batteryStatus = null;
		Integer gridStatus = null;
		Integer soc = null;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());

		} else {
			try {
				var response = getAsJsonObject(result.data());
				var essRealtimeStatus = getAsJsonObject(response, "ESSRealtimeStatus");
				pvPw = round(getAsFloat(essRealtimeStatus, "PvPw") * 1000);
				pcsPw = round(getAsFloat(essRealtimeStatus, "PcsPw"));
				gridPw = round(getAsFloat(essRealtimeStatus, "GridPw"));
				consPw = round(getAsFloat(essRealtimeStatus, "ConsPw"));
				batteryStatus = getAsInt(essRealtimeStatus, "BtStusCd");
				gridStatus = getAsInt(essRealtimeStatus, "GridStusCd");
				soc = round(getAsInt(essRealtimeStatus, "BtSoc"));

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
			}
		}

		var dcDischargePower = switch (batteryStatus) {
		// Battery is in Discharge mode
		case 0 -> null;
		// Battery is in Charge mode
		case 1 -> pcsPw;
		// Battery is in Idle mode
		case 2 -> 0;
		// Handle unknown status codes
		default -> {
			this.logWarn(this.log, "Unknown Battery Status Code: " + batteryStatus);
			yield null;
		}
		};

		this._setSlaveCommunicationFailed(error != null);
		this._setActivePower(pcsPw - pvPw);
		this._setDcDischargePower(dcDischargePower);
		this._setSoc(soc);

		this.latestPvPw = pvPw;
		this.latestPcsPw = pcsPw;
		this.latestGridPw = gridPw;
		this.latestConsPw = consPw;
		this.latestBatteryStatus = batteryStatus;
		this.latestGridStatus = gridStatus;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Integer getSurplusPower() {
		// Adjust the sign of gridPw and pcsPw based on the status codes
		if (this.latestGridStatus == 1) {
			this.latestGridPw = -this.latestGridPw;
		}
		if (this.latestBatteryStatus == 0) {
			this.latestPcsPw = -this.latestPcsPw;
		}

		// Calculate surplus power
		double surplusPower = (this.latestGridPw + this.latestPvPw) - (this.latestPcsPw + this.latestConsPw);
		// Return the surplus power or 'null' if there is no surplus power
		return surplusPower > 0 ? (int) surplusPower : null;
	}

	private void calculateEnergy() {
		// Calculate AC Energy
		var activePower = this.getActivePowerChannel().getNextValue().get();
		if (activePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else {
			if (activePower > 0) {
				// Discharge
				this.calculateAcChargeEnergy.update(0);
				this.calculateAcDischargeEnergy.update(activePower);
				this.calculateDcChargeEnergy.update(0);
				this.calculateDcDischargeEnergy.update(activePower);
			} else {
				// Charge
				this.calculateAcChargeEnergy.update(activePower * -1);
				this.calculateAcDischargeEnergy.update(0);
				this.calculateDcChargeEnergy.update(activePower * -1);
				this.calculateDcDischargeEnergy.update(0);
			}
		}
	}

}
