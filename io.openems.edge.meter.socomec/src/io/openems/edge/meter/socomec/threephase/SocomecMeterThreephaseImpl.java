package io.openems.edge.meter.socomec.threephase;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.socomec.AbstractSocomecMeter;
import io.openems.edge.meter.socomec.SocomecMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Socomec.Threephase", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SocomecMeterThreephaseImpl extends AbstractSocomecMeter implements SocomecMeterThreephase, SocomecMeter,
		ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(SocomecMeterThreephaseImpl.class);

	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	public SocomecMeterThreephaseImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SocomecMeter.ChannelId.values(), //
				SocomecMeterThreephase.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this.identifySocomecMeter();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	protected void identifiedCountisE23_E24_E27_E28() throws OpenemsException {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E)), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564)), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xC57C, 0xC587), //
						m(ElectricityMeter.ChannelId.CURRENT, new UnsignedDoublewordElement(0xC588)), //
						new DummyRegisterElement(0xC58A, 0xC58B), //
						m(ElectricityMeter.ChannelId.VOLTAGE, new UnsignedDoublewordElement(0xC58C),
								ElementToChannelConverter.SCALE_FACTOR_1))); //
		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							ElementToChannelConverter.SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							ElementToChannelConverter.SCALE_FACTOR_1) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							ElementToChannelConverter.SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							ElementToChannelConverter.SCALE_FACTOR_1) //
			));
		}
	}

	@Override
	protected void identifiedCountisE34_E44() throws OpenemsException {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564)), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert()))));

		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							ElementToChannelConverter.SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							ElementToChannelConverter.SCALE_FACTOR_1) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							ElementToChannelConverter.SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							ElementToChannelConverter.SCALE_FACTOR_1) //
			));
		}
	}

	@Override
	protected void identifiedDirisA14() throws OpenemsException {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564)), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())))); //

		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							ElementToChannelConverter.SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							ElementToChannelConverter.SCALE_FACTOR_1) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							ElementToChannelConverter.SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							ElementToChannelConverter.SCALE_FACTOR_1) //
			));
		}
	}

	@Override
	protected void identifiedDirisA10() throws OpenemsException {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564)), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())))); //

		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC65C, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC65C),
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0xC65E, 0xC661), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC662),
							ElementToChannelConverter.SCALE_FACTOR_3) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC65C, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC65C),
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0xC65E, 0xC661), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC662),
							ElementToChannelConverter.SCALE_FACTOR_3) //
			));
		}
	}

	@Override
	protected void identifiedDirisB30() throws OpenemsException {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0x480C, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0x480C),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0x480E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0x4810),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						new DummyRegisterElement(0x4812, 0x4819), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0x481A)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0x481C)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0x481E)), //
						new DummyRegisterElement(0x4820, 0x482B), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0x482C), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0x482E), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0x4830, 0x4837), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0x4838), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0x483A), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0x483C), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0x483E), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0x4840), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0x4842), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())) //
				));

		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0x4D83, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0x4D83),
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0x4D85), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0x4D86),
							ElementToChannelConverter.SCALE_FACTOR_3) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0x4D83, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0x4D83),
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0x4D85), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0x4D86),
							ElementToChannelConverter.SCALE_FACTOR_3) //
			));
		}
	}

	@Override
	protected void identifiedCountisE14() throws OpenemsException {
		this.thisIsNotAThreePhaseMeter();
	}

	private void thisIsNotAThreePhaseMeter() {
		this.logError(this.log, "This is not a threephase meter!");
		this.channel(SocomecMeterThreephase.ChannelId.NOT_A_THREEPHASE_METER).setNextValue(true);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
