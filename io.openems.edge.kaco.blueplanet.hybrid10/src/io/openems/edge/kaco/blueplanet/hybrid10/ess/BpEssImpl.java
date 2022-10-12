package io.openems.edge.kaco.blueplanet.hybrid10.ess;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.kaco.blueplanet.hybrid10.ErrorChannelId;
import io.openems.edge.kaco.blueplanet.hybrid10.core.BpCore;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.BatteryData;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.InverterData;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.Settings;
import io.openems.edge.kaco.blueplanet.hybrid10.edcom.Status;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Kaco.BlueplanetHybrid10.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class BpEssImpl extends AbstractOpenemsComponent implements BpEss, HybridEss, ManagedSymmetricEss, SymmetricEss,
		OpenemsComponent, TimedataProvider, EventHandler, ModbusSlave {

	private static final int WATCHDOG_SECONDS = 8;
	private static final int MAX_POWER_RAMP = 500; // [W/sec]

	private final Logger log = LoggerFactory.getLogger(BpEssImpl.class);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BpCore core;

	@Reference
	protected Cycle cycle;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Timedata timedata;

	@Reference
	private Power power;

	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_DISCHARGE_ENERGY);

	private Config config;

	public BpEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				ErrorChannelId.values(), //
				BpEss.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "core", config.core_id())) {
			return;
		}

		this.config = config;
		this._setCapacity(config.capacity());

		// Set Max-Apparent-Power
		this.timedata.getLatestValue(new ChannelAddress(config.id(), SymmetricEss.ChannelId.MAX_APPARENT_POWER.id()))
				.thenAccept(latestValue -> {
					Integer lastMaxApparentPower = TypeUtils.getAsType(OpenemsType.INTEGER, latestValue);
					if (lastMaxApparentPower != null
							&& lastMaxApparentPower != 10_000 /* throw away value that was previously fixed */) {
						this._setMaxApparentPower(lastMaxApparentPower);
					} else {
						this._setMaxApparentPower(MAX_POWER_RAMP); // start low
					}
				});
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		}
	}

	private void updateChannels() {
		Integer soc = null;
		Integer activePower = null;
		Integer dcDischargePower = null;
		Integer reactivePower = null;
		GridMode gridMode = GridMode.UNDEFINED;
		Float bmsVoltage = null;
		Float riso = null;
		Integer inverterStatus = null;
		Integer batteryStatus = null;
		PowerManagementConfiguration powerManagementConfiguration = PowerManagementConfiguration.UNDEFINED;

		if (!this.core.isConnected()) {
			this.logWarn(this.log, "Core is not connected!");

		} else {
			BatteryData battery = this.core.getBatteryData();
			Status status = this.core.getStatusData();
			InverterData inverter = this.core.getInverterData();

			if (battery != null) {
				soc = Math.round(battery.getSOE());
				float batteryPower = battery.getPower();
				activePower = Math.round(batteryPower - inverter.getPvPower()) * -1; // invert
				dcDischargePower = Math.round(batteryPower) * -1; // invert
				bmsVoltage = battery.getBmsVoltage();

				// Handle MaxApparentPower
				if (Math.abs(activePower) + MAX_POWER_RAMP > this.getMaxApparentPower().orElse(Integer.MAX_VALUE)) {
					this._setMaxApparentPower(Math.abs(activePower) + MAX_POWER_RAMP);
				}
			}

			if (status != null) {
				switch (status.getInverterStatus()) {
				case 12:
					gridMode = GridMode.OFF_GRID;
					break;
				case 13:
				case 14:
					gridMode = GridMode.ON_GRID;
					break;
				}

				inverterStatus = status.getInverterStatus();
				batteryStatus = status.getBatteryStatus();

				// Read Power Management Configuration, i.e. External EMS, Charging,...
				int powerConfig = status.getPowerConfig();
				for (PowerManagementConfiguration thisEnum : PowerManagementConfiguration.values()) {
					if (thisEnum.getValue() == powerConfig) {
						powerManagementConfiguration = thisEnum;
						break;
					}
				}

				// Set error channels
				List<String> errors = status.getErrors();
				for (ErrorChannelId channelId : ErrorChannelId.values()) {
					this.channel(channelId).setNextValue(errors.contains(channelId.getErrorCode()));
				}
			}

			if (inverter != null) {
				reactivePower = Math.round(
						inverter.getReactivPower(0) + inverter.getReactivPower(1) + inverter.getReactivPower(2)) * -1;
				riso = inverter.getRIso();
			}
		}

		this._setSoc(soc);
		this._setActivePower(activePower);
		this._setDcDischargePower(dcDischargePower);
		this._setReactivePower(reactivePower);
		this._setGridMode(gridMode);

		if (soc == null || soc >= 99) {
			this._setAllowedChargePower(0);
		} else {
			this._setAllowedChargePower(this.config.capacity() * -1);
		}
		if (soc == null || soc <= 0) {
			this._setAllowedDischargePower(0);
		} else {
			this._setAllowedDischargePower(this.config.capacity());
		}

		this.channel(BpEss.ChannelId.BMS_VOLTAGE).setNextValue(bmsVoltage);
		this.channel(BpEss.ChannelId.RISO).setNextValue(riso);
		this.channel(BpEss.ChannelId.INVERTER_STATUS).setNextValue(inverterStatus);
		this.channel(BpEss.ChannelId.BATTERY_STATUS).setNextValue(batteryStatus);
		this.channel(BpEss.ChannelId.POWER_MANAGEMENT_CONFIGURATION).setNextValue(powerManagementConfiguration);

		// Evaluate ExternalControlFault State-Channel
		switch (powerManagementConfiguration) {
		case UNDEFINED:
		case EXTERNAL_EMS:
			this._setExternalControlFault(false);
			break;
		case BATTERY_CHARGING:
		case MAX_YIELD:
		case SELF_CONSUMPTION:
			if (this.config.readOnly()) {
				this._setExternalControlFault(false);
			} else {
				this._setExternalControlFault(true);
			}
			break;
		}

		// Calculate AC Energy
		if (activePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
		} else if (activePower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			this.calculateAcDischargeEnergy.update(activePower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(activePower * -1);
			this.calculateAcDischargeEnergy.update(0);
		}

		// Calculate DC Energy
		if (dcDischargePower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else if (dcDischargePower > 0) {
			// Discharge
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(dcDischargePower);
		} else {
			// Charge
			this.calculateDcChargeEnergy.update(dcDischargePower * -1);
			this.calculateDcDischargeEnergy.update(0);
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";"
				+ this.getAllowedDischargePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString() //
				+ (this.config.readOnly() ? "|Read-Only-Mode" : "");
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	private Instant lastApplyPower = Instant.MIN;
	private int lastSetActivePower = Integer.MIN_VALUE;

	@Override
	public void applyPower(int activePower, int reactivePower) {
		Status status = this.core.getStatusData();
		Settings settings = this.core.getSettings();
		if (status == null || settings == null) {
			return;
		}

		// Detect if hy-switch Grid-Meter is available for Read-Only mode
		if (this.config.readOnly() && status.getPowerGridConfig() == 0 /* VECTIS disabled */) {
			this._setNoGridMeterDetected(true);
		} else {
			this._setNoGridMeterDetected(false);
		}

		// The "setPacSetPoint" method of the edcom version 8, has no effect
		// if the default user password is configured
		switch (this.core.getStableVersion()) {
		case VERSION_8:
			this._setUserPasswordNotChangedWithExternalKacoVersion8(true);
			if (this.core.isDefaultUser()) {
				return;
			}
			if (!this.core.getUserAccessDenied().orElse(true)) {
				// If the system is at least running, it should not remain in fault state
				this._setUserPasswordNotChangedWithExternalKacoVersion8(false);
			}
			break;
		case UNDEFINED:
		case VERSION_7_OR_OLDER:
			this._setUserPasswordNotChangedWithExternalKacoVersion8(false);
			break;
		}

		Instant now = Instant.now();
		if (this.lastSetActivePower == activePower
				&& Duration.between(this.lastApplyPower, now).getSeconds() < WATCHDOG_SECONDS) {
			// no need to apply to new set-point
			return;
		}

		if (this.config.readOnly()) {
			activePower = 0;
			// read-only: activates 'compensator normal operation'
			settings.setPacSetPoint(0);

		} else if (activePower == 0) {
			// avoid setting active power to zero, because this activates 'compensator
			// normal operation'
			settings.setPacSetPoint(0.0001f);

		} else {
			// apply power
			settings.setPacSetPoint(activePower);
		}

		this.lastApplyPower = Instant.now();
		this.lastSetActivePower = activePower;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		// Read-Only-Mode
		if (this.config.readOnly()) {
			return new Constraint[] {
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		}
		// Apply Power-Ramp for version > 5
		if (this.core.getVersionCom().orElse(0F) > 5F) {
			float maxDelta = MAX_POWER_RAMP * this.cycle.getCycleTime() / 1000;
			int activePower = this.getActivePower().orElse(0);
			return new Constraint[] {
					this.createPowerConstraint(MAX_POWER_RAMP + "W/sec Ramp", Phase.ALL, Pwr.ACTIVE,
							Relationship.LESS_OR_EQUALS, activePower + maxDelta),
					this.createPowerConstraint(MAX_POWER_RAMP + "W/sec Ramp", Phase.ALL, Pwr.ACTIVE,
							Relationship.GREATER_OR_EQUALS, activePower - maxDelta) };
		}
		return Power.NO_CONSTRAINTS;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Integer getSurplusPower() {
		// Is battery and inverter data available?
		BatteryData battery = this.core.getBatteryData();
		InverterData inverter = this.core.getInverterData();
		if (battery == null || inverter == null) {
			return null;
		}
		// Is battery full?
		if (battery.getSOE() < 99) {
			return null;
		}
		// Is PV producing?
		int pvPower = Math.round(inverter.getPvPower());
		if (pvPower < 10) {
			return null;
		}
		// Active Surplus feed-in
		return pvPower;
	}

	@Override
	public boolean isManaged() {
		return !this.config.readOnly();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				HybridEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(BpEss.class, accessMode, 100) //
						.build());
	}
}
