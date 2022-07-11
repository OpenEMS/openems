package io.openems.edge.sma.sunnyisland;

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
		name = "Ess.SMA.SunnyIsland", immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SunnyIslandEss extends AbstractOpenemsModbusComponent implements ManagedSinglePhaseEss, SinglePhaseEss,
		ManagedAsymmetricEss, AsymmetricEss, ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent {

	// TODO read MaxApparentPower from Modbus

	// SMA Sunny Island 6
	protected static final int MAX_APPARENT_POWER = 6000;

	// SMA Sunny Island 4
	// 4400W for max 30min
	// protected static final int MAX_APPARENT_POWER = 3300;

	private Config config;
	private SinglePhase singlePhase = null;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	public SunnyIslandEss() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				ManagedSinglePhaseEss.ChannelId.values(), //
				SiChannelId.values() //
		);

		this._setMaxApparentPower(SunnyIslandEss.MAX_APPARENT_POWER);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
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

		// Evaluate symmetric/single-phase mode
		if (config.phase() == Phase.ALL) {
			// assuming 3 phases are done by Master/Slave
			this._setMaxApparentPower(3 * SunnyIslandEss.MAX_APPARENT_POWER);

		} else {
			this._setMaxApparentPower(SunnyIslandEss.MAX_APPARENT_POWER);
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

		EnumWriteChannel setControlMode = this.channel(SiChannelId.SET_CONTROL_MODE);
		IntegerWriteChannel setActivePowerChannel = this.channel(SiChannelId.SET_ACTIVE_POWER);
		IntegerWriteChannel setReactivePowerChannel = this.channel(SiChannelId.SET_REACTIVE_POWER);

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
				new FC3ReadRegistersTask(30051, Priority.ONCE, //
						m(SiChannelId.DEVICE_CLASS, new UnsignedDoublewordElement(30051)), //
						m(SiChannelId.DEVICE_TYPE, new UnsignedDoublewordElement(30053)).debug(), //
						new DummyRegisterElement(30055, 30056), //
						m(SiChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(30057)).debug(), //
						m(SiChannelId.SOFTWARE_PACKAGE, new UnsignedDoublewordElement(30059))), //

				new FC3ReadRegistersTask(30199, Priority.ONCE, //
						m(SiChannelId.WAITING_TIME_UNTIL_FEED_IN, new UnsignedDoublewordElement(30199))), //

				new FC3ReadRegistersTask(30201, Priority.LOW, //
						m(SiChannelId.SYSTEM_STATE, new UnsignedDoublewordElement(30201))), //

				new FC3ReadRegistersTask(30211, Priority.ONCE, //
						m(SiChannelId.RECOMMENDED_ACTION, new UnsignedDoublewordElement(30211)), //
						m(SiChannelId.MESSAGE, new UnsignedDoublewordElement(30213)), //
						m(SiChannelId.FAULT_CORRECTION_MEASURE, new UnsignedDoublewordElement(30215))), //

				new FC3ReadRegistersTask(30595, Priority.LOW,
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(30595)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(30597))), //

				new FC3ReadRegistersTask(30775, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(30775)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(30777)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(30779)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(30781)), //
						m(SiChannelId.GRID_VOLTAGE_L1, new SignedDoublewordElement(30783),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(SiChannelId.GRID_VOLTAGE_L2, new SignedDoublewordElement(30785),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(SiChannelId.GRID_VOLTAGE_L3, new SignedDoublewordElement(30787),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(30789, 30802), //
						m(SiChannelId.FREQUENCY, new UnsignedDoublewordElement(30803),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						new DummyRegisterElement(30805, 30806), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(30807),
								ElementToChannelConverter.INVERT), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(30809),
								ElementToChannelConverter.INVERT), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(30811),
								ElementToChannelConverter.INVERT)), //

				new FC3ReadRegistersTask(30835, Priority.LOW, //
						m(SiChannelId.OPERATING_MODE_FOR_ACTIVE_POWER_LIMITATION,
								new UnsignedDoublewordElement(30835))), //

				new FC3ReadRegistersTask(30843, Priority.HIGH, //
						// BatteryAmpere 30843
						m(SiChannelId.BATTERY_CURRENT, new SignedDoublewordElement(30843),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(SymmetricEss.ChannelId.SOC, new UnsignedDoublewordElement(30845)), //
						m(SiChannelId.CURRENT_BATTERY_CAPACITY, new SignedDoublewordElement(30847)), //
						m(SiChannelId.BATTERY_TEMPERATURE, new SignedDoublewordElement(30849),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SiChannelId.BATTERY_VOLTAGE, new UnsignedDoublewordElement(30851), //
								ElementToChannelConverter.SCALE_FACTOR_1)),

				new FC3ReadRegistersTask(30877, Priority.LOW,
						m(SiChannelId.POWER_SUPPLY_STATUS, new UnsignedDoublewordElement(30877),
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
						m(SiChannelId.LOWEST_MEASURED_BATTERY_TEMPERATURE, new SignedDoublewordElement(30997),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SiChannelId.HIGHEST_MEASURED_BATTERY_TEMPERATURE, new SignedDoublewordElement(30999),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SiChannelId.MAX_OCCURRED_BATTERY_VOLTAGE, new SignedDoublewordElement(31001),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)),

				new FC3ReadRegistersTask(40189, Priority.HIGH, //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedDoublewordElement(40189),
								ElementToChannelConverter.INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedDoublewordElement(40191))), //

				new FC16WriteRegistersTask(40149, //
						m(SiChannelId.SET_ACTIVE_POWER, new SignedDoublewordElement(40149)).debug(), //
						m(SiChannelId.SET_CONTROL_MODE, new UnsignedDoublewordElement(40151)).debug(), //
						m(SiChannelId.SET_REACTIVE_POWER, new SignedDoublewordElement(40153)).debug()), //

				new FC16WriteRegistersTask(43090, //
						m(SiChannelId.GRID_GUARD_CODE, new UnsignedDoublewordElement(43090))), //

				new FC16WriteRegistersTask(40705, m(SiChannelId.MIN_SOC_POWER_ON, new UnsignedDoublewordElement(40705)), //
						m(SiChannelId.MIN_SOC_POWER_OFF, new UnsignedDoublewordElement(40707))));
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
