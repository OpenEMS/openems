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

import io.openems.common.exceptions.OpenemsException;
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
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.SMA.SunnyIsland6.0H-11", immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SunnyIsland6Ess extends AbstractOpenemsModbusComponent
		implements SinglePhaseEss, SymmetricEss, ManagedSymmetricEss, OpenemsComponent {

	@Reference
	private Power power;

	protected final static int MAX_APPARENT_POWER = 6000;

	@Reference
	protected ConfigurationAdmin cm;

	public SunnyIsland6Ess() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	// TODO IMP!! LOAD_POWER "30861"
	@Override
	public void applyPower(int activePower, int reactivePower) {
		IntegerWriteChannel setControlMode = this.channel(ChannelId.SET_CONTROL_MODE);
		IntegerWriteChannel setActivePowerChannel = this.channel(ChannelId.SET_ACTIVE_POWER);
		IntegerWriteChannel setReactivePowerChannel = this.channel(ChannelId.SET_REACTIVE_POWER);
		try {
			setControlMode.setNextWriteValue(SetControlMode.START);
			setActivePowerChannel.setNextWriteValue(activePower);
			setReactivePowerChannel.setNextWriteValue(reactivePower);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

//	private IntegerWriteChannel getSetControlMode() {
//		return this.channel(SunnyIsland6Ess.ChannelId.SET_CONTROL_MODE);
//	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.getPhase().setNextValue(config.Phase());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

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
				// TODO Quadruple
				// new FC3ReadRegistersTask(30513, Priority.LOW,
				// m(SunnyIsland6Ess.ChannelId.TOTAL_YIELD, new
				// UnsignedDoublewordElement(30513))), //
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
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(30775)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(30777)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(30779)), //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(30781)), //
						m(SunnyIsland6Ess.ChannelId.GRID_VOLTAGE_L1, new SignedDoublewordElement(30783),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(SunnyIsland6Ess.ChannelId.GRID_VOLTAGE_L2, new SignedDoublewordElement(30785),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(SunnyIsland6Ess.ChannelId.GRID_VOLTAGE_L3, new SignedDoublewordElement(30787),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(30789, 30802), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY, new UnsignedDoublewordElement(30803)), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(30805),
								ElementToChannelConverter.INVERT), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(30807),
								ElementToChannelConverter.INVERT), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(30809),
								ElementToChannelConverter.INVERT), //
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(30811),
								ElementToChannelConverter.INVERT)), //
				new FC3ReadRegistersTask(30825, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.OPERATING_MODE_FOR_REACTIVE_POWER,
								new UnsignedDoublewordElement(30825))), //
				new FC3ReadRegistersTask(30831, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.COSPHI_SET_POINT_READ, new SignedDoublewordElement(30831)), //
						new DummyRegisterElement(30833, 30834), //
						m(SunnyIsland6Ess.ChannelId.OPERATING_MODE_FOR_ACTIVE_POWER_LIMITATION,
								new UnsignedDoublewordElement(30835))), //
				new FC3ReadRegistersTask(30845, Priority.HIGH, //
						m(SymmetricEss.ChannelId.SOC, new UnsignedDoublewordElement(30845)), //
						m(SunnyIsland6Ess.ChannelId.CURRENT_BATTERY_CAPACITY, new SignedDoublewordElement(30847)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_TEMPERATURE, new SignedDoublewordElement(30849),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_VOLTAGE, new UnsignedDoublewordElement(30851)), //
						m(SunnyIsland6Ess.ChannelId.ACTIVE_BATTERY_CHARGING_MODE, new UnsignedDoublewordElement(30853)), //
						m(SunnyIsland6Ess.ChannelId.CURRENT_BATTERY_CHARGING_SET_VOLTAGE,
								new UnsignedDoublewordElement(30855))), //
				new FC3ReadRegistersTask(30857, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.NUMBER_OF_BATTERY_CHARGE_THROUGHPUTS,
								new SignedDoublewordElement(30857)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_MAINT_SOC, new UnsignedDoublewordElement(30859)), //
						m(SunnyIsland6Ess.ChannelId.LOAD_POWER, new SignedDoublewordElement(30861)), //
						new DummyRegisterElement(30863, 30864), //
						m(SunnyIsland6Ess.ChannelId.POWER_GRID_REFERENCE, new SignedDoublewordElement(30865)), //
						m(SunnyIsland6Ess.ChannelId.POWER_GRID_FEED_IN, new SignedDoublewordElement(30867)), //
						m(SunnyIsland6Ess.ChannelId.PV_POWER_GENERATED, new SignedDoublewordElement(30869)), //
						m(SunnyIsland6Ess.ChannelId.CURRENT_SELF_CONSUMPTION, new UnsignedDoublewordElement(30871)), //
						m(SunnyIsland6Ess.ChannelId.CURRENT_RISE_IN_SELF_CONSUMPTION,
								new SignedDoublewordElement(30873)), //
						m(SunnyIsland6Ess.ChannelId.MULTIFUNCTION_RELAY_STATUS, new UnsignedDoublewordElement(30875)), //
						m(SunnyIsland6Ess.ChannelId.POWER_SUPPLY_STATUS, new UnsignedDoublewordElement(30877)), //
						m(SunnyIsland6Ess.ChannelId.REASON_FOR_GENERATOR_REQUEST, new UnsignedDoublewordElement(30879)), //
						m(SunnyIsland6Ess.ChannelId.PV_MAINS_CONNECTION, new UnsignedDoublewordElement(30881)), //
						m(SunnyIsland6Ess.ChannelId.STATUS_OF_UTILITY_GRID, new UnsignedDoublewordElement(30883)), //
						new DummyRegisterElement(30885, 30900), //
						m(SunnyIsland6Ess.ChannelId.GRID_FREQ_OF_EXTERNAL_POWER_CONNECTION,
								new UnsignedDoublewordElement(30901)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_A,
								new UnsignedDoublewordElement(30903)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_B,
								new UnsignedDoublewordElement(30905)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_C,
								new UnsignedDoublewordElement(30907)), //
						m(SunnyIsland6Ess.ChannelId.CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_A,
								new SignedDoublewordElement(30909)), //
						m(SunnyIsland6Ess.ChannelId.CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_B,
								new SignedDoublewordElement(30911)), //
						m(SunnyIsland6Ess.ChannelId.CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_C,
								new SignedDoublewordElement(30913)), //
						new DummyRegisterElement(30915, 30916), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_STATUS, new UnsignedDoublewordElement(30917)), //
						new DummyRegisterElement(30919, 30924), //
						m(SunnyIsland6Ess.ChannelId.DATA_TRANSFER_RATE_OF_NETWORK_TERMINAL_A,
								new UnsignedDoublewordElement(30925)), //
						m(SunnyIsland6Ess.ChannelId.DUPLEX_MODE_OF_NETWORK_TERMINAL_A,
								new UnsignedDoublewordElement(30927)), //
						m(SunnyIsland6Ess.ChannelId.SPEED_WIRE_CONNECTION_STATUS_OF_NETWORK_TERMINAL_A,
								new UnsignedDoublewordElement(30929))), //

				new FC3ReadRegistersTask(30977, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.GRID_CURRENT_L1, new SignedDoublewordElement(30977)), //
						m(SunnyIsland6Ess.ChannelId.GRID_CURRENT_L2, new SignedDoublewordElement(30979)), //
						m(SunnyIsland6Ess.ChannelId.GRID_CURRENT_L3, new SignedDoublewordElement(30981)), //
						m(SunnyIsland6Ess.ChannelId.OUTPUT_OF_PHOTOVOLTAICS, new UnsignedDoublewordElement(30983)), //
						m(SunnyIsland6Ess.ChannelId.TOTAL_CURRENT_EXTERNAL_GRID_CONNECTION,
								new SignedDoublewordElement(30985)), //
						m(SunnyIsland6Ess.ChannelId.FAULT_BATTERY_SOC, new UnsignedDoublewordElement(30987)), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_BATTERY_CURRENT_IN_CHARGE_DIRECTION,
								new UnsignedDoublewordElement(30989)), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_BATTERY_CURRENT_IN_DISCHARGE_DIRECTION,
								new UnsignedDoublewordElement(30991)), //
						m(SunnyIsland6Ess.ChannelId.CHARGE_FACTOR_RATIO_OF_BATTERY_CHARGE_DISCHARGE,
								new UnsignedDoublewordElement(30993)), //
						m(SunnyIsland6Ess.ChannelId.OPERATING_TIME_OF_BATTERY_STATISTICS_COUNTER,
								new UnsignedDoublewordElement(30995), ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
						m(SunnyIsland6Ess.ChannelId.LOWEST_MEASURED_BATTERY_TEMPERATURE,
								new SignedDoublewordElement(30997), ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SunnyIsland6Ess.ChannelId.HIGHEST_MEASURED_BATTERY_TEMPERATURE,
								new SignedDoublewordElement(30999), ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(SunnyIsland6Ess.ChannelId.MAX_OCCURRED_BATTERY_VOLTAGE, new UnsignedDoublewordElement(31001)), //
						m(SunnyIsland6Ess.ChannelId.REMAINING_TIME_UNTIL_FULL_CHARGE,
								new UnsignedDoublewordElement(31003)), //
						m(SunnyIsland6Ess.ChannelId.REMAINING_TIME_UNTIL_EQUALIZATION_CHARGE,
								new UnsignedDoublewordElement(31005), ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SunnyIsland6Ess.ChannelId.REMAINING_ABSORPTION_TIME, new UnsignedDoublewordElement(31007)), //
						m(SunnyIsland6Ess.ChannelId.LOWER_DISCHARGE_LIMIT_FOR_SELF_CONSUMPTION_RANGE,
								new UnsignedDoublewordElement(31009)), //
						m(SunnyIsland6Ess.ChannelId.TOTAL_OUTPUT_CURRENT_OF_SOLAR_CHARGER,
								new UnsignedDoublewordElement(31011)), //
						m(SunnyIsland6Ess.ChannelId.REMAINING_MIN_OPERATING_TIME_OF_GENERATOR,
								new UnsignedDoublewordElement(31013)), //
						m(SunnyIsland6Ess.ChannelId.OPERATING_STATUS_MASTER_L1, new UnsignedDoublewordElement(31015)), //
						// TODO str32 for speed wire ip address
						new DummyRegisterElement(31017, 31056), //
						m(SunnyIsland6Ess.ChannelId.STATUS_BATTERY_APPLICATION_AREA,
								new UnsignedDoublewordElement(31057)), //
						m(SunnyIsland6Ess.ChannelId.ABSORPTION_PHASE_ACTIVE, new UnsignedDoublewordElement(31059)), //
						m(SunnyIsland6Ess.ChannelId.CONTROL_OF_BATTERY_CHARGING_VIA_COMMUNICATION_AVAILABLE,
								new UnsignedDoublewordElement(31061)), //
						m(SunnyIsland6Ess.ChannelId.TOTAL_ENERGY_PHOTOVOLTAICS, new UnsignedDoublewordElement(31063)), //
						m(SunnyIsland6Ess.ChannelId.TOTAL_ENERGY_PHOTOVOLTAICS_CURRENT_DAY,
								new UnsignedDoublewordElement(31065)), //
						m(SunnyIsland6Ess.ChannelId.NUMBER_OF_EQALIZATION_CHARGES,
								new UnsignedDoublewordElement(31067)), //
						m(SunnyIsland6Ess.ChannelId.NUMBER_OF_FULL_CHARGES, new UnsignedDoublewordElement(31069)), //
						m(SunnyIsland6Ess.ChannelId.RELATIVE_BATTERY_DISCHARGING_SINCE_THE_LAST_FULL_CHARGE,
								new UnsignedDoublewordElement(31071)), //
						m(SunnyIsland6Ess.ChannelId.RELATIVE_BATTERY_DISCHARGING_SINCE_LAST_EQUALIZATION_CHARGE,
								new UnsignedDoublewordElement(31073)), //
						m(SunnyIsland6Ess.ChannelId.OPERATING_TIME_ENERGY_COUNT, new UnsignedDoublewordElement(31075)), //
						m(SunnyIsland6Ess.ChannelId.PHOTOVOLTAIC_ENERGY_IN_SOLAR_CHARGER,
								new UnsignedDoublewordElement(31077))), //
				new FC3ReadRegistersTask(31393, Priority.HIGH, //
						m(SunnyIsland6Ess.ChannelId.BATTERY_CHARGING_SOC, new UnsignedDoublewordElement(31393)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_DISCHARGING_SOC, new UnsignedDoublewordElement(31395))), //
				new FC3ReadRegistersTask(31417, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.OUTPUT_EXTERNAL_POWER_CONNECTION,
								new SignedDoublewordElement(31417)), //
						m(SunnyIsland6Ess.ChannelId.OUTPUT_EXTERNAL_POWER_CONNECTION_L1,
								new SignedDoublewordElement(31419)), //
						m(SunnyIsland6Ess.ChannelId.OUTPUT_EXTERNAL_POWER_CONNECTION_L2,
								new SignedDoublewordElement(31421)), //
						m(SunnyIsland6Ess.ChannelId.OUTPUT_EXTERNAL_POWER_CONNECTION_L3,
								new SignedDoublewordElement(31423)), //
						m(SunnyIsland6Ess.ChannelId.REACTIVE_POWER_EXTERNAL_POWER_CONNECTION,
								new SignedDoublewordElement(31425)), //
						m(SunnyIsland6Ess.ChannelId.REACTIVE_POWER_EXTERNAL_POWER_CONNECTION_L1,
								new SignedDoublewordElement(31427)), //
						m(SunnyIsland6Ess.ChannelId.REACTIVE_POWER_EXTERNAL_POWER_CONNECTION_L2,
								new SignedDoublewordElement(31429)), //
						m(SunnyIsland6Ess.ChannelId.REACTIVE_POWER_EXTERNAL_POWER_CONNECTION_L3,
								new SignedDoublewordElement(31431))), //
				new FC3ReadRegistersTask(34657, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.STATUS_DIGITAL_INPUT, new UnsignedDoublewordElement(34657))), //
				new FC3ReadRegistersTask(40031, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.RATED_BATTERY_CAPACITY, new UnsignedDoublewordElement(40031)), //
						m(SunnyIsland6Ess.ChannelId.MAX_BATTERY_TEMPERATURE, new UnsignedDoublewordElement(40033)), //
						new DummyRegisterElement(40035, 40036), //
						m(SunnyIsland6Ess.ChannelId.RATED_BATTERY_VOLTAGE, new UnsignedDoublewordElement(40037)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_BOOST_CHARGE_TIME, new UnsignedDoublewordElement(40039)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_EQUALIZATION_CHARGE_TIME,
								new UnsignedDoublewordElement(40041)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_FULL_CHARGE_TIME, new UnsignedDoublewordElement(40043)), //
						m(SunnyIsland6Ess.ChannelId.MAX_BATTERY_CHARGING_CURRENT, new UnsignedDoublewordElement(40045)), //
						m(SunnyIsland6Ess.ChannelId.RATED_GENERATOR_CURRENT, new UnsignedDoublewordElement(40047)), //
						m(SunnyIsland6Ess.ChannelId.AUTOMATIC_GENERATOR_START, new UnsignedDoublewordElement(40049)), //
						new DummyRegisterElement(40051, 40054), //
						m(SunnyIsland6Ess.ChannelId.MANUAL_GENERATOR_CONTROL, new UnsignedDoublewordElement(40055)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_REQUEST_VIA_POWER_ON,
								new UnsignedDoublewordElement(40057)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_SHUT_DOWN_LOAD_LIMIT,
								new UnsignedDoublewordElement(40059)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_START_UP_LOAD_LIMIT,
								new UnsignedDoublewordElement(40061)), //
						m(SunnyIsland6Ess.ChannelId.FIRMWARE_VERSION_OF_THE_MAIN_PROCESSOR,
								new UnsignedDoublewordElement(40063)), //
						m(SunnyIsland6Ess.ChannelId.FIRMWARE_VERSION_OF_THE_LOGIC_COMPONENET,
								new UnsignedDoublewordElement(40065)), //
						new DummyRegisterElement(40067, 40070), //
						m(SunnyIsland6Ess.ChannelId.GRID_CREATING_GENERATOR, new UnsignedDoublewordElement(40071)), //
						new DummyRegisterElement(40073, 40074), //
						m(SunnyIsland6Ess.ChannelId.RISE_IN_SELF_CONSUMPTION_SWITCHED_ON,
								new UnsignedDoublewordElement(40075)), //
						m(SunnyIsland6Ess.ChannelId.INITIATE_DEVICE_RESTART, new UnsignedDoublewordElement(40077)), //
						new DummyRegisterElement(40079, 40084), //
						m(SunnyIsland6Ess.ChannelId.CELL_CHARGE_NOMINAL_VOLTAGE_FOR_BOOST_CHARGE,
								new UnsignedDoublewordElement(40085)), //
						m(SunnyIsland6Ess.ChannelId.CELL_CHARGE_NOMINAL_VOLTAGE_FOR_FULL_CHARGE,
								new UnsignedDoublewordElement(40087)), //
						m(SunnyIsland6Ess.ChannelId.CELL_CHARGE_NOMINAL_VOLTAGE_FOR_EQUALIZATION_CHARGE,
								new UnsignedDoublewordElement(40089)), //
						m(SunnyIsland6Ess.ChannelId.CELL_CHARGE_NOMINAL_VOLTAGE_FOR_FLOAT_CHARGE,
								new UnsignedDoublewordElement(40091)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_UPPER_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40093)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_UPPER_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40095)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_HYSTERESIS_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40097)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_HYSTERESIS_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40099)), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_UPPER_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40101)), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_UPPER_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40103)), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_HYSTERESIS_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40105)), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_HYSTERESIS_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40107)),
						new DummyRegisterElement(40109, 40110), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_GENERATOR_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40111)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_GENERATOR_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40113)), //
						new DummyRegisterElement(40115, 40118), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_GENERATOR_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40119)), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_GENERATOR_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40121)), //
						new DummyRegisterElement(40123, 40126), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_GENERATOR_MAXIMUM_REVERSE_POWER,
								new UnsignedDoublewordElement(40127)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_GENERATOR_MAXIMUM_REVERSE_POWER_TRIPPING_TIME,
								new UnsignedDoublewordElement(40129)), //
						new DummyRegisterElement(40131, 40134), //
						m(SunnyIsland6Ess.ChannelId.NOMINAL_FREQUENCY, new UnsignedDoublewordElement(40135)), //
						m(SunnyIsland6Ess.ChannelId.ACKNOWLEGDE_GENERATOR_ERRORS, new UnsignedDoublewordElement(40137)), //
						new DummyRegisterElement(40139, 40186), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_NOMINAL_CAPACITY, new UnsignedDoublewordElement(40187))), //
				new FC3ReadRegistersTask(40216, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.OPERATING_MODE_OF_ACTIVE_POWER_LIMITATION_AT_OVERFREQUENCY,
								new UnsignedDoublewordElement(40216)), //
						m(SunnyIsland6Ess.ChannelId.DIFFERENCE_BETWEEN_STARTING_FREQ_AND_GRID_FREQ,
								new UnsignedDoublewordElement(40218)), //
						m(SunnyIsland6Ess.ChannelId.DIFFERENCE_BETWEEN_RESET_FREQ_AND_GRID_FREQ,
								new UnsignedDoublewordElement(40220)), //
						m(SunnyIsland6Ess.ChannelId.COSPHI_AT_STARTING_POINT, new UnsignedDoublewordElement(40222)), //
						m(SunnyIsland6Ess.ChannelId.CONFIGURATION_OF_THE_COSPHI_STARTING_POINT,
								new UnsignedDoublewordElement(40224)), //
						m(SunnyIsland6Ess.ChannelId.COSPHI_AT_THE_END_POINT, new UnsignedDoublewordElement(40226)), //
						m(SunnyIsland6Ess.ChannelId.CONFIGURATION_OF_THE_COSPHI_END_POINT,
								new UnsignedDoublewordElement(40228)), //
						m(SunnyIsland6Ess.ChannelId.ACTIVE_POWER_AT_STARTING_POINT,
								new UnsignedDoublewordElement(40230)), //
						m(SunnyIsland6Ess.ChannelId.ACTIVE_POWER_AT_END_POINT, new UnsignedDoublewordElement(40232)), //
						new DummyRegisterElement(40234, 40237), //
						m(SunnyIsland6Ess.ChannelId.ACTIVE_POWER_GRADIENT_CONFIGURATION,
								new UnsignedDoublewordElement(40238)), //
						new DummyRegisterElement(40240, 40520), //
						m(SunnyIsland6Ess.ChannelId.GRID_REQUEST_VIA_POWER_SWITCH_ON,
								new UnsignedDoublewordElement(40521)), //
						m(SunnyIsland6Ess.ChannelId.GRID_REQUEST_SWITCH_ON_POWER_LIMIT,
								new UnsignedDoublewordElement(40523)), //
						m(SunnyIsland6Ess.ChannelId.GRID_REQUEST_SWITCH_OFF_POWER_LIMIT,
								new UnsignedDoublewordElement(40525)), //
						m(SunnyIsland6Ess.ChannelId.MANUAL_CONTROL_OF_NETWORK_CONNECTION,
								new UnsignedDoublewordElement(40527)), //
						m(SunnyIsland6Ess.ChannelId.GRID_REQUEST_VIA_CHARGE_TYPE, new UnsignedDoublewordElement(40529)), //
						m(SunnyIsland6Ess.ChannelId.TYPE_OF_AC_SUBDISTRIBUTION, new UnsignedDoublewordElement(40531)), //
						m(SunnyIsland6Ess.ChannelId.MANUAL_EQUAIZATION_CHARGE, new UnsignedDoublewordElement(40533)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_REQUEST, new UnsignedDoublewordElement(40535)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_GENERATOR_START_IN_TIME_RANGE,
								new UnsignedDoublewordElement(40537)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_GENERATOR_SHUTDOWN_IN_TIME_RANGE,
								new UnsignedDoublewordElement(40539)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_ADDTIONAL_TIME_RANGE_GENERATOR_REQUEST,
								new UnsignedDoublewordElement(40541)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_RANGE_FOR_GENERATOR_REQUEST,
								new UnsignedDoublewordElement(40543)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_GENERATOR_STOP_ADD_IN_TIME_RANGE,
								new UnsignedDoublewordElement(40545)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_GENERATOR_START_ADD_IN_TIME_RANGE,
								new UnsignedDoublewordElement(40547)), //
						m(SunnyIsland6Ess.ChannelId.TIME_CONTROLLED_GENERATOR_OPERATION,
								new UnsignedDoublewordElement(40549)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_FOR_TIME_CONTROLLED_GENERATOR_OPERATION,
								new UnsignedDoublewordElement(40551)), //
						m(SunnyIsland6Ess.ChannelId.OPERATING_TIME_FOR_TIME_CONTROLLED_GENERATOR_OPERATION,
								new UnsignedDoublewordElement(40553)), //
						m(SunnyIsland6Ess.ChannelId.REPETITION_CYCLE_OF_TIME_CONTROLLED_GENERATOR_OPERATION,
								new UnsignedDoublewordElement(40555)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_REQUEST_WITH_SET_CHARGE_TYPE,
								new UnsignedDoublewordElement(40557)), //
						m(SunnyIsland6Ess.ChannelId.REACTION_TO_DIGITAL_INPUT_OF_GENERATOR_REQUEST,
								new UnsignedDoublewordElement(40559)), //
						m(SunnyIsland6Ess.ChannelId.AVERAGE_TIME_FOR_GENERATOR_REQUEST_VIA_POWER,
								new UnsignedDoublewordElement(40561)), //
						m(SunnyIsland6Ess.ChannelId.AVERAGE_OPERATING_TIME_OF_GENERATOR,
								new UnsignedDoublewordElement(40563)), //
						m(SunnyIsland6Ess.ChannelId.AVERAGE_IDLE_PERIOD_OF_GENERATOR,
								new UnsignedDoublewordElement(40565)), //
						m(SunnyIsland6Ess.ChannelId.COOLING_DOWN_TIME_OF_GENERATOR,
								new UnsignedDoublewordElement(40567)), //
						m(SunnyIsland6Ess.ChannelId.IDLE_PERIOD_AFTER_GENERATOR_FAULT,
								new UnsignedDoublewordElement(40569)), //
						m(SunnyIsland6Ess.ChannelId.WARM_UP_TIME_OF_GENERATOR, new UnsignedDoublewordElement(40571)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_NOMINAL_FREQUENCY, new UnsignedDoublewordElement(40573)), //
						new DummyRegisterElement(40575, 40622), //
						m(SunnyIsland6Ess.ChannelId.TIME_CONTROLLED_INVERTER_OPERATION,
								new UnsignedDoublewordElement(40623)), //
						new DummyRegisterElement(40625, 40626), //
						m(SunnyIsland6Ess.ChannelId.OPERATING_TIME_FOR_TIME_CONTROLLED_INVERTER,
								new UnsignedDoublewordElement(40627)), //
						m(SunnyIsland6Ess.ChannelId.REPETITION_CYCLE_OF_TIME_CONTROLLED_INVERTER,
								new UnsignedDoublewordElement(40629)), //
						m(SunnyIsland6Ess.ChannelId.DEVICE_NAME, new UnsignedDoublewordElement(40631)), //
						new DummyRegisterElement(40633, 40646), //
						m(SunnyIsland6Ess.ChannelId.AUTOMATIC_UPDATES_ACTIVATED, new UnsignedDoublewordElement(40647)), //
						m(SunnyIsland6Ess.ChannelId.TIME_OF_THE_AUTOMATIC_UPDATE, new UnsignedDoublewordElement(40649)), //
						new DummyRegisterElement(40651, 40662), //
						m(SunnyIsland6Ess.ChannelId.GRID_GUARD_VERSION, new UnsignedDoublewordElement(40663)), //
						m(SunnyIsland6Ess.ChannelId.MEMORY_CARD_STATUS, new UnsignedDoublewordElement(40665)), //
						m(SunnyIsland6Ess.ChannelId.UPDATE_VERSION_OF_THE_MAIN_PROCESSOR,
								new UnsignedDoublewordElement(40667)), //
						m(SunnyIsland6Ess.ChannelId.START_FEED_IN_PV, new UnsignedDoublewordElement(40669)), //
						m(SunnyIsland6Ess.ChannelId.STOP_FEED_IN_PV, new UnsignedDoublewordElement(40671)), //
						m(SunnyIsland6Ess.ChannelId.CUT_OFF_TIME_UNTIL_CONNECTION_TO_EXTERNAL_NETWORK,
								new UnsignedDoublewordElement(40673)), //
						m(SunnyIsland6Ess.ChannelId.AUTOMATIC_FREQUENCY_SYNCHRONIZATION,
								new UnsignedDoublewordElement(40675)), //
						m(SunnyIsland6Ess.ChannelId.MAXIUMUM_CURRENT_FROM_PUBLIC_GRID,
								new UnsignedDoublewordElement(40677)), //
						m(SunnyIsland6Ess.ChannelId.POWER_FEEDBACK_TO_PUBLIC_GRID_ALLOWED,
								new UnsignedDoublewordElement(40679)), //
						m(SunnyIsland6Ess.ChannelId.GRID_REQUEST_VIA_SOC_SWITCHED_ON,
								new UnsignedDoublewordElement(40681)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_CONNECTION_TO_GRID,
								new UnsignedDoublewordElement(40683)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_DISCONNECTION_FROM_GRID,
								new UnsignedDoublewordElement(40685)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_ADDTIONAL_TIME_RANGE_GRID_REQUEST,
								new UnsignedDoublewordElement(40687)), //
						m(SunnyIsland6Ess.ChannelId.START_INTERVAL_GRID_REQUEST, new UnsignedDoublewordElement(40689)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_CONNECT_TO_GRID_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40691)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_DISCONNECT_FROM_GRID_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40693)), //
						m(SunnyIsland6Ess.ChannelId.ENERGY_SAVING_MODE_SWITCH_ON, new UnsignedDoublewordElement(40695)), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_GRID_REVERSE_POWER, new UnsignedDoublewordElement(40697)), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_GRID_REVERSE_POWER_TRIPPING_TIME,
								new UnsignedDoublewordElement(40699)), //
						m(SunnyIsland6Ess.ChannelId.TIME_UNTIL_CHANGE_OVER_TO_ENERGY_SAVING_MODE,
								new UnsignedDoublewordElement(40701)), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_DURATION_OF_ENERGY_SAVING_MODE,
								new UnsignedDoublewordElement(40703)), //
						new DummyRegisterElement(40705, 40708), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_OF_BATTERY_PROTECTION_MODE_LEVEL,
								new UnsignedDoublewordElement(40709)), //
						m(SunnyIsland6Ess.ChannelId.END_TIME_OF_BATTERY_PROTECTION_MODE_LEVEL,
								new UnsignedDoublewordElement(40711)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_SOC_FOR_PROTECTION_MODE,
								new UnsignedDoublewordElement(40713)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_SWITCH_ONLIMIT_AFTER_OVER_TEMP_SHUT_DOWN,
								new UnsignedDoublewordElement(40715)), //
						m(SunnyIsland6Ess.ChannelId.OUTPUT_RESISTANCE_OF_BATTERY_CONNECTION,
								new UnsignedDoublewordElement(40717)), //
						m(SunnyIsland6Ess.ChannelId.LOWER_LIMIT_DEEP_DISCHARGE_PROTECT_AREA_PRIOR_SHUTDOWN,
								new UnsignedDoublewordElement(40719)), //
						m(SunnyIsland6Ess.ChannelId.MINIMUM_WIDTH_OF_DEEP_DISCHARGE_PROTECTION_AREA,
								new UnsignedDoublewordElement(40721)), //
						m(SunnyIsland6Ess.ChannelId.MINIMUM_WIDTH_OF_BAKCUP_POWER_AREA,
								new UnsignedDoublewordElement(40723)), //
						m(SunnyIsland6Ess.ChannelId.AREA_WIDTH_FOR_CONSERVING_SOC,
								new UnsignedDoublewordElement(40725)), //
						m(SunnyIsland6Ess.ChannelId.MINIMUM_WIDTH_OF_OWN_CONSUMPTION_AREA,
								new UnsignedDoublewordElement(40727)), //
						m(SunnyIsland6Ess.ChannelId.MOST_PRODUCTIVE_MONTH_FOR_BATTERY_USAGE_RANGE,
								new UnsignedDoublewordElement(40729)), //
						m(SunnyIsland6Ess.ChannelId.SEASON_OPERATION_ACTIVE, new UnsignedDoublewordElement(40731)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_SET_POINT_WITH_DEACTIVATED_BATTERY_MENAGEMENT,
								new UnsignedDoublewordElement(40733)), //
						m(SunnyIsland6Ess.ChannelId.CYCLE_TIME_FOR_FULL_CHARGE, new UnsignedDoublewordElement(40735)), //
						m(SunnyIsland6Ess.ChannelId.CYCLE_TIME_FOR_EQUALIZATION_CHARGE,
								new UnsignedDoublewordElement(40737)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_TEMPERATUR_COMPENSATION,
								new UnsignedDoublewordElement(40739)), //
						m(SunnyIsland6Ess.ChannelId.AUTOMATIC_EQUALIZATION_CHARGE,
								new UnsignedDoublewordElement(40741)), //
						m(SunnyIsland6Ess.ChannelId.TYPE_OF_ADDTIONAL_DC_SOURCES, new UnsignedDoublewordElement(40743)), //
						m(SunnyIsland6Ess.ChannelId.LIMITATION_TYPE_OF_GENERATOR_CURRENT,
								new UnsignedDoublewordElement(40745)), //
						m(SunnyIsland6Ess.ChannelId.SENSIVITY_OF_GENERATOR_FAILURE_DETECTION,
								new UnsignedDoublewordElement(40747)), //
						new DummyRegisterElement(40749, 40750), //
						m(SunnyIsland6Ess.ChannelId.INVERTER_NOMINAL_VOLTAGE, new UnsignedDoublewordElement(40751)), //
						m(SunnyIsland6Ess.ChannelId.INVERTER_NOMINAL_FREQUENCY, new UnsignedDoublewordElement(40753)), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_AC_BATTERY_CHARGE_CURRENT,
								new UnsignedDoublewordElement(40755)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_VALUE_SOC_FOR_START_LOAD_SHEDDING_1,
								new UnsignedDoublewordElement(40757)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_VALUE_SOC_FOR_STOP_LOAD_SHEDDING_1,
								new UnsignedDoublewordElement(40759)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_ADDITIONAL_TIME_RANGE_LOAD_SHEDDING_1,
								new UnsignedDoublewordElement(40761)), //
						m(SunnyIsland6Ess.ChannelId.TIME_LOAD_SHEDDING_1, new UnsignedDoublewordElement(40763)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_START_LOAD_SHEDDING_1_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40765)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_STOP_LOAD_SHEDDING_1_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40767)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_VALUE_SOC_FOR_START_LOAD_SHEDDING_2,
								new UnsignedDoublewordElement(40769)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_VALUE_SOC_FOR_STOP_LOAD_SHEDDING_2,
								new UnsignedDoublewordElement(40771)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_ADDITIONAL_TIME_RANGE_LOAD_SHEDDING_2,
								new UnsignedDoublewordElement(40773)), //
						m(SunnyIsland6Ess.ChannelId.TIME_LOAD_SHEDDING_2, new UnsignedDoublewordElement(40775)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_START_LOAD_SHEDDING_2_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40777)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_STOP_LOAD_SHEDDING_2_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40779)), //
						new DummyRegisterElement(40781, 40786), //
						m(SunnyIsland6Ess.ChannelId.CLUSTER_BEHAVIOUR_WHEN_A_DEVICE_FAILS,
								new UnsignedDoublewordElement(40787)), //
						m(SunnyIsland6Ess.ChannelId.COMMUNICATION_VERSION, new UnsignedDoublewordElement(40789)), //
						m(SunnyIsland6Ess.ChannelId.TIME_OUT_FOR_COMMUNICATION_FAULT_INDICATION,
								new UnsignedDoublewordElement(40791)), //
						new DummyRegisterElement(40793, 40804), //
						m(SunnyIsland6Ess.ChannelId.ENERGY_SAVING_MODE, new UnsignedDoublewordElement(40805)), //
						new DummyRegisterElement(40807, 40810), //
						m(SunnyIsland6Ess.ChannelId.UPDATE_VERSION_OF_THE_LOGIC_COMPONENT,
								new UnsignedDoublewordElement(40811)), //
						new DummyRegisterElement(40813, 40818), //
						m(SunnyIsland6Ess.ChannelId.FIRMWARE_VERSION_OF_PROTOCOL_CONVERTER,
								new UnsignedDoublewordElement(40819)), //
						m(SunnyIsland6Ess.ChannelId.HARDWARE_VERSION_OF_PROTOCOL_CONVERTER,
								new UnsignedDoublewordElement(40821)), //
						new DummyRegisterElement(40823, 40826), //
						m(SunnyIsland6Ess.ChannelId.SERIAL_NUMBER_OF_THE_PROTOCOL_CONVERTER,
								new UnsignedDoublewordElement(40827))),

				new FC16WriteRegistersTask(40033, //
						m(SunnyIsland6Ess.ChannelId.MAX_BATTERY_TEMPERATURE, new UnsignedDoublewordElement(40033),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						new DummyRegisterElement(40035, 40036), //
						m(SunnyIsland6Ess.ChannelId.RATED_BATTERY_VOLTAGE, new UnsignedDoublewordElement(40037)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_BOOST_CHARGE_TIME, new UnsignedDoublewordElement(40039)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_EQUALIZATION_CHARGE_TIME,
								new UnsignedDoublewordElement(40041)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_FULL_CHARGE_TIME, new UnsignedDoublewordElement(40043)), //
						m(SunnyIsland6Ess.ChannelId.MAX_BATTERY_CHARGING_CURRENT, new UnsignedDoublewordElement(40045)), //
						m(SunnyIsland6Ess.ChannelId.RATED_GENERATOR_CURRENT, new UnsignedDoublewordElement(40047)), //
						m(SunnyIsland6Ess.ChannelId.AUTOMATIC_GENERATOR_START, new UnsignedDoublewordElement(40049)), //
						new DummyRegisterElement(40051, 40054), //
						m(SunnyIsland6Ess.ChannelId.MANUAL_GENERATOR_CONTROL, new UnsignedDoublewordElement(40055)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_REQUEST_VIA_POWER_ON,
								new UnsignedDoublewordElement(40057)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_SHUT_DOWN_LOAD_LIMIT,
								new UnsignedDoublewordElement(40059)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_START_UP_LOAD_LIMIT,
								new UnsignedDoublewordElement(40061)), //
						new DummyRegisterElement(40063, 40070), //
						m(SunnyIsland6Ess.ChannelId.GRID_CREATING_GENERATOR, new UnsignedDoublewordElement(40071)), //
						new DummyRegisterElement(40073, 40074), //
						m(SunnyIsland6Ess.ChannelId.RISE_IN_SELF_CONSUMPTION_SWITCHED_ON,
								new UnsignedDoublewordElement(40075)), //
						m(SunnyIsland6Ess.ChannelId.INITIATE_DEVICE_RESTART, new UnsignedDoublewordElement(40077)), //
						new DummyRegisterElement(40079, 40084), //
						m(SunnyIsland6Ess.ChannelId.CELL_CHARGE_NOMINAL_VOLTAGE_FOR_BOOST_CHARGE,
								new UnsignedDoublewordElement(40085)), //
						m(SunnyIsland6Ess.ChannelId.CELL_CHARGE_NOMINAL_VOLTAGE_FOR_FULL_CHARGE,
								new UnsignedDoublewordElement(40087)), //
						m(SunnyIsland6Ess.ChannelId.CELL_CHARGE_NOMINAL_VOLTAGE_FOR_EQUALIZATION_CHARGE,
								new UnsignedDoublewordElement(40089)), //
						m(SunnyIsland6Ess.ChannelId.CELL_CHARGE_NOMINAL_VOLTAGE_FOR_FLOAT_CHARGE,
								new UnsignedDoublewordElement(40091)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_UPPER_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40093)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_UPPER_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40095)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_HYSTERESIS_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40097)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_HYSTERESIS_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40099)), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_UPPER_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40101)), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_UPPER_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40103)), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_HYSTERESIS_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40105)), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_HYSTERESIS_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40107)),
						new DummyRegisterElement(40109, 40110), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_GENERATOR_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40111)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_GENERATOR_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40113)), //
						new DummyRegisterElement(40115, 40118), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_GENERATOR_MINIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40119)), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY_MONITORING_GENERATOR_MAXIMUM_THRESHOLD,
								new UnsignedDoublewordElement(40121)), //
						new DummyRegisterElement(40123, 40126), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_GENERATOR_MAXIMUM_REVERSE_POWER,
								new UnsignedDoublewordElement(40127)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_MONITORING_GENERATOR_MAXIMUM_REVERSE_POWER_TRIPPING_TIME,
								new UnsignedDoublewordElement(40129)), //
						new DummyRegisterElement(40131, 40134), //
						m(SunnyIsland6Ess.ChannelId.NOMINAL_FREQUENCY, new UnsignedDoublewordElement(40135)), //
						m(SunnyIsland6Ess.ChannelId.ACKNOWLEGDE_GENERATOR_ERRORS, new UnsignedDoublewordElement(40137)), //
						new DummyRegisterElement(40139, 40215), //
						m(SunnyIsland6Ess.ChannelId.OPERATING_MODE_OF_ACTIVE_POWER_LIMITATION_AT_OVERFREQUENCY,
								new UnsignedDoublewordElement(40216)), //
						m(SunnyIsland6Ess.ChannelId.DIFFERENCE_BETWEEN_STARTING_FREQ_AND_GRID_FREQ,
								new UnsignedDoublewordElement(40218)), //
						m(SunnyIsland6Ess.ChannelId.DIFFERENCE_BETWEEN_RESET_FREQ_AND_GRID_FREQ,
								new UnsignedDoublewordElement(40220)), //
						m(SunnyIsland6Ess.ChannelId.COSPHI_AT_STARTING_POINT, new UnsignedDoublewordElement(40222)), //
						m(SunnyIsland6Ess.ChannelId.CONFIGURATION_OF_THE_COSPHI_STARTING_POINT,
								new UnsignedDoublewordElement(40224)), //
						m(SunnyIsland6Ess.ChannelId.COSPHI_AT_THE_END_POINT, new UnsignedDoublewordElement(40226)), //
						m(SunnyIsland6Ess.ChannelId.CONFIGURATION_OF_THE_COSPHI_END_POINT,
								new UnsignedDoublewordElement(40228)), //
						m(SunnyIsland6Ess.ChannelId.ACTIVE_POWER_AT_STARTING_POINT,
								new UnsignedDoublewordElement(40230)), //
						m(SunnyIsland6Ess.ChannelId.ACTIVE_POWER_AT_END_POINT, new UnsignedDoublewordElement(40232)), //
						new DummyRegisterElement(40234, 40237), //
						m(SunnyIsland6Ess.ChannelId.ACTIVE_POWER_GRADIENT_CONFIGURATION,
								new UnsignedDoublewordElement(40238)), //
						new DummyRegisterElement(40240, 40520), //
						m(SunnyIsland6Ess.ChannelId.GRID_REQUEST_VIA_POWER_SWITCH_ON,
								new UnsignedDoublewordElement(40521)), //
						m(SunnyIsland6Ess.ChannelId.GRID_REQUEST_SWITCH_ON_POWER_LIMIT,
								new UnsignedDoublewordElement(40523)), //
						m(SunnyIsland6Ess.ChannelId.GRID_REQUEST_SWITCH_OFF_POWER_LIMIT,
								new UnsignedDoublewordElement(40525)), //
						m(SunnyIsland6Ess.ChannelId.MANUAL_CONTROL_OF_NETWORK_CONNECTION,
								new UnsignedDoublewordElement(40527)), //
						m(SunnyIsland6Ess.ChannelId.GRID_REQUEST_VIA_CHARGE_TYPE, new UnsignedDoublewordElement(40529)), //
						new DummyRegisterElement(40531, 40532), //
						m(SunnyIsland6Ess.ChannelId.MANUAL_EQUAIZATION_CHARGE, new UnsignedDoublewordElement(40533)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_REQUEST, new UnsignedDoublewordElement(40535)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_GENERATOR_START_IN_TIME_RANGE,
								new UnsignedDoublewordElement(40537)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_GENERATOR_SHUTDOWN_IN_TIME_RANGE,
								new UnsignedDoublewordElement(40539)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_ADDTIONAL_TIME_RANGE_GENERATOR_REQUEST,
								new UnsignedDoublewordElement(40541)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_RANGE_FOR_GENERATOR_REQUEST,
								new UnsignedDoublewordElement(40543)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_GENERATOR_STOP_ADD_IN_TIME_RANGE,
								new UnsignedDoublewordElement(40545)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_GENERATOR_START_ADD_IN_TIME_RANGE,
								new UnsignedDoublewordElement(40547)), //
						m(SunnyIsland6Ess.ChannelId.TIME_CONTROLLED_GENERATOR_OPERATION,
								new UnsignedDoublewordElement(40549)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_FOR_TIME_CONTROLLED_GENERATOR_OPERATION,
								new UnsignedDoublewordElement(40551)), //
						m(SunnyIsland6Ess.ChannelId.OPERATING_TIME_FOR_TIME_CONTROLLED_GENERATOR_OPERATION,
								new UnsignedDoublewordElement(40553)), //
						m(SunnyIsland6Ess.ChannelId.REPETITION_CYCLE_OF_TIME_CONTROLLED_GENERATOR_OPERATION,
								new UnsignedDoublewordElement(40555)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_REQUEST_WITH_SET_CHARGE_TYPE,
								new UnsignedDoublewordElement(40557)), //
						m(SunnyIsland6Ess.ChannelId.REACTION_TO_DIGITAL_INPUT_OF_GENERATOR_REQUEST,
								new UnsignedDoublewordElement(40559)), //
						m(SunnyIsland6Ess.ChannelId.AVERAGE_TIME_FOR_GENERATOR_REQUEST_VIA_POWER,
								new UnsignedDoublewordElement(40561)), //
						m(SunnyIsland6Ess.ChannelId.AVERAGE_OPERATING_TIME_OF_GENERATOR,
								new UnsignedDoublewordElement(40563)), //
						m(SunnyIsland6Ess.ChannelId.AVERAGE_IDLE_PERIOD_OF_GENERATOR,
								new UnsignedDoublewordElement(40565)), //
						m(SunnyIsland6Ess.ChannelId.COOLING_DOWN_TIME_OF_GENERATOR,
								new UnsignedDoublewordElement(40567)), //
						m(SunnyIsland6Ess.ChannelId.IDLE_PERIOD_AFTER_GENERATOR_FAULT,
								new UnsignedDoublewordElement(40569)), //
						m(SunnyIsland6Ess.ChannelId.WARM_UP_TIME_OF_GENERATOR, new UnsignedDoublewordElement(40571)), //
						m(SunnyIsland6Ess.ChannelId.GENERATOR_NOMINAL_FREQUENCY, new UnsignedDoublewordElement(40573)), //
						new DummyRegisterElement(40575, 40622), //
						m(SunnyIsland6Ess.ChannelId.TIME_CONTROLLED_INVERTER_OPERATION,
								new UnsignedDoublewordElement(40623)), //
						new DummyRegisterElement(40625, 40626), //
						m(SunnyIsland6Ess.ChannelId.OPERATING_TIME_FOR_TIME_CONTROLLED_INVERTER,
								new UnsignedDoublewordElement(40627)), //
						m(SunnyIsland6Ess.ChannelId.REPETITION_CYCLE_OF_TIME_CONTROLLED_INVERTER,
								new UnsignedDoublewordElement(40629)), //
						m(SunnyIsland6Ess.ChannelId.DEVICE_NAME, new UnsignedDoublewordElement(40631)), //
						new DummyRegisterElement(40633, 40646), //
						m(SunnyIsland6Ess.ChannelId.AUTOMATIC_UPDATES_ACTIVATED, new UnsignedDoublewordElement(40647)), //
						m(SunnyIsland6Ess.ChannelId.TIME_OF_THE_AUTOMATIC_UPDATE, new UnsignedDoublewordElement(40649)), //
						new DummyRegisterElement(40651, 40668), //
						m(SunnyIsland6Ess.ChannelId.START_FEED_IN_PV, new UnsignedDoublewordElement(40669)), //
						m(SunnyIsland6Ess.ChannelId.STOP_FEED_IN_PV, new UnsignedDoublewordElement(40671)), //
						m(SunnyIsland6Ess.ChannelId.CUT_OFF_TIME_UNTIL_CONNECTION_TO_EXTERNAL_NETWORK,
								new UnsignedDoublewordElement(40673)), //
						m(SunnyIsland6Ess.ChannelId.AUTOMATIC_FREQUENCY_SYNCHRONIZATION,
								new UnsignedDoublewordElement(40675)), //
						m(SunnyIsland6Ess.ChannelId.MAXIUMUM_CURRENT_FROM_PUBLIC_GRID,
								new UnsignedDoublewordElement(40677)), //
						m(SunnyIsland6Ess.ChannelId.POWER_FEEDBACK_TO_PUBLIC_GRID_ALLOWED,
								new UnsignedDoublewordElement(40679)), //
						m(SunnyIsland6Ess.ChannelId.GRID_REQUEST_VIA_SOC_SWITCHED_ON,
								new UnsignedDoublewordElement(40681)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_CONNECTION_TO_GRID,
								new UnsignedDoublewordElement(40683)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_DISCONNECTION_FROM_GRID,
								new UnsignedDoublewordElement(40685)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_ADDTIONAL_TIME_RANGE_GRID_REQUEST,
								new UnsignedDoublewordElement(40687)), //
						m(SunnyIsland6Ess.ChannelId.START_INTERVAL_GRID_REQUEST, new UnsignedDoublewordElement(40689)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_CONNECT_TO_GRID_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40691)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_DISCONNECT_FROM_GRID_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40693)), //
						m(SunnyIsland6Ess.ChannelId.ENERGY_SAVING_MODE_SWITCH_ON, new UnsignedDoublewordElement(40695)), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_GRID_REVERSE_POWER, new UnsignedDoublewordElement(40697)), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_GRID_REVERSE_POWER_TRIPPING_TIME,
								new UnsignedDoublewordElement(40699)), //
						m(SunnyIsland6Ess.ChannelId.TIME_UNTIL_CHANGE_OVER_TO_ENERGY_SAVING_MODE,
								new UnsignedDoublewordElement(40701)), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_DURATION_OF_ENERGY_SAVING_MODE,
								new UnsignedDoublewordElement(40703)), //
						new DummyRegisterElement(40705, 40708), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_OF_BATTERY_PROTECTION_MODE_LEVEL,
								new UnsignedDoublewordElement(40709)), //
						m(SunnyIsland6Ess.ChannelId.END_TIME_OF_BATTERY_PROTECTION_MODE_LEVEL,
								new UnsignedDoublewordElement(40711)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_SOC_FOR_PROTECTION_MODE,
								new UnsignedDoublewordElement(40713)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_SWITCH_ONLIMIT_AFTER_OVER_TEMP_SHUT_DOWN,
								new UnsignedDoublewordElement(40715)), //
						m(SunnyIsland6Ess.ChannelId.OUTPUT_RESISTANCE_OF_BATTERY_CONNECTION,
								new UnsignedDoublewordElement(40717)), //
						m(SunnyIsland6Ess.ChannelId.LOWER_LIMIT_DEEP_DISCHARGE_PROTECT_AREA_PRIOR_SHUTDOWN,
								new UnsignedDoublewordElement(40719)), //
						m(SunnyIsland6Ess.ChannelId.MINIMUM_WIDTH_OF_DEEP_DISCHARGE_PROTECTION_AREA,
								new UnsignedDoublewordElement(40721)), //
						m(SunnyIsland6Ess.ChannelId.MINIMUM_WIDTH_OF_BAKCUP_POWER_AREA,
								new UnsignedDoublewordElement(40723)), //
						m(SunnyIsland6Ess.ChannelId.AREA_WIDTH_FOR_CONSERVING_SOC,
								new UnsignedDoublewordElement(40725)), //
						m(SunnyIsland6Ess.ChannelId.MINIMUM_WIDTH_OF_OWN_CONSUMPTION_AREA,
								new UnsignedDoublewordElement(40727)), //
						m(SunnyIsland6Ess.ChannelId.MOST_PRODUCTIVE_MONTH_FOR_BATTERY_USAGE_RANGE,
								new UnsignedDoublewordElement(40729)), //
						m(SunnyIsland6Ess.ChannelId.SEASON_OPERATION_ACTIVE, new UnsignedDoublewordElement(40731)), //
						m(SunnyIsland6Ess.ChannelId.VOLTAGE_SET_POINT_WITH_DEACTIVATED_BATTERY_MENAGEMENT,
								new UnsignedDoublewordElement(40733)), //
						m(SunnyIsland6Ess.ChannelId.CYCLE_TIME_FOR_FULL_CHARGE, new UnsignedDoublewordElement(40735)), //
						m(SunnyIsland6Ess.ChannelId.CYCLE_TIME_FOR_EQUALIZATION_CHARGE,
								new UnsignedDoublewordElement(40737)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_TEMPERATUR_COMPENSATION,
								new UnsignedDoublewordElement(40739)), //
						m(SunnyIsland6Ess.ChannelId.AUTOMATIC_EQUALIZATION_CHARGE,
								new UnsignedDoublewordElement(40741)), //
						m(SunnyIsland6Ess.ChannelId.TYPE_OF_ADDTIONAL_DC_SOURCES, new UnsignedDoublewordElement(40743)), //
						m(SunnyIsland6Ess.ChannelId.LIMITATION_TYPE_OF_GENERATOR_CURRENT,
								new UnsignedDoublewordElement(40745)), //
						m(SunnyIsland6Ess.ChannelId.SENSIVITY_OF_GENERATOR_FAILURE_DETECTION,
								new UnsignedDoublewordElement(40747)), //
						new DummyRegisterElement(40749, 40750), //
						m(SunnyIsland6Ess.ChannelId.INVERTER_NOMINAL_VOLTAGE, new UnsignedDoublewordElement(40751)), //
						m(SunnyIsland6Ess.ChannelId.INVERTER_NOMINAL_FREQUENCY, new UnsignedDoublewordElement(40753)), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_AC_BATTERY_CHARGE_CURRENT,
								new UnsignedDoublewordElement(40755)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_VALUE_SOC_FOR_START_LOAD_SHEDDING_1,
								new UnsignedDoublewordElement(40757)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_VALUE_SOC_FOR_STOP_LOAD_SHEDDING_1,
								new UnsignedDoublewordElement(40759)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_ADDITIONAL_TIME_RANGE_LOAD_SHEDDING_1,
								new UnsignedDoublewordElement(40761)), //
						m(SunnyIsland6Ess.ChannelId.TIME_LOAD_SHEDDING_1, new UnsignedDoublewordElement(40763)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_START_LOAD_SHEDDING_1_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40765)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_STOP_LOAD_SHEDDING_1_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40767)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_VALUE_SOC_FOR_START_LOAD_SHEDDING_2,
								new UnsignedDoublewordElement(40769)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_VALUE_SOC_FOR_STOP_LOAD_SHEDDING_2,
								new UnsignedDoublewordElement(40771)), //
						m(SunnyIsland6Ess.ChannelId.START_TIME_ADDITIONAL_TIME_RANGE_LOAD_SHEDDING_2,
								new UnsignedDoublewordElement(40773)), //
						m(SunnyIsland6Ess.ChannelId.TIME_LOAD_SHEDDING_2, new UnsignedDoublewordElement(40775)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_START_LOAD_SHEDDING_2_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40777)), //
						m(SunnyIsland6Ess.ChannelId.LIMIT_SOC_FOR_STOP_LOAD_SHEDDING_2_IN_ADD_TIME_RANGE,
								new UnsignedDoublewordElement(40779)), //
						new DummyRegisterElement(40781, 40786), //
						m(SunnyIsland6Ess.ChannelId.CLUSTER_BEHAVIOUR_WHEN_A_DEVICE_FAILS,
								new UnsignedDoublewordElement(40787)), //
						new DummyRegisterElement(40789, 40790), //
						m(SunnyIsland6Ess.ChannelId.TIME_OUT_FOR_COMMUNICATION_FAULT_INDICATION,
								new UnsignedDoublewordElement(40791)), //
						new DummyRegisterElement(40793, 40794), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_BATTERY_CHARGING_POWER_CAPACITY,
								new UnsignedDoublewordElement(40795)), //
						new DummyRegisterElement(40797, 40798), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_BATTERY_DISCHARGING_POWER_CAPACITY,
								new UnsignedDoublewordElement(40799)),
						new DummyRegisterElement(40801, 40804), //
						m(SunnyIsland6Ess.ChannelId.ENERGY_SAVING_MODE, new UnsignedDoublewordElement(40805)), //
						new DummyRegisterElement(40807, 40810), //
						m(SunnyIsland6Ess.ChannelId.UPDATE_VERSION_OF_THE_LOGIC_COMPONENT,
								new UnsignedDoublewordElement(40811)), //
						new DummyRegisterElement(40813, 40818), //
						m(SunnyIsland6Ess.ChannelId.FIRMWARE_VERSION_OF_PROTOCOL_CONVERTER,
								new UnsignedDoublewordElement(40819)), //
						m(SunnyIsland6Ess.ChannelId.HARDWARE_VERSION_OF_PROTOCOL_CONVERTER,
								new UnsignedDoublewordElement(40821)), //
						new DummyRegisterElement(40823, 40826), //
						m(SunnyIsland6Ess.ChannelId.SERIAL_NUMBER_OF_THE_PROTOCOL_CONVERTER,
								new UnsignedDoublewordElement(40827))),
				new FC3ReadRegistersTask(40189, Priority.HIGH, //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedDoublewordElement(40189),
								ElementToChannelConverter.INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedDoublewordElement(40191))), //
				new FC16WriteRegistersTask(40236, //
						m(SunnyIsland6Ess.ChannelId.BMS_OPERATING_MODE, new UnsignedDoublewordElement(40236))), //
				new FC16WriteRegistersTask(40149, //
						m(SunnyIsland6Ess.ChannelId.SET_ACTIVE_POWER, new SignedDoublewordElement(40149)), //
						m(SunnyIsland6Ess.ChannelId.SET_CONTROL_MODE, new UnsignedDoublewordElement(40151)), //
						m(SunnyIsland6Ess.ChannelId.SET_REACTIVE_POWER, new SignedDoublewordElement(40153))), //
				new FC16WriteRegistersTask(43090, //
						m(SunnyIsland6Ess.ChannelId.GRID_GUARD_CODE, new UnsignedDoublewordElement(43090))), //
				new FC16WriteRegistersTask(40705,
						m(SunnyIsland6Ess.ChannelId.MIN_SOC_POWER_ON, new UnsignedDoublewordElement(40705)), //
						m(SunnyIsland6Ess.ChannelId.MIN_SOC_POWER_OFF, new UnsignedDoublewordElement(40707)))); //

		return protocol;
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		DEVICE_CLASS(new Doc()), //
		DEVICE_TYPE(new Doc()), //
		SERIAL_NUMBER(new Doc()), //
		SOFTWARE_PACKAGE(new Doc()), //
		WAITING_TIME_UNTIL_FEED_IN(new Doc().unit(Unit.SECONDS)), //
		MESSAGE(new Doc()), //
		SYSTEM_STATE(new Doc().options(SystemState.values())), //
		RECOMMENDED_ACTION(new Doc()), //
		FAULT_CORRECTION_MEASURE(new Doc()), //
		NUMBER_OF_EVENT_FOR_USER(new Doc()), //
		NUMBER_OF_EVENT_FOR_INSTALLER(new Doc()), //
		NUMBER_OF_EVENT_FOR_SERVICE(new Doc()), //
		NUMBER_OF_GENERATORS_STARTS(new Doc()), //
		AMP_HOURS_COUNTER_FOR_BATTERY_CHARGE(new Doc().unit(Unit.AMPERE_HOURS)), //
		AMP_HOURS_COUNTER_FOR_BATTERY_DISCHARGE(new Doc().unit(Unit.AMPERE_HOURS)), //
		METER_READING_CONSUMPTION_METER(new Doc().unit(Unit.WATT_HOURS)), //
		ENERGY_CONSUMED_FROM_GRID(new Doc().unit(Unit.WATT_HOURS)), //
		ENERGY_FED_INTO_GRID(new Doc().unit(Unit.WATT_HOURS)), //
		GRID_REFERENCE_COUNTER_READING(new Doc().unit(Unit.WATT_HOURS)), //
		GRID_FEED_IN_COUNTER_READING(new Doc().unit(Unit.WATT_HOURS)), //
		POWER_OUTAGE(new Doc().unit(Unit.SECONDS)), //
		RISE_IN_SELF_CONSUMPTION(new Doc().unit(Unit.WATT_HOURS)), //
		RISE_IN_SELF_CONSUMPTION_TODAY(new Doc().unit(Unit.WATT_HOURS)), //
		ABSORBED_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		RELEASED_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		NUMBER_OF_GRID_CONNECTIONS(new Doc()), //
		ACTIVE_POWER_L1(new Doc().unit(Unit.WATT)), //
		ACTIVE_POWER_L2(new Doc().unit(Unit.WATT)), //
		ACTIVE_POWER_L3(new Doc().unit(Unit.WATT)), //
		GRID_VOLTAGE_L1(new Doc().unit(Unit.VOLT)), //
		GRID_VOLTAGE_L2(new Doc().unit(Unit.VOLT)), //
		GRID_VOLTAGE_L3(new Doc().unit(Unit.VOLT)), //
		FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		REACTIVE_POWER_L1(new Doc().unit(Unit.VOLT_AMPERE)), //
		REACTIVE_POWER_L2(new Doc().unit(Unit.VOLT_AMPERE)), //
		REACTIVE_POWER_L3(new Doc().unit(Unit.VOLT_AMPERE)), //
		COSPHI_SET_POINT_READ(new Doc()), //
		CURRENT_BATTERY_CAPACITY(new Doc().unit(Unit.PERCENT)), //
		ACTIVE_BATTERY_CHARGING_MODE(new Doc()//
				.options(ActiveBatteryChargingMode.values())), //
		CURRENT_BATTERY_CHARGING_SET_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		NUMBER_OF_BATTERY_CHARGE_THROUGHPUTS(new Doc()), //
		BATTERY_MAINT_SOC(new Doc()), //
		LOAD_POWER(new Doc().unit(Unit.WATT)), //
		POWER_GRID_REFERENCE(new Doc().unit(Unit.WATT)), //
		POWER_GRID_FEED_IN(new Doc().unit(Unit.WATT)), //
		PV_POWER_GENERATED(new Doc().unit(Unit.WATT)), //
		CURRENT_SELF_CONSUMPTION(new Doc().unit(Unit.WATT)), //
		CURRENT_RISE_IN_SELF_CONSUMPTION(new Doc().unit(Unit.WATT)), //
		MULTIFUNCTION_RELAY_STATUS(new Doc()//
				.options(MultifunctionRelayStatus.values())), //
		POWER_SUPPLY_STATUS(new Doc()//
				.options(PowerSupplyStatus.values())), //
		REASON_FOR_GENERATOR_REQUEST(new Doc()//
				.options(ReasonForGeneratorRequest.values())), //
		PV_MAINS_CONNECTION(new Doc()//
				.options(PvMainsConnection.values())), //
		STATUS_OF_UTILITY_GRID(new Doc()//
				.options(StatusOfUtilityGrid.values())), //
		GRID_FREQ_OF_EXTERNAL_POWER_CONNECTION(new Doc().unit(Unit.HERTZ)), //
		VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_A(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_B(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_C(new Doc().unit(Unit.VOLT)), //
		CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_A(new Doc().unit(Unit.AMPERE)), //
		CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_B(new Doc().unit(Unit.AMPERE)), //
		CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_C(new Doc().unit(Unit.AMPERE)), //
		GENERATOR_STATUS(new Doc()//
				.options(GeneratorStatus.values())), //
		DATA_TRANSFER_RATE_OF_NETWORK_TERMINAL_A(new Doc()//
				.options(DataTransferRateOfNetworkTerminalA.values())), //
		DUPLEX_MODE_OF_NETWORK_TERMINAL_A(new Doc()//
				.options(DuplexModeOfNetworkTerminalA.values())), //
		SPEED_WIRE_CONNECTION_STATUS_OF_NETWORK_TERMINAL_A(new Doc()//
				.options(SpeedWireConnectionStatusOfNetworkTerminalA.values())), //
		GRID_CURRENT_L1(new Doc().unit(Unit.AMPERE)), //
		GRID_CURRENT_L2(new Doc().unit(Unit.AMPERE)), //
		GRID_CURRENT_L3(new Doc().unit(Unit.AMPERE)), //
		OUTPUT_OF_PHOTOVOLTAICS(new Doc()), //
		TOTAL_CURRENT_EXTERNAL_GRID_CONNECTION(new Doc().unit(Unit.AMPERE)), //
		FAULT_BATTERY_SOC(new Doc().unit(Unit.PERCENT)), //
		MAXIMUM_BATTERY_CURRENT_IN_CHARGE_DIRECTION(new Doc().unit(Unit.AMPERE)), //
		MAXIMUM_BATTERY_CURRENT_IN_DISCHARGE_DIRECTION(new Doc().unit(Unit.AMPERE)), //
		CHARGE_FACTOR_RATIO_OF_BATTERY_CHARGE_DISCHARGE(new Doc()), //
		OPERATING_TIME_OF_BATTERY_STATISTICS_COUNTER(new Doc().unit(Unit.SECONDS)), //
		LOWEST_MEASURED_BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		HIGHEST_MEASURED_BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		MAX_OCCURRED_BATTERY_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		REMAINING_TIME_UNTIL_FULL_CHARGE(new Doc().unit(Unit.SECONDS)), //
		REMAINING_TIME_UNTIL_EQUALIZATION_CHARGE(new Doc().unit(Unit.SECONDS)), //
		REMAINING_ABSORPTION_TIME(new Doc().unit(Unit.SECONDS)), //
		LOWER_DISCHARGE_LIMIT_FOR_SELF_CONSUMPTION_RANGE(new Doc().unit(Unit.PERCENT)), //
		TOTAL_OUTPUT_CURRENT_OF_SOLAR_CHARGER(new Doc().unit(Unit.AMPERE)), //
		REMAINING_MIN_OPERATING_TIME_OF_GENERATOR(new Doc().unit(Unit.SECONDS)), //
		OPERATING_STATUS_MASTER_L1(new Doc()), //
		STATUS_BATTERY_APPLICATION_AREA(new Doc()//
				.options(StatusBatteryApplicationArea.values())), //
		ABSORPTION_PHASE_ACTIVE(new Doc()//
				.options(AbsorptionPhaseActive.values())), //
		CONTROL_OF_BATTERY_CHARGING_VIA_COMMUNICATION_AVAILABLE(new Doc()//
				.options(ControlOfBatteryChargingViaCommunicationAvailable.values())), //
		TOTAL_ENERGY_PHOTOVOLTAICS(new Doc().unit(Unit.KILOWATT_HOURS)), //
		TOTAL_ENERGY_PHOTOVOLTAICS_CURRENT_DAY(new Doc().unit(Unit.WATT_HOURS)), //
		NUMBER_OF_EQALIZATION_CHARGES(new Doc().unit(Unit.KILOWATT_HOURS)), //
		NUMBER_OF_FULL_CHARGES(new Doc()), //
		RELATIVE_BATTERY_DISCHARGING_SINCE_THE_LAST_FULL_CHARGE(new Doc().unit(Unit.PERCENT)), //
		RELATIVE_BATTERY_DISCHARGING_SINCE_LAST_EQUALIZATION_CHARGE(new Doc().unit(Unit.PERCENT)), //
		OPERATING_TIME_ENERGY_COUNT(new Doc().unit(Unit.SECONDS)), //
		PHOTOVOLTAIC_ENERGY_IN_SOLAR_CHARGER(new Doc().unit(Unit.WATT_HOURS)), //
		BATTERY_CHARGING_SOC(new Doc().unit(Unit.WATT)), //
		BATTERY_DISCHARGING_SOC(new Doc().unit(Unit.WATT)), //
		OUTPUT_EXTERNAL_POWER_CONNECTION(new Doc().unit(Unit.WATT)), //
		OUTPUT_EXTERNAL_POWER_CONNECTION_L1(new Doc().unit(Unit.WATT)), //
		OUTPUT_EXTERNAL_POWER_CONNECTION_L2(new Doc().unit(Unit.WATT)), //
		OUTPUT_EXTERNAL_POWER_CONNECTION_L3(new Doc().unit(Unit.WATT)), //
		REACTIVE_POWER_EXTERNAL_POWER_CONNECTION(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWER_EXTERNAL_POWER_CONNECTION_L1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWER_EXTERNAL_POWER_CONNECTION_L2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWER_EXTERNAL_POWER_CONNECTION_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		STATUS_DIGITAL_INPUT(new Doc()//
				.options(StatusDigitalInput.values())), //
		RATED_BATTERY_CAPACITY(new Doc()), //
		MAX_BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_TYPE(new Doc()//
				.options(BatteryType.values())), //
		RATED_BATTERY_VOLTAGE(new Doc()), //
		BATTERY_BOOST_CHARGE_TIME(new Doc().unit(Unit.MINUTE)), //
		BATTERY_EQUALIZATION_CHARGE_TIME(new Doc().unit(Unit.HOUR)), //
		BATTERY_FULL_CHARGE_TIME(new Doc().unit(Unit.HOUR)), //
		MAX_BATTERY_CHARGING_CURRENT(new Doc().unit(Unit.AMPERE)), //
		RATED_GENERATOR_CURRENT(new Doc().unit(Unit.AMPERE)), //
		AUTOMATIC_GENERATOR_START(new Doc()//
				.options(AutomaticGeneratorStart.values())), //
		MANUAL_GENERATOR_CONTROL(new Doc()//
				.options(ManualGeneratorStart.values())), //
		GENERATOR_REQUEST_VIA_POWER_ON(new Doc()), //
		GENERATOR_SHUT_DOWN_LOAD_LIMIT(new Doc().unit(Unit.WATT)), //
		GENERATOR_START_UP_LOAD_LIMIT(new Doc().unit(Unit.WATT)), //
		FIRMWARE_VERSION_OF_THE_MAIN_PROCESSOR(new Doc()), //
		FIRMWARE_VERSION_OF_THE_LOGIC_COMPONENET(new Doc()), //
		GRID_CREATING_GENERATOR(new Doc()//
				.options(GridCreatingGenerator.values())), //
		RISE_IN_SELF_CONSUMPTION_SWITCHED_ON(new Doc()//
				.options(RiseInSelfConsumptionSwitchedOn.values())), //
		INITIATE_DEVICE_RESTART(new Doc()), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_BOOST_CHARGE(new Doc().unit(Unit.VOLT)), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_FULL_CHARGE(new Doc().unit(Unit.VOLT)), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_EQUALIZATION_CHARGE(new Doc().unit(Unit.VOLT)), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_FLOAT_CHARGE(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_MONITORING_UPPER_MINIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_MONITORING_UPPER_MAXIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_MONITORING_HYSTERESIS_MINIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_MONITORING_HYSTERESIS_MAXIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		FREQUENCY_MONITORING_UPPER_MINIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		FREQUENCY_MONITORING_UPPER_MAXIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		FREQUENCY_MONITORING_HYSTERESIS_MINIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		FREQUENCY_MONITORING_HYSTERESIS_MAXIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		VOLTAGE_MONITORING_GENERATOR_MINIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_MONITORING_GENERATOR_MAXIMUM_THRESHOLD(new Doc().unit(Unit.VOLT)), //
		FREQUENCY_MONITORING_GENERATOR_MINIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		FREQUENCY_MONITORING_GENERATOR_MAXIMUM_THRESHOLD(new Doc().unit(Unit.HERTZ)), //
		VOLTAGE_MONITORING_GENERATOR_MAXIMUM_REVERSE_POWER(new Doc()), //
		VOLTAGE_MONITORING_GENERATOR_MAXIMUM_REVERSE_POWER_TRIPPING_TIME(new Doc().unit(Unit.SECONDS)), //
		NOMINAL_FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		ACKNOWLEGDE_GENERATOR_ERRORS(new Doc()//
				.options(AcknowledgeGeneratorErrors.values())), //
		BATTERY_NOMINAL_CAPACITY(new Doc()), //
		OPERATING_MODE_OF_ACTIVE_POWER_LIMITATION_AT_OVERFREQUENCY(new Doc()//
				.options(OperatingModeOfActivePowerLimitationAtOverFrequency.values())), //
		DIFFERENCE_BETWEEN_STARTING_FREQ_AND_GRID_FREQ(new Doc().unit(Unit.HERTZ)), //
		DIFFERENCE_BETWEEN_RESET_FREQ_AND_GRID_FREQ(new Doc().unit(Unit.HERTZ)), //
		COSPHI_AT_STARTING_POINT(new Doc().unit(Unit.HERTZ)), //
		CONFIGURATION_OF_THE_COSPHI_STARTING_POINT(new Doc().unit(Unit.HERTZ)//
				.options(ConfigurationOfTheCosphiStartingPoint.values())), //
		COSPHI_AT_THE_END_POINT(new Doc().unit(Unit.HERTZ)), //
		CONFIGURATION_OF_THE_COSPHI_END_POINT(new Doc().unit(Unit.HERTZ)//
				.options(ConfigurationOfTheCosphiEndPoint.values())), //
		ACTIVE_POWER_AT_STARTING_POINT(new Doc().unit(Unit.PERCENT)), //
		ACTIVE_POWER_AT_END_POINT(new Doc().unit(Unit.PERCENT)), //
		BMS_OPERATING_MODE(new Doc()//
				.options(BMSOperatingMode.values())), //
		ACTIVE_POWER_GRADIENT_CONFIGURATION(new Doc().unit(Unit.PERCENT)), //
		GRID_REQUEST_VIA_POWER_SWITCH_ON(new Doc()//
				.options(GridRequestViPowerSwitchOn.values())), //
		GRID_REQUEST_SWITCH_ON_POWER_LIMIT(new Doc().unit(Unit.WATT)), //
		GRID_REQUEST_SWITCH_OFF_POWER_LIMIT(new Doc().unit(Unit.WATT)), //
		MANUAL_CONTROL_OF_NETWORK_CONNECTION(new Doc()//
				.options(ManualControlOfNetworkConnection.values())), //
		GRID_REQUEST_VIA_CHARGE_TYPE(new Doc()//
				.options(GridRequestViaChargeType.values())), //
		TYPE_OF_AC_SUBDISTRIBUTION(new Doc()//
				.options(TypeOfACSubdistribution.values())), //
		MANUAL_EQUAIZATION_CHARGE(new Doc()//
				.options(ManualEqualizationCharge.values())), //
		GENERATOR_REQUEST(new Doc()), //
		LIMIT_SOC_GENERATOR_START_IN_TIME_RANGE(new Doc().unit(Unit.PERCENT)), //
		LIMIT_SOC_GENERATOR_SHUTDOWN_IN_TIME_RANGE(new Doc().unit(Unit.PERCENT)), //
		START_TIME_ADDTIONAL_TIME_RANGE_GENERATOR_REQUEST(new Doc()), //
		START_TIME_RANGE_FOR_GENERATOR_REQUEST(new Doc()), //
		LIMIT_SOC_GENERATOR_STOP_ADD_IN_TIME_RANGE(new Doc().unit(Unit.PERCENT)), //
		LIMIT_SOC_GENERATOR_START_ADD_IN_TIME_RANGE(new Doc().unit(Unit.PERCENT)), //
		TIME_CONTROLLED_GENERATOR_OPERATION(new Doc()//
				.options(TimeControlledGeneratorOperation.values())), //
		START_TIME_FOR_TIME_CONTROLLED_GENERATOR_OPERATION(new Doc()), //
		OPERATING_TIME_FOR_TIME_CONTROLLED_GENERATOR_OPERATION(new Doc()), //
		REPETITION_CYCLE_OF_TIME_CONTROLLED_GENERATOR_OPERATION(new Doc()//
				.options(RepetitionCycleOfTheTimeControlledGeneratorOperation.values())), //
		GENERATOR_REQUEST_WITH_SET_CHARGE_TYPE(new Doc()), //
		REACTION_TO_DIGITAL_INPUT_OF_GENERATOR_REQUEST(new Doc()), //
		AVERAGE_TIME_FOR_GENERATOR_REQUEST_VIA_POWER(new Doc().unit(Unit.SECONDS)), //
		AVERAGE_OPERATING_TIME_OF_GENERATOR(new Doc().unit(Unit.SECONDS)), //
		AVERAGE_IDLE_PERIOD_OF_GENERATOR(new Doc().unit(Unit.SECONDS)), //
		COOLING_DOWN_TIME_OF_GENERATOR(new Doc().unit(Unit.SECONDS)), //
		IDLE_PERIOD_AFTER_GENERATOR_FAULT(new Doc().unit(Unit.SECONDS)), //
		WARM_UP_TIME_OF_GENERATOR(new Doc().unit(Unit.SECONDS)), //
		GENERATOR_NOMINAL_FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		TIME_CONTROLLED_INVERTER_OPERATION(new Doc()//
				.options(TimeControlledInverterOperation.values())), //
		START_TIME_FOR_TIME_CONTROLLED_INVERTER(new Doc()), //
		OPERATING_TIME_FOR_TIME_CONTROLLED_INVERTER(new Doc().unit(Unit.SECONDS)), //
		REPETITION_CYCLE_OF_TIME_CONTROLLED_INVERTER(new Doc()//
				.options(RepetitionCycleOfTheControlledInverter.values())), //
		DEVICE_NAME(new Doc()), //
		AUTOMATIC_UPDATES_ACTIVATED(new Doc()), //
		TIME_OF_THE_AUTOMATIC_UPDATE(new Doc()), //
		GRID_GUARD_VERSION(new Doc()), //
		MEMORY_CARD_STATUS(new Doc()//
				.options(MemoryCardStatus.values())), //
		UPDATE_VERSION_OF_THE_MAIN_PROCESSOR(new Doc()), //
		START_FEED_IN_PV(new Doc()), //
		STOP_FEED_IN_PV(new Doc()), //
		CUT_OFF_TIME_UNTIL_CONNECTION_TO_EXTERNAL_NETWORK(new Doc()), //
		AUTOMATIC_FREQUENCY_SYNCHRONIZATION(new Doc()//
				.options(AutomaticFrequencySynchronization.values())), //
		MAXIUMUM_CURRENT_FROM_PUBLIC_GRID(new Doc()), //
		POWER_FEEDBACK_TO_PUBLIC_GRID_ALLOWED(new Doc()//
				.options(PowerFeedbackToPublicGridAllowed.values())), //
		GRID_REQUEST_VIA_SOC_SWITCHED_ON(new Doc()), //
		LIMIT_SOC_FOR_CONNECTION_TO_GRID(new Doc()), //
		LIMIT_SOC_FOR_DISCONNECTION_FROM_GRID(new Doc()), //
		START_TIME_ADDTIONAL_TIME_RANGE_GRID_REQUEST(new Doc()), //
		START_INTERVAL_GRID_REQUEST(new Doc()), //
		LIMIT_SOC_FOR_CONNECT_TO_GRID_IN_ADD_TIME_RANGE(new Doc()), //
		LIMIT_SOC_FOR_DISCONNECT_FROM_GRID_IN_ADD_TIME_RANGE(new Doc()), //
		ENERGY_SAVING_MODE_SWITCH_ON(new Doc()), //
		MAXIMUM_GRID_REVERSE_POWER(new Doc()), //
		MAXIMUM_GRID_REVERSE_POWER_TRIPPING_TIME(new Doc()), //
		TIME_UNTIL_CHANGE_OVER_TO_ENERGY_SAVING_MODE(new Doc()), //
		MAXIMUM_DURATION_OF_ENERGY_SAVING_MODE(new Doc()), //
		START_TIME_OF_BATTERY_PROTECTION_MODE_LEVEL(new Doc()), //
		END_TIME_OF_BATTERY_PROTECTION_MODE_LEVEL(new Doc()), //
		BATTERY_SOC_FOR_PROTECTION_MODE(new Doc()), //
		BATTERY_SWITCH_ONLIMIT_AFTER_OVER_TEMP_SHUT_DOWN(new Doc()), //
		OUTPUT_RESISTANCE_OF_BATTERY_CONNECTION(new Doc()), //
		LOWER_LIMIT_DEEP_DISCHARGE_PROTECT_AREA_PRIOR_SHUTDOWN(new Doc()), //
		MINIMUM_WIDTH_OF_DEEP_DISCHARGE_PROTECTION_AREA(new Doc()), //
		MINIMUM_WIDTH_OF_BAKCUP_POWER_AREA(new Doc()), //
		AREA_WIDTH_FOR_CONSERVING_SOC(new Doc()), //
		MINIMUM_WIDTH_OF_OWN_CONSUMPTION_AREA(new Doc()), //
		MOST_PRODUCTIVE_MONTH_FOR_BATTERY_USAGE_RANGE(new Doc()), //
		SEASON_OPERATION_ACTIVE(new Doc()), //
		VOLTAGE_SET_POINT_WITH_DEACTIVATED_BATTERY_MENAGEMENT(new Doc()), //
		CYCLE_TIME_FOR_FULL_CHARGE(new Doc()), //
		CYCLE_TIME_FOR_EQUALIZATION_CHARGE(new Doc()), //
		BATTERY_TEMPERATUR_COMPENSATION(new Doc()), //
		AUTOMATIC_EQUALIZATION_CHARGE(new Doc()), //
		TYPE_OF_ADDTIONAL_DC_SOURCES(new Doc()), //
		LIMITATION_TYPE_OF_GENERATOR_CURRENT(new Doc()), //
		SENSIVITY_OF_GENERATOR_FAILURE_DETECTION(new Doc()), //
		INVERTER_NOMINAL_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		INVERTER_NOMINAL_FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		MAXIMUM_AC_BATTERY_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), //
		LIMIT_VALUE_SOC_FOR_START_LOAD_SHEDDING_1(new Doc().unit(Unit.PERCENT)), //
		LIMIT_VALUE_SOC_FOR_STOP_LOAD_SHEDDING_1(new Doc().unit(Unit.PERCENT)), //
		START_TIME_ADDITIONAL_TIME_RANGE_LOAD_SHEDDING_1(new Doc()), //
		TIME_LOAD_SHEDDING_1(new Doc()), //
		LIMIT_SOC_FOR_START_LOAD_SHEDDING_1_IN_ADD_TIME_RANGE(new Doc()), //
		LIMIT_SOC_FOR_STOP_LOAD_SHEDDING_1_IN_ADD_TIME_RANGE(new Doc()), //
		LIMIT_VALUE_SOC_FOR_START_LOAD_SHEDDING_2(new Doc().unit(Unit.PERCENT)), //
		LIMIT_VALUE_SOC_FOR_STOP_LOAD_SHEDDING_2(new Doc().unit(Unit.PERCENT)), //
		START_TIME_ADDITIONAL_TIME_RANGE_LOAD_SHEDDING_2(new Doc()), //
		TIME_LOAD_SHEDDING_2(new Doc()), //
		LIMIT_SOC_FOR_START_LOAD_SHEDDING_2_IN_ADD_TIME_RANGE(new Doc()), //
		LIMIT_SOC_FOR_STOP_LOAD_SHEDDING_2_IN_ADD_TIME_RANGE(new Doc()), //
		CLUSTER_BEHAVIOUR_WHEN_A_DEVICE_FAILS(new Doc()), //
		COMMUNICATION_VERSION(new Doc()), //
		TIME_OUT_FOR_COMMUNICATION_FAULT_INDICATION(new Doc()), //
		ENERGY_SAVING_MODE(new Doc()), //
		UPDATE_VERSION_OF_THE_LOGIC_COMPONENT(new Doc()), //
		FIRMWARE_VERSION_OF_PROTOCOL_CONVERTER(new Doc()), //
		HARDWARE_VERSION_OF_PROTOCOL_CONVERTER(new Doc()), //
		SERIAL_NUMBER_OF_THE_PROTOCOL_CONVERTER(new Doc()), //

		BATTERY_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		DEBUG_SET_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT) //
//				 on each setNextWrite to the channel -> store the value in the DEBUG-channel
				.onInit(channel -> { //
					((IntegerWriteChannel) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_SET_ACTIVE_POWER).setNextValue(value);
					});
				})), //
		SET_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE)), //
		MIN_SOC_POWER_ON(new Doc()), //
		GRID_GUARD_CODE(new Doc()), //
		MIN_SOC_POWER_OFF(new Doc()), //
		SET_CONTROL_MODE(new Doc()//
				.options(SetControlMode.values())), //
		METER_SETTING(new Doc()//
				.options(MeterSetting.values())), //
		OPERATING_MODE_FOR_ACTIVE_POWER_LIMITATION(new Doc()//
				.options(OperatingModeForActivePowerLimitation.values())), //
		OPERATING_MODE_FOR_REACTIVE_POWER(new Doc()), //
		MAXIMUM_BATTERY_CHARGING_POWER_CAPACITY(new Doc()), //
		MAXIMUM_BATTERY_DISCHARGING_POWER_CAPACITY(new Doc()), //
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
		return "";
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
