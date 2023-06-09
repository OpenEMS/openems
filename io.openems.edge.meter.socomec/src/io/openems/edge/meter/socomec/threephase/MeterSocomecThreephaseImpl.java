package io.openems.edge.meter.socomec.threephase;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

import java.util.function.Consumer;

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
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.socomec.AbstractSocomecMeter;
import io.openems.edge.meter.socomec.SocomecMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Socomec.Threephase", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterSocomecThreephaseImpl extends AbstractSocomecMeter implements MeterSocomecThreephase, SocomecMeter,
		SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(MeterSocomecThreephaseImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public MeterSocomecThreephaseImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SocomecMeter.ChannelId.values(), //
				MeterSocomecThreephase.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
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
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C), SCALE_FACTOR_1), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564)), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xC57C, 0xC587), //
						m(SymmetricMeter.ChannelId.CURRENT, new UnsignedDoublewordElement(0xC588)), //
						new DummyRegisterElement(0xC58A, 0xC58B), //
						m(SymmetricMeter.ChannelId.VOLTAGE, new UnsignedDoublewordElement(0xC58C), SCALE_FACTOR_1))); //
		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							SCALE_FACTOR_1) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							SCALE_FACTOR_1) //
			));
		}
	}

	@Override
	protected void identifiedCountisE34_E44() throws OpenemsException {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C), SCALE_FACTOR_1), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564)), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert()))));

		this.calculateSumCurrent();
		this.calculateAverageVoltage();

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							SCALE_FACTOR_1) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							SCALE_FACTOR_1) //
			));
		}
	}

	@Override
	protected void identifiedDirisA14() throws OpenemsException {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C), SCALE_FACTOR_1), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564)), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())))); //

		this.calculateSumCurrent();
		this.calculateAverageVoltage();

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							SCALE_FACTOR_1) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							SCALE_FACTOR_1) //
			));
		}
	}

	@Override
	protected void identifiedDirisA10() throws OpenemsException {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C), SCALE_FACTOR_1), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564)), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())))); //

		this.calculateSumCurrent();
		this.calculateAverageVoltage();

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC65C, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC65C),
							SCALE_FACTOR_3), //
					new DummyRegisterElement(0xC65E, 0xC661), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC662),
							SCALE_FACTOR_3) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC65C, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC65C),
							SCALE_FACTOR_3), //
					new DummyRegisterElement(0xC65E, 0xC661), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC662),
							SCALE_FACTOR_3) //
			));
		}
	}

	@Override
	protected void identifiedDirisB30() throws OpenemsException {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0x480C, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0x480C), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0x480E), SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0x4810), SCALE_FACTOR_1), //
						new DummyRegisterElement(0x4812, 0x4819), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0x481A)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0x481C)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0x481E)), //
						new DummyRegisterElement(0x4820, 0x482B), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0x482C), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0x482E), //
								INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0x4830, 0x4837), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0x4838), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0x483A), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0x483C), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0x483E), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0x4840), //
								INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0x4842), //
								INVERT_IF_TRUE(this.config.invert())) //
				));

		this.calculateSumCurrent();
		this.calculateAverageVoltage();

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0x4D83, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0x4D83),
							SCALE_FACTOR_3), //
					new DummyRegisterElement(0x4D85), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0x4D86),
							SCALE_FACTOR_3) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0x4D83, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0x4D83),
							SCALE_FACTOR_3), //
					new DummyRegisterElement(0x4D85), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0x4D86),
							SCALE_FACTOR_3) //
			));
		}
	}

	/**
	 * Calculate Sum Current from Current L1, L2 and L3.
	 */
	private void calculateSumCurrent() {
		final Consumer<Value<Integer>> calculateSumCurrent = ignore -> {
			this._setCurrent(TypeUtils.sum(//
					this.getCurrentL1Channel().getNextValue().get(), //
					this.getCurrentL2Channel().getNextValue().get(), //
					this.getCurrentL3Channel().getNextValue().get() //
			));
		};
		this.getCurrentL1Channel().onSetNextValue(calculateSumCurrent);
		this.getCurrentL2Channel().onSetNextValue(calculateSumCurrent);
		this.getCurrentL3Channel().onSetNextValue(calculateSumCurrent);
	}

	/**
	 * Calculate Average Voltage from Current L1, L2 and L3.
	 */
	private void calculateAverageVoltage() {
		final Consumer<Value<Integer>> calculateAverageVoltage = ignore -> {
			this._setVoltage(TypeUtils.averageRounded(//
					this.getVoltageL1Channel().getNextValue().get(), //
					this.getVoltageL2Channel().getNextValue().get(), //
					this.getVoltageL3Channel().getNextValue().get() //
			));
		};
		this.getVoltageL1Channel().onSetNextValue(calculateAverageVoltage);
		this.getVoltageL2Channel().onSetNextValue(calculateAverageVoltage);
		this.getVoltageL3Channel().onSetNextValue(calculateAverageVoltage);
	}

	@Override
	protected void identifiedCountisE14() throws OpenemsException {
		this.thisIsNotAThreePhaseMeter();
	}

	private void thisIsNotAThreePhaseMeter() {
		this.logError(this.log, "This is not a threephase meter!");
		this.channel(MeterSocomecThreephase.ChannelId.NOT_A_THREEPHASE_METER).setNextValue(true);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
