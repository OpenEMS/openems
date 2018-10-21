package io.openems.edge.sma;

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

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.SMA.SunnyIsland6.0H-11",
		// TODO naming "Ess.SMA...."
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SunnyIsland6Ess extends AbstractOpenemsModbusComponent
		implements SymmetricEss, ManagedSymmetricEss, OpenemsComponent {

	@Reference
	private Power power;

	protected final static int MAX_APPARENT_POWER = 4600;

	@Reference
	protected ConfigurationAdmin cm;

	public SunnyIsland6Ess() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		IntegerWriteChannel setGridGuard = this.channel(ChannelId.GRID_GUARD_CODE);
		IntegerWriteChannel setBmsOperatingMode = this.channel(ChannelId.BMS_OPERATING_MODE);
		IntegerWriteChannel setControlMode = this.channel(ChannelId.SET_CONTROL_MODE);
		IntegerWriteChannel setActivePowerChannel = this.channel(ChannelId.SET_ACTIVE_POWER);
		IntegerWriteChannel setMaximumBatteryChargingPower = this.channel(ChannelId.MAXIMUM_BATTERY_CHARGING_POWER);
		IntegerWriteChannel setMaximumBatteryDishargingPower = this
				.channel(ChannelId.MAXIMUM_BATTERY_DISCHARGING_POWER);
		IntegerWriteChannel setReactivePowerChannel = this.channel(ChannelId.SET_REACTIVE_POWER);
		setGridGuard.setNextValue(1);
		setBmsOperatingMode.setNextValue(2424);
		setControlMode.setNextValue(802);
		setMaximumBatteryChargingPower.setNextValue(4600);
		setMaximumBatteryDishargingPower.setNextValue(4600);
		setActivePowerChannel.setNextValue(activePower);
		setReactivePowerChannel.setNextValue(reactivePower);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
//TODO ENUM UNITS;
	// 887 = disable
	// rest of the Channels

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		ModbusProtocol protocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(30051, Priority.ONCE, //
						m(SunnyIsland6Ess.ChannelId.DEVICE_CLASS, new UnsignedDoublewordElement(30051)), //
						m(SunnyIsland6Ess.ChannelId.DEVICE_TYPE, new UnsignedDoublewordElement(30053)), //
						new DummyRegisterElement(30055, 30056), //
						m(SunnyIsland6Ess.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(30057)), //
						m(SunnyIsland6Ess.ChannelId.SOFTWARE_PACKAGE, new UnsignedDoublewordElement(30059))), //
				new FC3ReadRegistersTask(30199, Priority.ONCE, //
						m(SunnyIsland6Ess.ChannelId.WAITING_TIME_UNTIL_FEED_IN, new UnsignedDoublewordElement(30199))), //
				new FC3ReadRegistersTask(30201, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.SYSTEM_STATE, new UnsignedDoublewordElement(30201))), //
				new FC3ReadRegistersTask(30211, Priority.ONCE, //
						m(SunnyIsland6Ess.ChannelId.RECOMMENDED_ACTION, new UnsignedDoublewordElement(30211)), //
						m(SunnyIsland6Ess.ChannelId.MESSAGE, new UnsignedDoublewordElement(30213)), //
						m(SunnyIsland6Ess.ChannelId.FAULT_CORRECTION_MEASURE, new UnsignedDoublewordElement(30215))), //
				// new FC3ReadRegistersTask(30513, Priority.LOW,
