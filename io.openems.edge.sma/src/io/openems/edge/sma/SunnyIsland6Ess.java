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
		name = "Ess.SMA.SunnyIsland6.0H-11",
		// TODO naming "Ess.SMA...."
		immediate = true, //
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

	@Override
	public void applyPower(int activePower, int reactivePower) {
		IntegerWriteChannel setControlMode = this.channel(ChannelId.SET_CONTROL_MODE);
		IntegerWriteChannel setActivePowerChannel = this.channel(ChannelId.SET_ACTIVE_POWER);
		IntegerWriteChannel setReactivePowerChannel = this.channel(ChannelId.SET_REACTIVE_POWER);
		try {
			setControlMode.setNextWriteValue(802);
			setActivePowerChannel.setNextWriteValue(activePower);
			setReactivePowerChannel.setNextWriteValue(reactivePower);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
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
						m(SunnyIsland6Ess.ChannelId.GRID_VOLTAGE_L1, new SignedDoublewordElement(30783)), //
						m(SunnyIsland6Ess.ChannelId.GRID_VOLTAGE_L2, new SignedDoublewordElement(30785)), //
						m(SunnyIsland6Ess.ChannelId.GRID_VOLTAGE_L3, new SignedDoublewordElement(30787)), //
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
						m(SunnyIsland6Ess.ChannelId.OPERATING_MODE_FOR_ACTIVE_POWER,
								new UnsignedDoublewordElement(30835))), //
				new FC3ReadRegistersTask(30845, Priority.HIGH, //
						m(SymmetricEss.ChannelId.SOC, new UnsignedDoublewordElement(30845)), //
						m(SunnyIsland6Ess.ChannelId.CURRENT_BATTERY_CAPACITY, new SignedDoublewordElement(30847)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_TEMPERATURE, new SignedDoublewordElement(30849)), //
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
								new UnsignedDoublewordElement(30995)), //
						m(SunnyIsland6Ess.ChannelId.LOWEST_MEASURED_BATTERY_TEMPERATURE,
								new SignedDoublewordElement(30997)), //
						m(SunnyIsland6Ess.ChannelId.HIGHEST_MEASURED_BATTERY_TEMPERATURE,
								new SignedDoublewordElement(30999)), //
						m(SunnyIsland6Ess.ChannelId.MAX_OCCURRED_BATTERY_VOLTAGE, new UnsignedDoublewordElement(31001)), //
						m(SunnyIsland6Ess.ChannelId.REMAINING_TIME_UNTIL_FULL_CHARGE,
								new UnsignedDoublewordElement(31003)), //
						m(SunnyIsland6Ess.ChannelId.REMAINING_TIME_UNTIL_EQUALIZATION_CHARGE,
								new UnsignedDoublewordElement(31005)), //
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
						m(SunnyIsland6Ess.ChannelId.CONTROL_OF_BATTERY_CHARGING_VIA_COMMUNICATION_AVAIULABLE,
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
								new UnsignedDoublewordElement(40107))), //

				new FC16WriteRegistersTask(40033, //
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
								new UnsignedDoublewordElement(40107))//

				// TODO .------------------------------------
				), //
				new FC3ReadRegistersTask(40189, Priority.HIGH, //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedDoublewordElement(40189),
								ElementToChannelConverter.INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedDoublewordElement(40191))), //
				new FC16WriteRegistersTask(40236, //
						m(SunnyIsland6Ess.ChannelId.BMS_OPERATING_MODE, new UnsignedDoublewordElement(40236))), //
				new FC16WriteRegistersTask(40149, //
						m(SunnyIsland6Ess.ChannelId.SET_ACTIVE_POWER, new SignedDoublewordElement(40149))), //
				new FC16WriteRegistersTask(40151,
						m(SunnyIsland6Ess.ChannelId.SET_CONTROL_MODE, new UnsignedDoublewordElement(40151)), //
						m(SunnyIsland6Ess.ChannelId.SET_REACTIVE_POWER, new SignedDoublewordElement(40153))), //
				new FC16WriteRegistersTask(43090, //
						m(SunnyIsland6Ess.ChannelId.GRID_GUARD_CODE, new UnsignedDoublewordElement(43090))), //
				new FC16WriteRegistersTask(40705,
						m(SunnyIsland6Ess.ChannelId.MIN_SOC_POWER_ON, new UnsignedDoublewordElement(40705)), //
						m(SunnyIsland6Ess.ChannelId.MIN_SOC_POWER_OFF, new UnsignedDoublewordElement(40707))), //
				new FC3ReadRegistersTask(40795, Priority.LOW, //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_BATTERY_CHARGING_POWER,
								new UnsignedDoublewordElement(40795)), //
						new DummyRegisterElement(40797, 40798), //
						m(SunnyIsland6Ess.ChannelId.MAXIMUM_BATTERY_DISCHARGING_POWER,
								new UnsignedDoublewordElement(40799))),
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
		WAITING_TIME_UNTIL_FEED_IN(new Doc().unit(Unit.SECONDS)), //
		MESSAGE(new Doc()), //
		SYSTEM_STATE(new Doc()//
				.option(35, "Fehler")//
				.option(303, "Aus")//
				.option(307, "OK")//
				.option(455, "Warnung")), //
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
		ACTIVE_BATTERY_CHARGING_MODE(new Doc()), //
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
				.option(51, "Closed")//
				.option(311, "Open")), //
		POWER_SUPPLY_STATUS(new Doc()//
				.option(303, "Off")//
				.option(1461, "Utility Grid Connected")//
				.option(1462, "Backup Not Available")//
				.option(1463, "Backup")), //
		REASON_FOR_GENERATOR_REQUEST(new Doc()//
				.option(1773, "No Request")//
				.option(1774, "Load")//
				.option(1775, "Time Control")//
				.option(1776, "Manual One Hour")//
				.option(1777, "Manual Start")//
				.option(1778, "External Source")), //
		PV_MAINS_CONNECTION(new Doc()//
				.option(1779, "Disconnected")//
				.option(1780, "Utility Grid")//
				.option(1781, "Stand-Alone Grid")), //
		STATUS_OF_UTILITY_GRID(new Doc()//
				.option(303, "Off")//
				.option(1394, "Waiting For Valid AC Utility Grid")//
				.option(1461, "Utility Grid Connection")//
				.option(1466, "Waiting")//
				.option(1787, "Initialization")//
				.option(2183, "Grid Operation Without Feed-Back")//
				.option(2184, "Energy Saving In The Utility Grid")//
				.option(2185, "End Energy Saving In The Utility Grid")//
				.option(2186, "Start Energy Saving In The Utility Grid")), //
		GRID_FREQ_OF_EXTERNAL_POWER_CONNECTION(new Doc().unit(Unit.HERTZ)), //
		VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_A(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_B(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_EXTERNAL_POWER_CONNECTION_PHASE_C(new Doc().unit(Unit.VOLT)), //
		CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_A(new Doc().unit(Unit.AMPERE)), //
		CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_B(new Doc().unit(Unit.AMPERE)), //
		CURRENT_EXTERNAL_POWER_CONNECTION_PHASE_C(new Doc().unit(Unit.AMPERE)), //
		GENERATOR_STATUS(new Doc()//
				.option(303, "Off")//
				.option(1392, "Error")//
				.option(1787, "Initialization")//
				.option(1788, "Ready")//
				.option(1789, "Warm-Up")//
				.option(1790, "Synchronize").option(1791, "Activated")//
				.option(1792, "Re-Synchronize")//
				.option(1793, "Generator Seperation")//
				.option(1794, "Shut-Off Delay")//
				.option(1795, "Blocked")//
				.option(1796, "Blocked After Error")), //
		DATA_TRANSFER_RATE_OF_NETWORK_TERMINAL_A(new Doc()//
				.option(1720, "10 MBit")//
				.option(1721, "100 MBit")//
				.option(1725, "Not Connected")), //
		DUPLEX_MODE_OF_NETWORK_TERMINAL_A(new Doc()//
				.option(1725, "Not Connected")//
				.option(1726, "Half Duplex")//
				.option(1727, "Full Duplex")), //
		SPEED_WIRE_CONNECTION_STATUS_OF_NETWORK_TERMINAL_A(new Doc()//
				.option(35, "Alarm")//
				.option(307, "OK")//
				.option(455, "Warning")//
				.option(1725, "Not Connected")), //
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
				.option(2614, "Self-Consumption Range")//
				.option(2615, "Conversation Range of State of Charge")//
				.option(2616, "Backup Power Supply Range")//
				.option(2617, "Depp-Discharge Protection Range")//
				.option(2618, "Deep-Discharge Range")), //
		ABSORPTION_PHASE_ACTIVE(new Doc()//
				.option(1129, "Yes")//
				.option(1130, "No")), //
		CONTROL_OF_BATTERY_CHARGING_VIA_COMMUNICATION_AVAIULABLE(new Doc()//
				.option(1129, "Yes")//
				.option(1130, "No")), //
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
				.option(303, "Off")//
				.option(308, "On")), //
		RATED_BATTERY_CAPACITY(new Doc()), //
		MAX_BATTERY_TEMPERATURE(new Doc()), //
		BATTERY_TYPE(new Doc()//
				.option(1782, "Valve-Regulated Lead-Acid Battery (VRLA)")//
				.option(1783, "Flooded Lead-Acid Battery (FLA)")//
				.option(1784, "Nickel/Cadmium (NiCd)")//
				.option(1785, "Lithium-Ion (Li-Ion)")), //
		RATED_BATTERY_VOLTAGE(new Doc()), //
		BATTERY_BOOST_CHARGE_TIME(new Doc()), //
		BATTERY_EQUALIZATION_CHARGE_TIME(new Doc()), //
		BATTERY_FULL_CHARGE_TIME(new Doc()), //
		MAX_BATTERY_CHARGING_CURRENT(new Doc().unit(Unit.AMPERE)), //
		RATED_GENERATOR_CURRENT(new Doc()), //
		AUTOMATIC_GENERATOR_START(new Doc()//
				.option(1129, "Yes")//
				.option(1130, "No")), //
		MANUAL_GENERATOR_CONTROL(new Doc()//
				.option(381, "Stop")//
				.option(1467, "Start")), //
		GENERATOR_REQUEST_VIA_POWER_ON(new Doc()), //
		GENERATOR_SHUT_DOWN_LOAD_LIMIT(new Doc()), //
		GENERATOR_START_UP_LOAD_LIMIT(new Doc()), //
		FIRMWARE_VERSION_OF_THE_MAIN_PROCESSOR(new Doc()), //
		FIRMWARE_VERSION_OF_THE_LOGIC_COMPONENET(new Doc()), //
		GRID_CREATING_GENERATOR(new Doc()//
				.option(1799, "None")//
				.option(1801, "Utility-Grid")//
				.option(1802, "Utility Grid and Generator")//
				.option(1803, "Invalid Configuration for the PV Production Meter")), //
		RISE_IN_SELF_CONSUMPTION_SWITCHED_ON(new Doc()//
				.option(1129, "Yes")//
				.option(1130, "No")), //
		INITIATE_DEVICE_RESTART(new Doc()), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_BOOST_CHARGE(new Doc()), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_FULL_CHARGE(new Doc()), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_EQUALIZATION_CHARGE(new Doc()), //
		CELL_CHARGE_NOMINAL_VOLTAGE_FOR_FLOAT_CHARGE(new Doc()), //
		VOLTAGE_MONITORING_UPPER_MINIMUM_THRESHOLD(new Doc()), //
		VOLTAGE_MONITORING_UPPER_MAXIMUM_THRESHOLD(new Doc()), //
		VOLTAGE_MONITORING_HYSTERESIS_MINIMUM_THRESHOLD(new Doc()), //
		VOLTAGE_MONITORING_HYSTERESIS_MAXIMUM_THRESHOLD(new Doc()), //
		FREQUENCY_MONITORING_UPPER_MINIMUM_THRESHOLD(new Doc()), //
		FREQUENCY_MONITORING_UPPER_MAXIMUM_THRESHOLD(new Doc()), //
		FREQUENCY_MONITORING_HYSTERESIS_MINIMUM_THRESHOLD(new Doc()), //
		FREQUENCY_MONITORING_HYSTERESIS_MAXIMUM_THRESHOLD(new Doc()), //

		BATTERY_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE)), //
		MIN_SOC_POWER_ON(new Doc()), //
		GRID_GUARD_CODE(new Doc()), //
		MIN_SOC_POWER_OFF(new Doc()), //
		SET_CONTROL_MODE(new Doc()//
				.option(802, "START")//
				.option(803, "STOP")), //
		METER_SETTING(new Doc()//
				.option(3053, "SMA Energy Meter")//
				.option(3547, "Wechselrichter")), //
		OPERATING_MODE_FOR_ACTIVE_POWER(new Doc()), //
		OPERATING_MODE_FOR_REACTIVE_POWER(new Doc()), //
		MAXIMUM_BATTERY_CHARGING_POWER(new Doc()), //
		MAXIMUM_BATTERY_DISCHARGING_POWER(new Doc()), //
		BMS_OPERATING_MODE(new Doc()//
				.option(303, "Off")//
				.option(308, "On")//
				.option(2289, "Battery Charging")//
				.option(2290, "Battery Discharging")//
				.option(2424, "Default Setting")), //
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
