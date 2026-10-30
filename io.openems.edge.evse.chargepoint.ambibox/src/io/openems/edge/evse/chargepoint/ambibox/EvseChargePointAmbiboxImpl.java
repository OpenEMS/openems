package io.openems.edge.evse.chargepoint.ambibox;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.ambibox.enums.EvseState;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Ambibox", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
@GenerateTargetsFromReferences("Modbus")
public class EvseChargePointAmbiboxImpl extends EvseChargePointAmbibox
		implements EvseChargePoint, ElectricityMeter, OpenemsComponent, EventHandler, ModbusComponent {

	private final Logger log = LoggerFactory.getLogger(EvseChargePointAmbiboxImpl.class);

	private Config config;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvseChargePointAmbiboxImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointAmbibox.ChannelId.values() //
		);
		ElectricityMeter.calculatePhasesFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.config = config;
		super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var inputBase = this.config.connector().inputBaseAddress;
		final var holdingBase = this.config.connector().holdingBaseAddress;

		return new ModbusProtocol(this, //

				/*
				 * Input Registers of the EV charger; see 'User Interface (Modbus TCP)'.
				 * All values are 32 bit (2 registers each); unused registers are covered
				 * by DummyRegisterElements.
				 */
				new FC4ReadInputRegistersTask(inputBase, Priority.LOW, //

						// 0: Sleep (int32; 0: awake 1: asleep)
						m(EvseChargePointAmbibox.ChannelId.SLEEP, new SignedDoublewordElement(inputBase + 0)), //

						// 2: Current AC (float32 A)
						m(ElectricityMeter.ChannelId.CURRENT, //
								new FloatDoublewordElement(inputBase + 2), SCALE_FACTOR_3), //

						// 4/6/8: Current AC Phase 1..3 (float32 A)
						m(ElectricityMeter.ChannelId.CURRENT_L1, //
								new FloatDoublewordElement(inputBase + 4), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, //
								new FloatDoublewordElement(inputBase + 6), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, //
								new FloatDoublewordElement(inputBase + 8), SCALE_FACTOR_3), //

						// 10: Voltage AC (float32 V)
						m(ElectricityMeter.ChannelId.VOLTAGE, //
								new FloatDoublewordElement(inputBase + 10), SCALE_FACTOR_3), //

						// 12/14/16: Voltage AC Phase 1..3 (float32 V)
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, //
								new FloatDoublewordElement(inputBase + 12), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, //
								new FloatDoublewordElement(inputBase + 14), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, //
								new FloatDoublewordElement(inputBase + 16), SCALE_FACTOR_3), //

						// 18: Power AC (int32 W)
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, //
								new SignedDoublewordElement(inputBase + 18)), //

						// 20: Power Factor (float32)
						m(EvseChargePointAmbibox.ChannelId.POWER_FACTOR, //
								new FloatDoublewordElement(inputBase + 20)), //

						// 22: Grid Frequency (float32 Hz)
						m(ElectricityMeter.ChannelId.FREQUENCY, //
								new FloatDoublewordElement(inputBase + 22), SCALE_FACTOR_3), //

						// 24: Energy AC (float32 Wh) - skipped; see Energy AC import/export
						new DummyRegisterElement(inputBase + 24, inputBase + 25), //

						// 26: Energy AC import (float32 Wh)
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
								new FloatDoublewordElement(inputBase + 26)), //

						// 28: Energy AC export (float32 Wh)
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
								new FloatDoublewordElement(inputBase + 28)), //

						// 30: Voltage DC (float32 V)
						m(EvseChargePointAmbibox.ChannelId.VOLTAGE_DC, //
								new FloatDoublewordElement(inputBase + 30)), //

						// 32: Current DC (float32 A)
						m(EvseChargePointAmbibox.ChannelId.CURRENT_DC, //
								new FloatDoublewordElement(inputBase + 32)), //

						// 34: Power DC (int32 W)
						m(EvseChargePointAmbibox.ChannelId.POWER_DC, //
								new SignedDoublewordElement(inputBase + 34)), //

						// 36: Number Phases (uint32)
						m(EvseChargePointAmbibox.ChannelId.NUMBER_PHASES, //
								new UnsignedDoublewordElement(inputBase + 36)), //

						// 38: Minimum PowerAC (int32 W)
						m(EvseChargePointAmbibox.ChannelId.CHARGER_MIN_POWER, //
								new SignedDoublewordElement(inputBase + 38)), //

						// 40: Maximum PowerAC (int32 W)
						m(EvseChargePointAmbibox.ChannelId.CHARGER_MAX_POWER, //
								new SignedDoublewordElement(inputBase + 40)), //

						// 42: Inverter State (enum uint32)
						m(EvseChargePointAmbibox.ChannelId.INVERTER_STATE, //
								new UnsignedDoublewordElement(inputBase + 42)), //

						// 44: Inverter Error (enum uint32; flag set)
						m(EvseChargePointAmbibox.ChannelId.INVERTER_ERROR, //
								new UnsignedDoublewordElement(inputBase + 44)), //

						// 46: Inverter Temperature (float32 °C)
						m(EvseChargePointAmbibox.ChannelId.INVERTER_TEMPERATURE, //
								new FloatDoublewordElement(inputBase + 46)), //

						// 48: Capacity (float32 Wh)
						m(EvseChargePointAmbibox.ChannelId.CAPACITY, //
								new FloatDoublewordElement(inputBase + 48)), //

						// 50: Min State of Charge (float32 %)
						m(EvseChargePointAmbibox.ChannelId.MIN_SOC, //
								new FloatDoublewordElement(inputBase + 50)), //

						// 52: Max State of Charge (float32 %)
						m(EvseChargePointAmbibox.ChannelId.MAX_SOC, //
								new FloatDoublewordElement(inputBase + 52)), //

						// 54: State of Charge (float32 %)
						m(EvseChargePointAmbibox.ChannelId.EV_SOC, //
								new FloatDoublewordElement(inputBase + 54)), //

						// 56: State of Health (float32 %)
						m(EvseChargePointAmbibox.ChannelId.STATE_OF_HEALTH, //
								new FloatDoublewordElement(inputBase + 56)), //

						// 58: Time to full SoC (uint32 s)
						m(EvseChargePointAmbibox.ChannelId.TIME_TO_FULL_SOC, //
								new UnsignedDoublewordElement(inputBase + 58)), //

						// 60: Number Cycles (uint32)
						m(EvseChargePointAmbibox.ChannelId.NUMBER_CYCLES, //
								new UnsignedDoublewordElement(inputBase + 60)), //

						// 62: Min ChargePower (uint32 W)
						m(EvseChargePointAmbibox.ChannelId.EV_MIN_CHARGE_POWER, //
								new UnsignedDoublewordElement(inputBase + 62)), //

						// 64: Max ChargePower (uint32 W)
						m(EvseChargePointAmbibox.ChannelId.EV_MAX_CHARGE_POWER, //
								new UnsignedDoublewordElement(inputBase + 64)), //

						// 66: Min DischargePower (uint32 W); discharge management not implemented
						m(EvseChargePointAmbibox.ChannelId.EV_MIN_DISCHARGE_POWER, //
								new UnsignedDoublewordElement(inputBase + 66)), //

						// 68: Max DischargePower (uint32 W); discharge management not implemented
						m(EvseChargePointAmbibox.ChannelId.EV_MAX_DISCHARGE_POWER, //
								new UnsignedDoublewordElement(inputBase + 68)), //

						// 70: Battery State (enum uint32)
						m(EvseChargePointAmbibox.ChannelId.BATTERY_STATE, //
								new UnsignedDoublewordElement(inputBase + 70)), //

						// 72: Battery Temperature (float32 °C)
						m(EvseChargePointAmbibox.ChannelId.BATTERY_TEMPERATURE, //
								new FloatDoublewordElement(inputBase + 72)), //

						// 74: Battery Error (enum uint32; flag set)
						m(EvseChargePointAmbibox.ChannelId.BATTERY_ERROR, //
								new UnsignedDoublewordElement(inputBase + 74)), //

						// 76: Control Mode (enum uint32)
						m(EvseChargePointAmbibox.ChannelId.CONTROL_MODE, //
								new UnsignedDoublewordElement(inputBase + 76)), //

						// 78: Power DC Battery (int32 W)
						m(EvseChargePointAmbibox.ChannelId.POWER_DC_BATTERY, //
								new SignedDoublewordElement(inputBase + 78)), //

						// 80: Charge Protocol (enum uint32)
						m(EvseChargePointAmbibox.ChannelId.CHARGE_PROTOCOL, //
								new UnsignedDoublewordElement(inputBase + 80)), //

						// 82: Session State (enum uint32)
						m(EvseChargePointAmbibox.ChannelId.SESSION_STATE, //
								new UnsignedDoublewordElement(inputBase + 82)), //

						// 84: EV connected (bool)
						m(EvseChargePointAmbibox.ChannelId.EV_CONNECTED, //
								new UnsignedDoublewordElement(inputBase + 84)), //

						// 86: Seconds to departure (uint32 s)
						m(EvseChargePointAmbibox.ChannelId.SECONDS_TO_DEPARTURE, //
								new UnsignedDoublewordElement(inputBase + 86)), //

						// 88: Departure SoC (float32 %)
						m(EvseChargePointAmbibox.ChannelId.DEPARTURE_SOC, //
								new FloatDoublewordElement(inputBase + 88)), //

						// 90: Departure Energy (uint32 Wh)
						m(EvseChargePointAmbibox.ChannelId.DEPARTURE_ENERGY, //
								new UnsignedDoublewordElement(inputBase + 90)), //

						// 92: Min Energy Request (uint32 Wh)
						m(EvseChargePointAmbibox.ChannelId.MIN_ENERGY_REQUEST, //
								new UnsignedDoublewordElement(inputBase + 92)), //

						// 94: Max Energy Request (uint32 Wh)
						m(EvseChargePointAmbibox.ChannelId.MAX_ENERGY_REQUEST, //
								new UnsignedDoublewordElement(inputBase + 94)), //

						// 96: EvCharger Error (enum uint32; flag set)
						m(EvseChargePointAmbibox.ChannelId.EV_CHARGER_ERROR, //
								new UnsignedDoublewordElement(inputBase + 96)) //
										.onUpdateCallback(value -> this.setEvChargerErrorFlags(value)), //

						// 98: Energy AC import session (float32 Wh)
						m(EvseChargePointAmbibox.ChannelId.ENERGY_SESSION, //
								new FloatDoublewordElement(inputBase + 98)), //

						// 100: Energy AC export session (float32 Wh)
						m(EvseChargePointAmbibox.ChannelId.ENERGY_EXPORT_SESSION, //
								new FloatDoublewordElement(inputBase + 100)), //

						// 102: Replug Required (bool)
						m(EvseChargePointAmbibox.ChannelId.REPLUG_REQUIRED, //
								new UnsignedDoublewordElement(inputBase + 102)) //
				),

				/*
				 * Holding registers of the EV charger.
				 */
				new FC3ReadRegistersTask(holdingBase, Priority.LOW, //
						// 0: Target PowerAC (int32 W; negative value = charging)
						m(EvseChargePointAmbibox.ChannelId.SET_TARGET_POWER, //
								new SignedDoublewordElement(holdingBase), INVERT)), //

				new FC16WriteRegistersTask(holdingBase, //
						// 0: Target PowerAC (int32 W; negative value = charging)
						m(EvseChargePointAmbibox.ChannelId.SET_TARGET_POWER, //
								new SignedDoublewordElement(holdingBase), INVERT), //

						// 2: Wake Up (uint32; 1 = wake up)
						m(EvseChargePointAmbibox.ChannelId.SET_WAKE_UP, //
								new UnsignedDoublewordElement(holdingBase + 2)) //
				));
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.config == null) {
			return null;
		}
		if (this.config.readOnly()) {
			return ChargePointAbilities.create()//
					.build();
		}

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(THREE_PHASE, //
						ApplySetPoint.MIN_POWER_THREE_PHASE, //
						this.config.maxHwPower())) //
				.setIsEvConnected(this.getEvConnectedChannel().value().orElse(false)) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				// Phase Switch not available
				.setPhaseSwitchManual(null)//
				.build();
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config == null || this.config.readOnly()) {
			return;
		}

		this.applySetPoint(actions.getApplySetPointInWatt().value());
	}

	/**
	 * Applies the charge power set-point in [W] by writing it to the 'Target
	 * PowerAC' Holding Register. The sign is inverted when writing the register,
	 * i.e. negative register values charge the vehicle.
	 *
	 * @param setPointInWatt the charge power in [W]; 0 stops charging
	 */
	private void applySetPoint(int setPointInWatt) {
		try {
			this.setSetTargetPower(setPointInWatt);

			// Wake up the charger if charging is requested while it is asleep
			if (setPointInWatt > 0 && this.getSleepChannel().value().orElse(false)) {
				this.setWakeUp();
			}

		} catch (OpenemsNamedException e) {
			this.log.warn("Unable to apply set-point [" + setPointInWatt + " W]: " + e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append("L:").append(this.getActivePower().asString());
		if (!this.config.readOnly()) {
			b //
					.append("|SetTargetPower:") //
					.append(this.getSetTargetPowerChannel().value().asString()) //
					.append("|SessionState:") //
					.append(this.getSessionStateChannel().value().asString()) //
					.append("|EvConnected:") //
					.append(this.getEvConnectedChannel().value().asString());
		}
		return b.toString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.updateIsReadyForCharging();
		}
		}
	}

	/**
	 * Updates the IS_READY_FOR_CHARGING state.
	 */
	private void updateIsReadyForCharging() {
		if (this.config.readOnly()) {
			return;
		}

		final var isReadyForCharging = evaluateIsReadyForCharging(//
				this.getEvConnectedChannel().getNextValue().get(), //
				this.getSessionStateChannel().getNextValue().asEnum(), //
				this.getEvChargerErrorChannel().getNextValue().get());
		setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, isReadyForCharging);
	}

	/**
	 * Evaluates whether charging can be started or continued.
	 *
	 * @param evConnected    is an Electric-Vehicle connected
	 * @param sessionState   the {@link EvseState}
	 * @param evChargerError the 'EvCharger Error' flag set
	 * @return true if the Charge-Point is ready for charging
	 */
	private static boolean evaluateIsReadyForCharging(Boolean evConnected, EvseState sessionState,
			Integer evChargerError) {
		if (evConnected == null || !evConnected) {
			return false;
		}
		if (sessionState == EvseState.ERROR || sessionState == EvseState.UNDEFINED) {
			return false;
		}
		return evChargerError == null || evChargerError == 0;
	}

	/**
	 * Maps the 'EvCharger Error' flag set to the individual error channels.
	 *
	 * @param value the raw register value
	 */
	private void setEvChargerErrorFlags(Long value) {
		final var bits = value == null ? 0 : value;
		setValue(this, EvseChargePointAmbibox.ChannelId.ERR_EV_COMMUNICATION, (bits & (1 << 0)) != 0);
		setValue(this, EvseChargePointAmbibox.ChannelId.ERR_PE_COMMUNICATION, (bits & (1 << 1)) != 0);
		setValue(this, EvseChargePointAmbibox.ChannelId.ERR_LOAD_DUMP, (bits & (1 << 2)) != 0);
		setValue(this, EvseChargePointAmbibox.ChannelId.ERR_PLUG_TEMPERATURE, (bits & (1 << 3)) != 0);
		setValue(this, EvseChargePointAmbibox.ChannelId.ERR_CABLE_CHECK, (bits & (1 << 4)) != 0);
		setValue(this, EvseChargePointAmbibox.ChannelId.ERR_EMERGENCY_OFF, (bits & (1 << 5)) != 0);
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return PhaseRotation.L1_L2_L3;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}
}
