package io.openems.edge.ess.samsung.ess;

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

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.io.ess.samsung.common.SamsungApi;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.api.HybridEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Samsung", immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)

@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class SamsungEssImpl extends AbstractOpenemsComponent
		implements SamsungEss, SymmetricEss, OpenemsComponent, EventHandler, TimedataProvider, HybridEss {

	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	private final Logger log = LoggerFactory.getLogger(SamsungEssImpl.class);
	private SamsungApi samsungApi = null;

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
		this.samsungApi = new SamsungApi(config.ip());
		this._setCapacity(config.capacity());
		this._setGridMode(GridMode.ON_GRID);

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.fetchAndUpdateEssRealtimeStatus();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}

	private void fetchAndUpdateEssRealtimeStatus() {
		try {
			// Fetch the necessary data from the API
			JsonObject necessaryData = samsungApi.getEssRealtimeStatus();

			// Populate the appropriate channels with the fetched data
			double PvPw = necessaryData.get("PvPw").getAsDouble() * 1000;
			double PcsPw = necessaryData.get("PcsPw").getAsDouble();
			int btSoc = necessaryData.get("BtSoc").getAsInt();
			int BtStusCd = necessaryData.get("BtStusCd").getAsInt();

			// Update the channels
			this.channel(SymmetricEss.ChannelId.SOC).setNextValue(btSoc);

			switch (BtStusCd) {
			case 0:
				// Battery is in Discharge mode
				if (PcsPw > 0) {
					this._setDcDischargePower((int) PcsPw);
				} else {
					this._setDcDischargePower((int) -PcsPw);
				}
				break;
			case 1:
				// Battery is in Charge mode
				if (PcsPw > 0) {
					this._setDcDischargePower((int) -PcsPw);
				} else {
					this._setDcDischargePower((int) -PcsPw);
				}
				break;
			case 2:
				// Battery is in Idle mode
				this._setDcDischargePower(0);
				break;
			default:
				// Handle unknown status codes
				this.logWarn(log, "Unknown Battery Status Code: " + BtStusCd);
				break;
			}
			// this._setDcDischargePower(dcDischargePower);
			this._setActivePower((int) (PcsPw - PvPw));
			this._setSlaveCommunicationFailed(false);

		} catch (OpenemsNamedException e) {
			this._setSlaveCommunicationFailed(true);
			this._setActivePower(0);
			this.log.warn("Failed to fetch ESS Real-time Status", e);
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString(); //
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Integer getSurplusPower() {
		try {
			// Fetch the necessary data from the API
			JsonObject necessaryData = samsungApi.getEssRealtimeStatus();

			// Extract relevant power values and status codes from the JSON object
			double gridPw = necessaryData.get("GridPw").getAsDouble();
			double pvPw = necessaryData.get("PvPw").getAsDouble();
			double pcsPw = necessaryData.get("PcsPw").getAsDouble();
			double consPw = necessaryData.get("ConsPw").getAsDouble();
			int gridStatus = necessaryData.get("GridStusCd").getAsInt();
			int batteryStatus = necessaryData.get("BtStusCd").getAsInt();

			// Adjust the sign of gridPw and pcsPw based on the status codes
			if (gridStatus == 1) {
				gridPw = -gridPw;
			}
			if (batteryStatus == 0) {
				pcsPw = -pcsPw;
			}

			// Calculate surplus power
			double surplusPower = (gridPw + pvPw) - (pcsPw + consPw);
			// Return the surplus power or 'null' if there is no surplus power
			return surplusPower > 0 ? (int) surplusPower : null;
		} catch (OpenemsNamedException e) {
			log.warn("Failed to fetch ESS Real-time Status for Surplus Power Calculation", e);
			return null;
		}
	}

	private void calculateEnergy() {
		/*
		 * Calculate AC Energy
		 */
		var acActivePower = this.getActivePowerChannel().getNextValue().get();
		if (acActivePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
		} else if (acActivePower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			this.calculateAcDischargeEnergy.update(acActivePower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(acActivePower * -1);
			this.calculateAcDischargeEnergy.update(0);
		}
	}

}