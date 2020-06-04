package io.openems.edge.meter.socomec.singlephase;

import java.util.concurrent.CompletableFuture;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverterChain;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.socomec.SocomecMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Socomec.Singlephase", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SocomecMeterSinglephaseImpl extends AbstractOpenemsModbusComponent implements SocomecMeterSinglephase,
		SocomecMeter, SinglePhaseMeter, SymmetricMeter, AsymmetricMeter, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(SocomecMeterSinglephaseImpl.class);

	private final ModbusProtocol modbusProtocol;

	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	public SocomecMeterSinglephaseImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SocomecMeter.ChannelId.values(), //
				SocomecMeterSinglephase.ChannelId.values() //
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
	public SinglePhase getPhase() {
		return this.config.phase();
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
				// SocomecMeterThreephaseImpl.
				if (name.startsWith("countis e14")) {
					this.logInfo(this.log, "Identified Socomec Countis E14 meter");
					this.protocolCountisE14();

				} else if (//
				name.startsWith("countis e24") || //
				name.startsWith("diris a-10") || //
				name.startsWith("diris a14") || //
				name.startsWith("diris b30")) {
					this.logError(this.log, "Identified Socomec " + name + " meter. This is not a singlephase meter!");
					this.channel(SocomecMeterSinglephase.ChannelId.NOT_A_SINGLEPHASE_METER).setNextValue(true);

				} else {
					this.logError(this.log, "Unable to identify Socomec " + name + " meter!");
					this.channel(SocomecMeter.ChannelId.UNKNOWN_SOCOMEC_METER).setNextValue(true);
				}
			});
		});

	}

	/**
	 * Applies the modbus protocol for Socomec Countis E14.
	 */
	private void protocolCountisE14() {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(new UnsignedDoublewordElement(0xc558)) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new ElementToChannelConverterChain(//
										ElementToChannelConverter.SCALE_FACTOR_1, //
										ElementToChannelConverter.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L1))) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new ElementToChannelConverterChain(//
										ElementToChannelConverter.SCALE_FACTOR_1, //
										ElementToChannelConverter.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L2))) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new ElementToChannelConverterChain(//
										ElementToChannelConverter.SCALE_FACTOR_1, //
										ElementToChannelConverter.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L3))) //
								.build(), //
						new DummyRegisterElement(0xc55A, 0xc55D), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E)), //
						m(new UnsignedDoublewordElement(0xc560)) //
								.m(SymmetricMeter.ChannelId.CURRENT,
										ElementToChannelConverter.INVERT_IF_TRUE(config.invert())) //
								.m(AsymmetricMeter.ChannelId.CURRENT_L1, new ElementToChannelConverterChain(//
										ElementToChannelConverter.INVERT_IF_TRUE(config.invert()), //
										ElementToChannelConverter.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L1))) //
								.m(AsymmetricMeter.ChannelId.CURRENT_L2, new ElementToChannelConverterChain(//
										ElementToChannelConverter.INVERT_IF_TRUE(config.invert()), //
										ElementToChannelConverter.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L2))) //
								.m(AsymmetricMeter.ChannelId.CURRENT_L3, new ElementToChannelConverterChain(//
										ElementToChannelConverter.INVERT_IF_TRUE(config.invert()), //
										ElementToChannelConverter.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L3))) //
								.build(), //
						new DummyRegisterElement(0xc562, 0xc567), //
						m(new SignedDoublewordElement(0xc568)) //
								.m(SymmetricMeter.ChannelId.ACTIVE_POWER,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(config.invert()))
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, //
										new ElementToChannelConverterChain(//
												ElementToChannelConverter
														.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(config.invert()),
												ElementToChannelConverter
														.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L1))) //
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, //
										new ElementToChannelConverterChain(//
												ElementToChannelConverter
														.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(config.invert()),
												ElementToChannelConverter
														.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L2))) //
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, //
										new ElementToChannelConverterChain(//
												ElementToChannelConverter
														.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(config.invert()),
												ElementToChannelConverter
														.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L3))) //
								.build(), //
						m(new SignedDoublewordElement(0xc56A)) //
								.m(SymmetricMeter.ChannelId.REACTIVE_POWER,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(config.invert()))
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, //
										new ElementToChannelConverterChain(//
												ElementToChannelConverter
														.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(config.invert()),
												ElementToChannelConverter
														.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L1))) //
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, //
										new ElementToChannelConverterChain(//
												ElementToChannelConverter
														.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(config.invert()),
												ElementToChannelConverter
														.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L2))) //
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, //
										new ElementToChannelConverterChain(//
												ElementToChannelConverter
														.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(config.invert()),
												ElementToChannelConverter
														.SET_ZERO_IF_TRUE(config.phase() != SinglePhase.L3))) //
								.build()));
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

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				SinglePhaseMeter.getModbusSlaveNatureTable(accessMode) //
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
