package io.openems.edge.sma.sunnyisland;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.sma.enums.PowerSupplyStatus;
import io.openems.edge.sma.enums.SetControlMode;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.SMA.SunnyIsland", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EssSmaSunnyIslandImpl extends AbstractOpenemsModbusComponent
		implements ManagedSinglePhaseEss, SinglePhaseEss, ManagedAsymmetricEss, AsymmetricEss, ManagedSymmetricEss,
		SymmetricEss, ModbusComponent, OpenemsComponent {

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;
	private SinglePhase singlePhase = null;

	public EssSmaSunnyIslandImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				ManagedSinglePhaseEss.ChannelId.values(), //
				EssSmaSunnyIsland.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// Evaluate 'SinglePhase'
		switch (config.phase()) {
		case ALL:
			this.singlePhase = null;
			break;
		case L1:
			this.singlePhase = SinglePhase.L1;
			break;
		case L2:
			this.singlePhase = SinglePhase.L2;
			break;
		case L3:
			this.singlePhase = SinglePhase.L3;
			break;
		}

		if (this.singlePhase != null) {
			SinglePhaseEss.initializeCopyPhaseChannel(this, this.singlePhase);
		}

		this._setGridMode(GridMode.ON_GRID);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if (this.config.readOnlyMode()) {
			return;
		}

		EnumWriteChannel setControlMode = this.channel(EssSmaSunnyIsland.ChannelId.SET_CONTROL_MODE);
		IntegerWriteChannel setActivePowerChannel = this.channel(EssSmaSunnyIsland.ChannelId.SET_ACTIVE_POWER);
		IntegerWriteChannel setReactivePowerChannel = this.channel(EssSmaSunnyIsland.ChannelId.SET_REACTIVE_POWER);

		setControlMode.setNextWriteValue(SetControlMode.START);
		setActivePowerChannel.setNextWriteValue(activePower);
		setReactivePowerChannel.setNextWriteValue(reactivePower);
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {
		if (this.config.phase() == Phase.ALL) {
			return;
		}

		ManagedSinglePhaseEss.super.applyPower(activePowerL1, reactivePowerL1, activePowerL2, reactivePowerL2,
				activePowerL3, reactivePowerL3);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(30051, Priority.LOW, //
						m(EssSmaSunnyIsland.ChannelId.DEVICE_CLASS, new UnsignedDoublewordElement(30051)), //
						m(EssSmaSunnyIsland.ChannelId.DEVICE_TYPE, new UnsignedDoublewordElement(30053)), //
						new DummyRegisterElement(30055, 30056), //
						m(EssSmaSunnyIsland.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(30057)), //
						m(EssSmaSunnyIsland.ChannelId.SOFTWARE_PACKAGE, new UnsignedDoublewordElement(30059))), //

				new FC3ReadRegistersTask(30199, Priority.LOW, //
						m(EssSmaSunnyIsland.ChannelId.WAITING_TIME_UNTIL_FEED_IN, new UnsignedDoublewordElement(30199)), //
						m(EssSmaSunnyIsland.ChannelId.SYSTEM_STATE, new UnsignedDoublewordElement(30201)), //
						new DummyRegisterElement(30203, 30210), //
						m(EssSmaSunnyIsland.ChannelId.RECOMMENDED_ACTION, new UnsignedDoublewordElement(30211)), //
						m(EssSmaSunnyIsland.ChannelId.MESSAGE, new UnsignedDoublewordElement(30213)), //
						m(EssSmaSunnyIsland.ChannelId.FAULT_CORRECTION_MEASURE, new UnsignedDoublewordElement(30215))), //

				new FC3ReadRegistersTask(30231, Priority.LOW, //
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new UnsignedDoublewordElement(30231),
								new ElementToChannelConverter(v -> {
									if (v == null) {
										return null;
									}
									int value = TypeUtils.getAsType(OpenemsType.INTEGER, v);
									// Evaluate symmetric/single-phase mode
									if (this.config.phase() == Phase.ALL) {
										// assuming 3 phases are done by Master/Slave
										return 3 * value;

									} else {
										return value;
									}
								}))), //

				new FC3ReadRegistersTask(30595, Priority.LOW,
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(30595)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(30597))), //

				new FC3ReadRegistersTask(30775, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(30775)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(30777)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(30779)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(30781)), //
						m(EssSmaSunnyIsland.ChannelId.GRID_VOLTAGE_L1, new SignedDoublewordElement(30783),
								SCALE_FACTOR_MINUS_2), //
						m(EssSmaSunnyIsland.ChannelId.GRID_VOLTAGE_L2, new SignedDoublewordElement(30785),
								SCALE_FACTOR_MINUS_2), //
						m(EssSmaSunnyIsland.ChannelId.GRID_VOLTAGE_L3, new SignedDoublewordElement(30787),
								SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(30789, 30802), //
						m(EssSmaSunnyIsland.ChannelId.FREQUENCY, new UnsignedDoublewordElement(30803), SCALE_FACTOR_1), //
						new DummyRegisterElement(30805, 30806), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(30807), INVERT), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(30809), INVERT), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(30811), INVERT)), //

				new FC3ReadRegistersTask(30835, Priority.LOW, //
						m(EssSmaSunnyIsland.ChannelId.OPERATING_MODE_FOR_ACTIVE_POWER_LIMITATION,
								new UnsignedDoublewordElement(30835))), //

				new FC3ReadRegistersTask(30843, Priority.HIGH, //
						// BatteryAmpere 30843
						m(EssSmaSunnyIsland.ChannelId.BATTERY_CURRENT, new SignedDoublewordElement(30843),
								SCALE_FACTOR_MINUS_3),
						m(SymmetricEss.ChannelId.SOC, new UnsignedDoublewordElement(30845)), //
						m(EssSmaSunnyIsland.ChannelId.CURRENT_BATTERY_CAPACITY, new SignedDoublewordElement(30847)), //
						m(EssSmaSunnyIsland.ChannelId.BATTERY_TEMPERATURE, new SignedDoublewordElement(30849),
								SCALE_FACTOR_MINUS_1), //
						m(EssSmaSunnyIsland.ChannelId.BATTERY_VOLTAGE, new UnsignedDoublewordElement(30851), //
								SCALE_FACTOR_1)),

				new FC3ReadRegistersTask(30877, Priority.LOW,
						m(EssSmaSunnyIsland.ChannelId.POWER_SUPPLY_STATUS, new UnsignedDoublewordElement(30877),
								// set values at SymmetricEss.ChannelId.GRID_MODE as well
								new ElementToChannelConverter((value) -> {
									if (value == null) {
										return null;
									}

									int intValue = TypeUtils.getAsType(OpenemsType.INTEGER, value);
									final GridMode gridMode;
									if (intValue == PowerSupplyStatus.OFF.getValue()) {
										gridMode = GridMode.OFF_GRID;

									} else {
										if (intValue == PowerSupplyStatus.UTILITY_GRID_CONNECTED.getValue()) {
											gridMode = GridMode.ON_GRID;

										} else {
											gridMode = GridMode.UNDEFINED;
										}
									}
									this._setGridMode(gridMode);

									return intValue;
								}))),

				new FC3ReadRegistersTask(30997, Priority.LOW,
						m(EssSmaSunnyIsland.ChannelId.LOWEST_MEASURED_BATTERY_TEMPERATURE,
								new SignedDoublewordElement(30997), SCALE_FACTOR_MINUS_1),
						m(EssSmaSunnyIsland.ChannelId.HIGHEST_MEASURED_BATTERY_TEMPERATURE,
								new SignedDoublewordElement(30999), SCALE_FACTOR_MINUS_1),
						m(EssSmaSunnyIsland.ChannelId.MAX_OCCURRED_BATTERY_VOLTAGE, new SignedDoublewordElement(31001),
								SCALE_FACTOR_MINUS_2)),

				new FC3ReadRegistersTask(40189, Priority.HIGH, //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedDoublewordElement(40189),
								INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedDoublewordElement(40191))), //

				new FC16WriteRegistersTask(40149, //
						m(EssSmaSunnyIsland.ChannelId.SET_ACTIVE_POWER, new SignedDoublewordElement(40149)), //
						m(EssSmaSunnyIsland.ChannelId.SET_CONTROL_MODE, new UnsignedDoublewordElement(40151)), //
						m(EssSmaSunnyIsland.ChannelId.SET_REACTIVE_POWER, new SignedDoublewordElement(40153))), //

				new FC16WriteRegistersTask(43090, //
						m(EssSmaSunnyIsland.ChannelId.GRID_GUARD_CODE, new UnsignedDoublewordElement(43090))), //

				new FC16WriteRegistersTask(40705,
						m(EssSmaSunnyIsland.ChannelId.MIN_SOC_POWER_ON, new UnsignedDoublewordElement(40705)), //
						m(EssSmaSunnyIsland.ChannelId.MIN_SOC_POWER_OFF, new UnsignedDoublewordElement(40707))));
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";" //
				+ this.getAllowedDischargePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public SinglePhase getPhase() {
		return this.singlePhase;
	}

}
