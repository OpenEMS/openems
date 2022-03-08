package io.openems.edge.evcs.goe.chargerhome;

import java.net.UnknownHostException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Evcs.Goe.ChargerHome", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class GoeChargerHomeImpl extends AbstractOpenemsComponent
		implements ManagedEvcs, Evcs, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(GoeChargerHomeImpl.class);
	private GoeApi goeapi = null;

	protected Config config;

	@Reference
	private EvcsPower evcsPower;

	// Is charger active
	public boolean isActive;

	// Actual current
	public int activeCurrent;

	// Minimal current
	private int minCurrent;

	// Maximum current
	private int maxCurrent;

	// Last energy session
	private int lastEnergySession;

	/**
	 * Constructor.
	 */
	public GoeChargerHomeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				GoeChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.channel(GoeChannelId.ALIAS).setNextValue(config.alias());
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
					this.channel(GoeChannelId.SERIAL).setNextValue(JsonUtils.getAsString(json, "sse"));
					this.channel(GoeChannelId.FIRMWARE).setNextValue(JsonUtils.getAsString(json, "fwv"));

					// Current status
					var status = JsonUtils.getAsInt(json, "car");
					this.channel(GoeChannelId.STATUS_GOE).setNextValue(status);
					this.channel(Evcs.ChannelId.STATUS).setNextValue(this.convertGoeStatus(status));

					// Detailed charge information
					this.activeCurrent = JsonUtils.getAsInt(json, "amp") * 1000;
					this.channel(GoeChannelId.CURR_USER).setNextValue(this.activeCurrent);

					var nrg = JsonUtils.getAsJsonArray(json, "nrg");
					this.channel(GoeChannelId.VOLTAGE_L1).setNextValue(JsonUtils.getAsInt(nrg, 0));
					this.channel(GoeChannelId.VOLTAGE_L2).setNextValue(JsonUtils.getAsInt(nrg, 1));
					this.channel(GoeChannelId.VOLTAGE_L3).setNextValue(JsonUtils.getAsInt(nrg, 2));
					this.channel(GoeChannelId.CURRENT_L1).setNextValue(JsonUtils.getAsInt(nrg, 4) * 100);
					this.channel(GoeChannelId.CURRENT_L2).setNextValue(JsonUtils.getAsInt(nrg, 5) * 100);
					this.channel(GoeChannelId.CURRENT_L3).setNextValue(JsonUtils.getAsInt(nrg, 6) * 100);
					var power = JsonUtils.getAsInt(nrg, 11);
					this.channel(GoeChannelId.ACTUAL_POWER).setNextValue(power * 10);
					this.channel(Evcs.ChannelId.CHARGE_POWER).setNextValue(power * 10);

					var phases = this.convertGoePhase(JsonUtils.getAsInt(json, "pha"));
					this.channel(Evcs.ChannelId.PHASES).setNextValue(phases);

					// Hardware limits
					var cableCurrent = JsonUtils.getAsInt(json, "cbl") * 1000;
					this.maxCurrent = cableCurrent > 0 && cableCurrent < this.config.maxHwCurrent() //
							? cableCurrent //
							: this.config.maxHwCurrent();
					this._setMinimumHardwarePower(this.minCurrent / 1000 * phases * 230);
					this._setMaximumHardwarePower(this.maxCurrent / 1000 * phases * 230);

					// Energy
					this.channel(GoeChannelId.ENERGY_TOTAL).setNextValue(JsonUtils.getAsInt(json, "eto") * 100);
					this.channel(Evcs.ChannelId.ENERGY_SESSION)
							.setNextValue(JsonUtils.getAsInt(json, "dws") * 10 / 3600);

					// Error
					this.channel(GoeChannelId.ERROR).setNextValue(JsonUtils.getAsString(json, "err"));
					this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(false);

					// Set the power and energy
					this.setPower();
					this.setEnergySession();

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
	 * Sets the current from SET_CHARGE_POWER channel.
	 *
	 * <p>
	 * Possible charge currents are between MinCurrent and MaxCurrent. Values below
	 * are set to zero and values above are set to the maximum.
	 */
	private void setPower() {
		WriteChannel<Integer> energyLimitChannel = this.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		int energyLimit = energyLimitChannel.getNextValue().orElse(0);
		// Check energy limit
		if (energyLimit == 0 || energyLimit > this.getEnergySession().orElse(0)) {
			WriteChannel<Integer> channel = this.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT);
			var valueOpt = channel.getNextWriteValueAndReset();
			if (valueOpt.isPresent()) {
				var power = valueOpt.get();
				Channel<Integer> minimumHardwarePowerChannel = this.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER);

				// Charging under MINIMUM_HARDWARE_POWER isn't possible
				if (power < minimumHardwarePowerChannel.value().orElse(0)) {
					power = 0;
					this.goeapi.setActive(false);
				} else {
					this.goeapi.setActive(true);
				}
				var phases = this.getPhases();
				Integer current = power * 1000 / phases.orElse(3) /* e.g. 3 phases */ / 230; /* voltage */

				/*
				 * Limits the charging value because goe knows only values between MinCurrent
				 * and MaxCurrent
				 */
				if (current > this.maxCurrent) {
					current = this.maxCurrent;
				}
				if (current < this.minCurrent) {
					current = this.minCurrent;
				}
				var result = this.goeapi.setCurrent(current);
				if (result.isJsonObject()) {
					this._setSetChargePowerLimit(power);
					this.debugLog(result.toString());
				}
			}
		} else {
			this.goeapi.setActive(false);
			this.debugLog("Maximum energy limit reached");
			this._setStatus(Status.ENERGY_LIMIT_REACHED);
		}
	}

	/**
	 * Sets the Energy Limit for this session from SET_ENERGY_SESSION channel.
	 *
	 * <p>
	 * Allowed values for the command setenergy are 0; 1-65535 the value of the
	 * command is 0.1 Wh. The charging station will charge till this limit.
	 */
	private void setEnergySession() {
		WriteChannel<Integer> channel = this.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		var valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			var energyTarget = valueOpt.get();
			if (energyTarget < 0) {
				return;
			}

			/*
			 * limits the target value because go-e knows only values between 0 and 65535
			 * 0.1Wh
			 */
			energyTarget /= 100;
			energyTarget = energyTarget > 65535 ? 65535 : energyTarget;
			energyTarget = energyTarget > 0 && energyTarget < 1 ? 1 : energyTarget;
			if (!energyTarget.equals(this.lastEnergySession)) {
				// Set energy limit
				this.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT).setNextValue(energyTarget * 100);
				this.debugLog("Setting go-e " + this.alias() + " Energy Limit in this Session to [" + energyTarget / 10
						+ " kWh]");

				if (this.goeapi.setMaxEnergy(energyTarget)) {
					this.lastEnergySession = energyTarget;
				}
			}
		}
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
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

}