//						m(SunnyIsland6Ess.ChannelId.TOTAL_YIELD, new UnsignedDoublewordElement(30513))), //
				new FC3ReadRegistersTask(30559, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.NUMBER_OF_EVENT_FOR_USER, new UnsignedDoublewordElement(30559)), //
						m(SunnyIsland6Ess.ChannelId.NUMBER_OF_EVENT_FOR_INSTALLER,
								new UnsignedDoublewordElement(30561)), //
						m(SunnyIsland6Ess.ChannelId.NUMBER_OF_EVENT_FOR_SERVICE, new UnsignedDoublewordElement(30563)), //
						m(SunnyIsland6Ess.ChannelId.NUMBER_OF_GENERATORS_STARTS, new UnsignedDoublewordElement(30565)), //
						m(SunnyIsland6Ess.ChannelId.AMP_HOURS_COUNTER_FOR_BATTERY_CHARGE,
								new UnsignedDoublewordElement(30567)), //
						m(SunnyIsland6Ess.ChannelId.AMP_HOURS_COUNTER_FOR_BATTERY_DISCHARGE,
								new UnsignedDoublewordElement(30569)), //
						m(SunnyIsland6Ess.ChannelId.METER_READING_CONSUMPTION_METER,
								new UnsignedDoublewordElement(30571)), //
						new DummyRegisterElement(30573, 30576), //
						m(SunnyIsland6Ess.ChannelId.ENERGY_CONSUMED_FROM_GRID, new UnsignedDoublewordElement(30577)), //
						m(SunnyIsland6Ess.ChannelId.ENERGY_FED_INTO_GRID, new UnsignedDoublewordElement(30579)), //
						m(SunnyIsland6Ess.ChannelId.GRID_REFERENCE_COUNTER_READING,
								new UnsignedDoublewordElement(30581)), //
						m(SunnyIsland6Ess.ChannelId.GRID_FEED_IN_COUNTER_READING, new UnsignedDoublewordElement(30583)), //
						m(SunnyIsland6Ess.ChannelId.POWER_OUTAGE, new UnsignedDoublewordElement(30585)), //
						new DummyRegisterElement(30587, 30588), //
						m(SunnyIsland6Ess.ChannelId.RISE_IN_SELF_CONSUMPTION, new UnsignedDoublewordElement(30589)), //
						m(SunnyIsland6Ess.ChannelId.RISE_IN_SELF_CONSUMPTION_TODAY,
								new UnsignedDoublewordElement(30591)), //
						new DummyRegisterElement(30593, 30594), //
						m(SunnyIsland6Ess.ChannelId.ABSORBED_ENERGY, new UnsignedDoublewordElement(30595)), //
						m(SunnyIsland6Ess.ChannelId.RELEASED_ENERGY, new UnsignedDoublewordElement(30597)), //
						m(SunnyIsland6Ess.ChannelId.NUMBER_OF_GRID_CONNECTIONS, new UnsignedDoublewordElement(30599))), //

				new FC3ReadRegistersTask(30775, Priority.HIGH, //
						// TODO For lines conf Asymmetric
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(30775)), //
						m(SunnyIsland6Ess.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(30777)), //
						m(SunnyIsland6Ess.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(30779)), //
						m(SunnyIsland6Ess.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(30781)), //
						m(SunnyIsland6Ess.ChannelId.GRID_VOLTAGE_L1, new SignedDoublewordElement(30783)), //
						m(SunnyIsland6Ess.ChannelId.GRID_VOLTAGE_L2, new SignedDoublewordElement(30785)), //
						m(SunnyIsland6Ess.ChannelId.GRID_VOLTAGE_L3, new SignedDoublewordElement(30787)), //
						new DummyRegisterElement(30789, 30802), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY, new UnsignedDoublewordElement(30803)), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(30805),
								ElementToChannelConverter.INVERT), //
						m(SunnyIsland6Ess.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(30807),
								ElementToChannelConverter.INVERT), //
						m(SunnyIsland6Ess.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(30809),
								ElementToChannelConverter.INVERT), //
						m(SunnyIsland6Ess.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(30811),
								ElementToChannelConverter.INVERT)), //
				new FC3ReadRegistersTask(30825, Priority.HIGH, //
						m(SunnyIsland6Ess.ChannelId.OPERATING_MODE_FOR_REACTIVE_POWER,
								new UnsignedDoublewordElement(30825))), //
				new FC3ReadRegistersTask(30835, Priority.HIGH, //
						m(SunnyIsland6Ess.ChannelId.OPERATING_MODE_FOR_ACTIVE_POWER,
								new UnsignedDoublewordElement(30835))), //
				new FC3ReadRegistersTask(30843, Priority.HIGH, //
						m(SunnyIsland6Ess.ChannelId.BATTERY_CURRENT, new SignedDoublewordElement(30843)),
						m(SymmetricEss.ChannelId.SOC, new UnsignedDoublewordElement(30845)), //
						new DummyRegisterElement(30847, 30848), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_TEMPERATURE, new SignedDoublewordElement(30849)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_VOLTAGE, new UnsignedDoublewordElement(30851))), //
				new FC3ReadRegistersTask(40189, Priority.HIGH, //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedDoublewordElement(40189),
								ElementToChannelConverter.INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedDoublewordElement(40191))), //
				new FC16WriteRegistersTask(40236, //
						m(SunnyIsland6Ess.ChannelId.BMS_OPERATING_MODE, new UnsignedDoublewordElement(40236))), //
				new FC16WriteRegistersTask(40212, //
						m(SunnyIsland6Ess.ChannelId.SET_ACTIVE_POWER, new SignedDoublewordElement(40212))), //
				new FC16WriteRegistersTask(40151,
						m(SunnyIsland6Ess.ChannelId.SET_CONTROL_MODE, new UnsignedDoublewordElement(40151)), //
						m(SunnyIsland6Ess.ChannelId.SET_REACTIVE_POWER, new SignedDoublewordElement(40153))), //
				new FC16WriteRegistersTask(43090, //
						m(SunnyIsland6Ess.ChannelId.GRID_GUARD_CODE, new UnsignedDoublewordElement(43090))), //
				new FC16WriteRegistersTask(40705,
						m(SunnyIsland6Ess.ChannelId.MIN_SOC_POWER_ON, new UnsignedDoublewordElement(40705)), //
						m(SunnyIsland6Ess.ChannelId.MIN_SOC_POWER_OFF, new UnsignedDoublewordElement(40707))), //
				new FC16WriteRegistersTask(40795, //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_BATTERY_CHARGING_POWER,
								new UnsignedDoublewordElement(40795)), //
						new DummyRegisterElement(40797, 40798), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_BATTERY_DISCHARGING_POWER,
								new UnsignedDoublewordElement(40799))));
		return protocol;
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		DEVICE_CLASS(new Doc()), //
		DEVICE_TYPE(new Doc()), //
		SERIAL_NUMBER(new Doc()), //
		SOFTWARE_PACKAGE(new Doc()), //
		WAITING_TIME_UNTIL_FEED_IN(new Doc()), //
		MESSAGE(new Doc()), //
		SYSTEM_STATE(new Doc()//
				.option(35, "Fehler")//
				.option(303, "Aus")//
				.option(307, "OK")//
				.option(455, "Warnung")), //
		RECOMMENDED_ACTION(new Doc()), //
		FAULT_CORRECTION_MEASURE(new Doc()), //
		TOTAL_YIELD(new Doc()), //
		NUMBER_OF_EVENT_FOR_USER(new Doc()), //
		NUMBER_OF_EVENT_FOR_INSTALLER(new Doc()), //
		NUMBER_OF_EVENT_FOR_SERVICE(new Doc()), //
		NUMBER_OF_GENERATORS_STARTS(new Doc()), //
		AMP_HOURS_COUNTER_FOR_BATTERY_CHARGE(new Doc()), //
		AMP_HOURS_COUNTER_FOR_BATTERY_DISCHARGE(new Doc()), //
		METER_READING_CONSUMPTION_METER(new Doc()), //
		ENERGY_CONSUMED_FROM_GRID(new Doc()), //
		ENERGY_FED_INTO_GRID(new Doc()), //
		GRID_REFERENCE_COUNTER_READING(new Doc()), //
		GRID_FEED_IN_COUNTER_READING(new Doc()), //
		POWER_OUTAGE(new Doc()), //
		RISE_IN_SELF_CONSUMPTION(new Doc()), //
		RISE_IN_SELF_CONSUMPTION_TODAY(new Doc()), //
		ABSORBED_ENERGY(new Doc()), //
		RELEASED_ENERGY(new Doc()), //
		NUMBER_OF_GRID_CONNECTIONS(new Doc()), //
		ACTIVE_POWER_L1(new Doc()), //
		ACTIVE_POWER_L2(new Doc()), //
		ACTIVE_POWER_L3(new Doc()), //
		GRID_VOLTAGE_L1(new Doc()), //
		GRID_VOLTAGE_L2(new Doc()), //
		GRID_VOLTAGE_L3(new Doc()), //
		FREQUENCY(new Doc().unit(Unit.MILLIHERTZ)), //
		REACTIVE_POWER_L1(new Doc()), //
		REACTIVE_POWER_L2(new Doc()), //
		REACTIVE_POWER_L3(new Doc()), //
		BATTERY_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		BATTERY_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE)), //
		MIN_SOC_POWER_ON(new Doc()), //
		GRID_GUARD_CODE(new Doc()), //
		MIN_SOC_POWER_OFF(new Doc()), //
		SET_CONTROL_MODE(new Doc()//
				.option(802, "START")//
				.option(803, "STOP")), // S
		METER_SETTING(new Doc()//
				.option(3053, "SMA Energy Meter")//
				.option(3547, "Wechselrichter")), //
		OPERATING_MODE_FOR_ACTIVE_POWER(new Doc()), //
		OPERATING_MODE_FOR_REACTIVE_POWER(new Doc()), //
		MAXIMUM_BATTERY_CHARGING_POWER(new Doc()), //
		MAXIMUM_BATTERY_DISCHARGING_POWER(new Doc()), //
		BMS_OPERATING_MODE(new Doc()), //
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public String debugLog() {
		return "Mode:" + this.channel(ChannelId.GRID_GUARD_CODE).getNextValue();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

}
