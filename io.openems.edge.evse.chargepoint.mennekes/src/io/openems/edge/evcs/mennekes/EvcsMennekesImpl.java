package io.openems.edge.evcs.mennekes;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.evcs.api.Evcs.calculateUsedPhasesFromCurrent;
import static io.openems.edge.meter.api.ElectricityMeter.calculateAverageVoltageFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.calculateSumActivePowerFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.calculateSumCurrentFromPhases;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.evse.chargepoint.bender.EvseChargePointBender;
import io.openems.edge.evse.chargepoint.bender.OcppState;
import io.openems.edge.evse.chargepoint.mennekes.common.AbstractMennekes;
import io.openems.edge.evse.chargepoint.mennekes.common.Mennekes;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Mennekes", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvcsMennekesImpl extends AbstractMennekes
		implements Evcs, ElectricityMeter, ManagedEvcs, OpenemsComponent, ModbusComponent, EventHandler, Mennekes {

	private final Logger log = LoggerFactory.getLogger(EvcsMennekesImpl.class);

	// TODO: Add functionality to distinguish between firmware version. For firmware
	// version >= 5.22 there are several new registers. Currently it is programmed
	// for firmware version 5.14.
	// private boolean softwareVersionSmallerThan_5_22 = true;

	private Config config;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * Handles charge states.
	 */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	public EvcsMennekesImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePointBender.ChannelId.values(), //
				Mennekes.ChannelId.values());

		calculateUsedPhasesFromCurrent(this);
		calculateSumCurrentFromPhases(this);
		calculateSumActivePowerFromPhases(this);
		calculateAverageVoltageFromPhases(this);

		// Calculates required Channels from other existing Channels.
		this.addStatusListener();
	}

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limit and the phases used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.applyConfig(config);
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	private void applyConfig(Config config) {
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
		this._setPowerPrecision(230);
		this._setPhases(3);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_EXECUTE_WRITE -> {
			if (!this.isReadOnly()) {
				this.writeHandler.run();
			}
			break;
		}
		}
	}

	private void addStatusListener() {
		this.channel(EvseChargePointBender.ChannelId.OCPP_CP_STATUS).onSetNextValue(s -> {
			final OcppState rawState = s == null //
					? OcppState.UNAVAILABLE //
					: s.asEnum();

			// Maps the raw state into a {@link Status}.
			this._setStatus(switch (rawState) {
			case CHARGING, FINISHING //
				-> Status.CHARGING;
			case FAULTED //
				-> Status.ERROR;
			case PREPARING //
				-> Status.READY_FOR_CHARGING;
			case RESERVED //
				-> Status.NOT_READY_FOR_CHARGING;
			case AVAILABLE, SUSPENDED_EV, SUSPENDED_EVSE //
				-> Status.CHARGING_REJECTED;
			case OCCUPIED //
				-> this.getActivePower().orElse(0) > 0 //
						? Status.CHARGING //
						: Status.CHARGING_REJECTED;
			case UNAVAILABLE //
				-> Status.ERROR;
			case UNDEFINED //
				-> Status.UNDEFINED;
			});
		});
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
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
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
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
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	@Override
	public void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.getSetChargePowerLimit().orElse(null) + "|" + this.getStatus().getName();
	}

	public boolean isCharging() {
		return this.getActivePower().orElse(0) > 0;
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		if (this.isReadOnly()) {
			return false;
		}
		var phases = this.getPhasesAsInt();
		var current = Math.round(power / phases / Evcs.DEFAULT_VOLTAGE);

		/*
		 * Limits the charging value because Mennekes knows only values between 6 and 32
		 * A
		 */
		current = Math.min(current, Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT / 1000);

		if (current < Evcs.DEFAULT_MINIMUM_HARDWARE_CURRENT / 1000) {
			current = 0;
		}

		try {
			this.getApplyCurrentLimitChannel().setNextWriteValue(current);
		} catch (OpenemsNamedException e) {
			this.log.warn("Failed to apply current limit.", e);
		}
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public MeterType getMeterType() {
		if (this.config.readOnly()) {
			return MeterType.CONSUMPTION_METERED;
		} else {
			return MeterType.MANAGED_CONSUMPTION_METERED;
		}
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

}
