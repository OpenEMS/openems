package io.openems.edge.meter.socomec.threephase;

import java.util.concurrent.CompletableFuture;
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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.socomec.SocomecMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Socomec.Threephase", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SocomecMeterThreephaseImpl extends AbstractOpenemsModbusComponent implements SocomecMeterThreephase,
		SocomecMeter, SymmetricMeter, AsymmetricMeter, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(SocomecMeterThreephaseImpl.class);

	private final ModbusProtocol modbusProtocol;

	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	public SocomecMeterThreephaseImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SocomecMeter.ChannelId.values(), //
				SocomecMeterThreephase.ChannelId.values() //
		);
		this.modbusProtocol = new ModbusProtocol(this);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.config = config;
		this.identifySocomecMeter();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	protected final ModbusProtocol defineModbusProtocol() {
		return this.modbusProtocol;
	}

	/**
	 * Identifies the Socomec meter and applies the appropriate modbus protocol.
	 */
	private void identifySocomecMeter() {
		// Search for Socomec identifier register. Needs to be "SOCO".
		this.readELementOnce(new UnsignedQuadruplewordElement(0xC350)).thenAccept(value -> {
			if (value != 0x0053004F0043004FL /* SOCO */) {
				this.channel(SocomecMeter.ChannelId.NO_SOCOMEC_METER).setNextValue(true);
				return;
			}
			// Found Socomec meter
			this.readELementOnce(new StringWordElement(0xC38A, 8)).thenAccept(name -> {
				name = name.toLowerCase();
				// NOTE: if you add a meter name here, make sure to also add it in
				// SocomecMeterSinglephaseImpl.
				if (name.startsWith("countis e23")) {
					this.logInfo(this.log, "Identified Socomec Countis E23 meter");
					this.protocolCountisE23_E24();

				} else if (name.startsWith("countis e24")) {
					this.logInfo(this.log, "Identified Socomec Countis E24 meter");
					this.protocolCountisE23_E24();

				} else if (name.startsWith("diris a-10") || name.startsWith("diris a10")) {
					this.logInfo(this.log, "Identified Socomec Diris A10 meter");
					this.protocolDirisA10();

				} else if (name.startsWith("diris a14")) {
					this.logInfo(this.log, "Identified Socomec Diris A14 meter");
					this.protocolDirisA14();

				} else if (name.startsWith("diris b30")) {
					this.logInfo(this.log, "Identified Socomec Diris B30 meter");
					this.protocolDirisB30();

				} else if (name.startsWith("countis e14")) {
					this.logError(this.log, "Identified Socomec [" + name + "] meter. This is not a threephase meter!");
					this.channel(SocomecMeterThreephase.ChannelId.NOT_A_THREEPHASE_METER).setNextValue(true);

				} else {
					this.logError(this.log, "Unable to identify Socomec [" + name + "] meter!");
					this.channel(SocomecMeter.ChannelId.UNKNOWN_SOCOMEC_METER).setNextValue(true);
				}
			});
		});
	}

	/**
	 * Applies the modbus protocol for Socomec Countis E23 and E24. Both are
	 * identical.
	 */
	private void protocolCountisE23_E24() {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xC57C, 0xC587), //
						m(SymmetricMeter.ChannelId.CURRENT, new UnsignedDoublewordElement(0xC588)), //
						new DummyRegisterElement(0xC58A, 0xC58B), //
						m(SymmetricMeter.ChannelId.VOLTAGE, new UnsignedDoublewordElement(0xC58C),
								ElementToChannelConverter.SCALE_FACTOR_1))); //
		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							ElementToChannelConverter.SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							ElementToChannelConverter.SCALE_FACTOR_1) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							ElementToChannelConverter.SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							ElementToChannelConverter.SCALE_FACTOR_1) //
			));
		}
	}

	/**
	 * Applies the modbus protocol for Socomec Diris A14.
	 */
	private void protocolDirisA14() {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())))); //

		this.calculateSumCurrent();
		this.calculateAverageVoltage();

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							ElementToChannelConverter.SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							ElementToChannelConverter.SCALE_FACTOR_1) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							ElementToChannelConverter.SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							ElementToChannelConverter.SCALE_FACTOR_1) //
			));
		}
	}

	/**
	 * Applies the modbus protocol for Socomec Diris A10.
	 */
	private void protocolDirisA10() {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0xc558),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc566, 0xc567), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())))); //

		this.calculateSumCurrent();
		this.calculateAverageVoltage();

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC65C, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC65C),
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0xC65E, 0xC661), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC662),
							ElementToChannelConverter.SCALE_FACTOR_3) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC65C, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC65C),
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0xC65E, 0xC661), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC662),
							ElementToChannelConverter.SCALE_FACTOR_3) //
			));
		}
	}

	/**
	 * Applies the modbus protocol for Socomec Diris B30.
	 */
	private void protocolDirisB30() {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0x480C, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0x480C),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0x480E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0x4810),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						new DummyRegisterElement(0x4812, 0x4819), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0x481A), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0x481C), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0x481E), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0x4820, 0x482B), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0x482C), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0x482E), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						new DummyRegisterElement(0x4830, 0x4837), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0x4838), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0x483A), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0x483C), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0x483E), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0x4840), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0x4842), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.config.invert())) //
				));

		this.calculateSumCurrent();
		this.calculateAverageVoltage();

		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0x4D83, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0x4D83),
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0x4D85), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0x4D86),
							ElementToChannelConverter.SCALE_FACTOR_3) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0x4D83, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0x4D83),
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0x4D85), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0x4D86),
							ElementToChannelConverter.SCALE_FACTOR_3) //
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

	/**
	 * Reads given Element once from Modbus.
	 * 
	 * @param <T>     the Type of the element
	 * @param element the element
	 * @return a future value, e.g. a integer
	 */
	private <T> CompletableFuture<T> readELementOnce(AbstractModbusElement<T> element) {
		// Prepare result
		final CompletableFuture<T> result = new CompletableFuture<T>();

		// Activate task
		final Task task = new FC3ReadRegistersTask(element.getStartAddress(), Priority.HIGH, element);
		this.modbusProtocol.addTask(task);

		// Register listener for element
		element.onUpdateCallback(value -> {
			if (value == null) {
				// try again
				return;
			}
			// do not try again
			this.modbusProtocol.removeTask(task);
			result.complete(value);
		});

		return result;
	}
}
