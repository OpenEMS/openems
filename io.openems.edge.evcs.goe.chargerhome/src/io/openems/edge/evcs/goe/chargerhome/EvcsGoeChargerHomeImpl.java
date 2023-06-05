package io.openems.edge.evcs.goe.chargerhome;

import java.net.UnknownHostException;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Goe.ChargerHome", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class EvcsGoeChargerHomeImpl extends AbstractManagedEvcsComponent
		implements EvcsGoeChargerHome, ManagedEvcs, Evcs, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(EvcsGoeChargerHomeImpl.class);

	@Reference
	private EvcsPower evcsPower;

	/** Is charger active. */
	public boolean isActive;
	/** Actual current. */
	public int activeCurrent;

	protected Config config;

	private GoeApi goeapi = null;
	/** Minimal current. */
	private int minCurrent;
	/** Maximum current. */
	private int maxCurrent;

	public EvcsGoeChargerHomeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				EvcsGoeChargerHome.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.channel(EvcsGoeChargerHome.ChannelId.ALIAS).setNextValue(config.alias());
		this.config = config;
		this.minCurrent = config.minHwCurrent();
		this.maxCurrent = config.maxHwCurrent();
		this._setChargingType(ChargingType.AC);
		this._setPowerPrecision(230);

		// start api-Worker
		this.goeapi = new GoeApi(this);
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
		super.handleEvent(event);
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:

			// handle writes
			var json = this.goeapi.getStatus();
			if (json == null) {
				this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(true);

			} else {
				try {
					// Is Active
					var alw = JsonUtils.getAsInt(json, "alw");
					if (alw == 1) {
						this.isActive = true;
					} else {
						this.isActive = false;
					}

					// General information
					this.channel(EvcsGoeChargerHome.ChannelId.SERIAL).setNextValue(JsonUtils.getAsString(json, "sse"));
					this.channel(EvcsGoeChargerHome.ChannelId.FIRMWARE)
							.setNextValue(JsonUtils.getAsString(json, "fwv"));

					// Current status
					var status = JsonUtils.getAsInt(json, "car");
					this.channel(EvcsGoeChargerHome.ChannelId.STATUS_GOE).setNextValue(status);
					this.channel(Evcs.ChannelId.STATUS).setNextValue(this.convertGoeStatus(status));

					// Detailed charge information
					this.activeCurrent = JsonUtils.getAsInt(json, "amp") * 1000;
					this.channel(EvcsGoeChargerHome.ChannelId.CURR_USER).setNextValue(this.activeCurrent);

					var nrg = JsonUtils.getAsJsonArray(json, "nrg");
					this.channel(EvcsGoeChargerHome.ChannelId.VOLTAGE_L1).setNextValue(JsonUtils.getAsInt(nrg, 0));
					this.channel(EvcsGoeChargerHome.ChannelId.VOLTAGE_L2).setNextValue(JsonUtils.getAsInt(nrg, 1));
					this.channel(EvcsGoeChargerHome.ChannelId.VOLTAGE_L3).setNextValue(JsonUtils.getAsInt(nrg, 2));
					this.channel(EvcsGoeChargerHome.ChannelId.CURRENT_L1)
							.setNextValue(JsonUtils.getAsInt(nrg, 4) * 100);
					this.channel(EvcsGoeChargerHome.ChannelId.CURRENT_L2)
							.setNextValue(JsonUtils.getAsInt(nrg, 5) * 100);
					this.channel(EvcsGoeChargerHome.ChannelId.CURRENT_L3)
							.setNextValue(JsonUtils.getAsInt(nrg, 6) * 100);
					var power = JsonUtils.getAsInt(nrg, 11);
					this.channel(EvcsGoeChargerHome.ChannelId.ACTUAL_POWER).setNextValue(power * 10);
					this.channel(Evcs.ChannelId.CHARGE_POWER).setNextValue(power * 10);

					// Hardware limits
					var cableCurrent = JsonUtils.getAsInt(json, "cbl") * 1000;
					this.maxCurrent = cableCurrent > 0 && cableCurrent < this.config.maxHwCurrent() //
							? cableCurrent //
							: this.config.maxHwCurrent();

					this._setFixedMinimumHardwarePower(
							Math.round(this.minCurrent / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue());
					this._setFixedMaximumHardwarePower(
							Math.round(this.maxCurrent / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue());

					// Phases
					int phases = this.convertGoePhase(JsonUtils.getAsInt(json, "pha"));
					this._setPhases(phases);

					// Energy
					this.channel(EvcsGoeChargerHome.ChannelId.ENERGY_TOTAL)
							.setNextValue(JsonUtils.getAsInt(json, "eto") * 100);
					this.channel(Evcs.ChannelId.ENERGY_SESSION)
							.setNextValue(JsonUtils.getAsInt(json, "dws") * 10 / 3600);

					// Error
					this.channel(EvcsGoeChargerHome.ChannelId.ERROR).setNextValue(JsonUtils.getAsString(json, "err"));
					this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(false);

				} catch (OpenemsNamedException e) {
					this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(true);
				}
			}
			break;
		default:
			break;
		}
	}

	private Status convertGoeStatus(int status) {
		switch (status) {
		case 1: // ready for charging, car unplugged
			return Status.NOT_READY_FOR_CHARGING;
		case 2: // charging
			return Status.CHARGING;
		case 3: // waiting for car
			return Status.READY_FOR_CHARGING;
		case 4: // charging finished, car plugged
			return Status.CHARGING_FINISHED;
		default:
			return Status.UNDEFINED;
		}
	}

	/**
	 * Converts the binary input into the amount of phases that are used to charge.
	 *
	 * @param phase binary phase input
	 * @return amount of phases
	 */
	private int convertGoePhase(int phase) {
		var phasen = (byte) phase & 0b00111000;
		switch (phasen) {
		case 8: // 0b00001000: Phase 1 is active
			return 1;
		case 24: // 0b00011000: Phase 1+2 is active
			return 2;
		case 56: // 0b00111000: Phase1-3 are active
			return 3;
		default:
			return 0;
		}
	}

	/**
	 * Debug Log.
	 * 
	 * <p>
	 * Logging only if the debug mode is enabled
	 * 
	 * @param message text that should be logged
	 */
	public void debugLog(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {
		this.goeapi.setActive(true);
		return this.sendChargePowerLimit(power);
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsException {
		this.goeapi.setActive(false);
		return this.sendChargePowerLimit(0);
	}

	private boolean sendChargePowerLimit(int power) {
		var phases = this.getPhasesAsInt();
		var current = power * 1000 / phases /* e.g. 3 phases */ / 230; /* voltage */

		var result = this.goeapi.setCurrent(current);
		if (result.isJsonObject()) {
			this._setSetChargePowerLimit(power);
			this.debugLog(result.toString());
			return true;
		}
		return false;
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}
}
