package io.openems.edge.meter.siemens.pac1600;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverters;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Siemens.PAC1600", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterSiemensPac1600Impl extends AbstractOpenemsModbusComponent
		implements MeterSiemensPac1600, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.GRID;
	private boolean invert;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSiemensPac1600Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterSiemensPac1600.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumCurrentFromPhases(this);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(1),
								ElementToChannelConverters.SCALE_FACTOR_1),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(3),
								ElementToChannelConverters.SCALE_FACTOR_1),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(5),
								ElementToChannelConverters.SCALE_FACTOR_1),

						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(7),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_1),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(9),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_1),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(11),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_1),

						m(MeterSiemensPac1600.ChannelId.VOLTAGE_L1L2, new UnsignedDoublewordElement(13),
								ElementToChannelConverters.SCALE_FACTOR_1),
						m(MeterSiemensPac1600.ChannelId.VOLTAGE_L2L3, new UnsignedDoublewordElement(15),
								ElementToChannelConverters.SCALE_FACTOR_1),
						m(MeterSiemensPac1600.ChannelId.VOLTAGE_L3L1, new UnsignedDoublewordElement(17),
								ElementToChannelConverters.SCALE_FACTOR_1),

						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(19),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_2_AND_INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(21),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_2_AND_INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(23),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_2_AND_INVERT_IF_TRUE(this.invert)),

						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(25),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_2_AND_INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(27),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_2_AND_INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(29),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_2_AND_INVERT_IF_TRUE(this.invert))),

				new FC3ReadRegistersTask(49, Priority.HIGH, // DummyRegisterElement didn`t work here
						m(ElectricityMeter.ChannelId.FREQUENCY, new SignedDoublewordElement(49),
								ElementToChannelConverters.SCALE_FACTOR_1),
						m(ElectricityMeter.ChannelId.VOLTAGE, new SignedDoublewordElement(51),
								ElementToChannelConverters.SCALE_FACTOR_1),
						m(MeterSiemensPac1600.ChannelId.VOLTAGE_LL, new SignedDoublewordElement(53),
								ElementToChannelConverters.SCALE_FACTOR_1),
						new DummyRegisterElement(55, 56), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(57),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_2_AND_INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(59),
								ElementToChannelConverters.SCALE_FACTOR_MINUS_2_AND_INVERT_IF_TRUE(this.invert))));
		if (!this.invert) {
			modbusProtocol
					.addTask(new FC3ReadRegistersTask(6687, Priority.LOW,
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(6687),
									ElementToChannelConverters.DIRECT_1_TO_1),
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(6689),
									ElementToChannelConverters.DIRECT_1_TO_1),
							m(MeterSiemensPac1600.ChannelId.REACTIVE_CONSUMPTION_ENERGY,
									new UnsignedDoublewordElement(6691), ElementToChannelConverters.DIRECT_1_TO_1),
							m(MeterSiemensPac1600.ChannelId.REACTIVE_PRODUCTION_ENERGY,
									new UnsignedDoublewordElement(6693), ElementToChannelConverters.DIRECT_1_TO_1),
							new DummyRegisterElement(6695, 6706),
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1,
									new UnsignedDoublewordElement(6707), ElementToChannelConverters.DIRECT_1_TO_1),
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1,
									new UnsignedDoublewordElement(6709), ElementToChannelConverters.DIRECT_1_TO_1),
							m(MeterSiemensPac1600.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L1,
									new UnsignedDoublewordElement(6711), ElementToChannelConverters.DIRECT_1_TO_1),
							m(MeterSiemensPac1600.ChannelId.REACTIVE_PRODUCTION_ENERGY_L1,
									new UnsignedDoublewordElement(6713), ElementToChannelConverters.DIRECT_1_TO_1)));
			modbusProtocol.addTask(new FC3ReadRegistersTask(6727, Priority.LOW, // DummyRegisterElement didn`t work here
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, new UnsignedDoublewordElement(6727),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, new UnsignedDoublewordElement(6729),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(MeterSiemensPac1600.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L2, new UnsignedDoublewordElement(6731),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(MeterSiemensPac1600.ChannelId.REACTIVE_PRODUCTION_ENERGY_L2, new UnsignedDoublewordElement(6733),
							ElementToChannelConverters.DIRECT_1_TO_1),
					new DummyRegisterElement(6735, 6746),
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, new UnsignedDoublewordElement(6747),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, new UnsignedDoublewordElement(6749),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(MeterSiemensPac1600.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L3, new UnsignedDoublewordElement(6751),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(MeterSiemensPac1600.ChannelId.REACTIVE_PRODUCTION_ENERGY_L3, new UnsignedDoublewordElement(6753),
							ElementToChannelConverters.DIRECT_1_TO_1)));
		} else {
			modbusProtocol
					.addTask(new FC3ReadRegistersTask(6687, Priority.LOW,
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(6687),
									ElementToChannelConverters.DIRECT_1_TO_1),
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(6689),
									ElementToChannelConverters.DIRECT_1_TO_1),
							m(MeterSiemensPac1600.ChannelId.REACTIVE_PRODUCTION_ENERGY,
									new UnsignedDoublewordElement(6691), ElementToChannelConverters.DIRECT_1_TO_1),
							m(MeterSiemensPac1600.ChannelId.REACTIVE_CONSUMPTION_ENERGY,
									new UnsignedDoublewordElement(6693), ElementToChannelConverters.DIRECT_1_TO_1),
							new DummyRegisterElement(6695, 6706),
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1,
									new UnsignedDoublewordElement(6707), ElementToChannelConverters.DIRECT_1_TO_1),
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1,
									new UnsignedDoublewordElement(6709), ElementToChannelConverters.DIRECT_1_TO_1),
							m(MeterSiemensPac1600.ChannelId.REACTIVE_PRODUCTION_ENERGY_L1,
									new UnsignedDoublewordElement(6711), ElementToChannelConverters.DIRECT_1_TO_1),
							m(MeterSiemensPac1600.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L1,
									new UnsignedDoublewordElement(6713), ElementToChannelConverters.DIRECT_1_TO_1)));
			modbusProtocol.addTask(new FC3ReadRegistersTask(6727, Priority.LOW, // DummyRegisterElement didn`t work here
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, new UnsignedDoublewordElement(6727),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, new UnsignedDoublewordElement(6729),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(MeterSiemensPac1600.ChannelId.REACTIVE_PRODUCTION_ENERGY_L2, new UnsignedDoublewordElement(6731),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(MeterSiemensPac1600.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L2, new UnsignedDoublewordElement(6733),
							ElementToChannelConverters.DIRECT_1_TO_1),
					new DummyRegisterElement(6735, 6746),
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, new UnsignedDoublewordElement(6747),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, new UnsignedDoublewordElement(6749),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(MeterSiemensPac1600.ChannelId.REACTIVE_PRODUCTION_ENERGY_L3, new UnsignedDoublewordElement(6751),
							ElementToChannelConverters.DIRECT_1_TO_1),
					m(MeterSiemensPac1600.ChannelId.REACTIVE_CONSUMPTION_ENERGY_L3, new UnsignedDoublewordElement(6753),
							ElementToChannelConverters.DIRECT_1_TO_1)));
		}
		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode));
	}
}