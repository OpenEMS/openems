package io.openems.edge.meter.kdk.puct2;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.KDK.2PUCT", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterKdk2puctImpl extends AbstractOpenemsModbusComponent
		implements MeterKdk2puct, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private Config config;

	@Reference
	protected ConfigurationAdmin cm;

	private static final long OLD_SOFTWARE_VERSION_CHECKSUM = 573118835; // hexadecimal - 22291973

	private static final int CT_RATIO_NOT_NEEDED = 1;

	public MeterKdk2puctImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				MeterKdk2puct.ChannelId.values(), //
				SymmetricMeter.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(MeterKdk2puct.class, accessMode, 100) //
						.build());
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x4007, Priority.LOW, //
						m(MeterKdk2puct.ChannelId.SOFTWARE_VERSION, new FloatDoublewordElement(0x4007))),
				new FC3ReadRegistersTask(0x401F, Priority.LOW, //
						m(MeterKdk2puct.ChannelId.PRIMARY_CURRENT, new SignedWordElement(0x401F)),
						m(MeterKdk2puct.ChannelId.SECONDARY_CURRENT, new SignedWordElement(0x4020)),
						new DummyRegisterElement(0x4021, 0x4022), //
						m(MeterKdk2puct.ChannelId.SOFTWARE_VERSION_CHECK_SUM, new UnsignedDoublewordElement(0x4023))),
				new FC3ReadRegistersTask(0x5002, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, //
								new FloatDoublewordElement(0x5002), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, //
								new FloatDoublewordElement(0x5004), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, //
								new FloatDoublewordElement(0x5006), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(SymmetricMeter.ChannelId.FREQUENCY, //
								new FloatDoublewordElement(0x5008), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						new DummyRegisterElement(0x500A, 0x500B), //
						m(MeterKdk2puct.ChannelId.METER_CURRENT_L1, //
								new FloatDoublewordElement(0x500C), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(MeterKdk2puct.ChannelId.METER_CURRENT_L2, //
								new FloatDoublewordElement(0x500E), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(MeterKdk2puct.ChannelId.METER_CURRENT_L3, //
								new FloatDoublewordElement(0x5010), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(MeterKdk2puct.ChannelId.METER_ACTIVE_POWER, //
								new FloatDoublewordElement(0x5012),
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(MeterKdk2puct.ChannelId.METER_ACTIVE_POWER_L1, //
								new FloatDoublewordElement(0x5014),
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(MeterKdk2puct.ChannelId.METER_ACTIVE_POWER_L2, //
								new FloatDoublewordElement(0x5016),
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(MeterKdk2puct.ChannelId.METER_ACTIVE_POWER_L3, //
								new FloatDoublewordElement(0x5018),
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(MeterKdk2puct.ChannelId.METER_REACTIVE_POWER, //
								new FloatDoublewordElement(0x501A),
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(MeterKdk2puct.ChannelId.METER_REACTIVE_POWER_L1, //
								new FloatDoublewordElement(0x501C),
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(MeterKdk2puct.ChannelId.METER_REACTIVE_POWER_L2, //
								new FloatDoublewordElement(0x501E),
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.config.invert())), //
						m(MeterKdk2puct.ChannelId.METER_REACTIVE_POWER_L3, //
								new FloatDoublewordElement(0x5020),
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.config.invert())) //
				));

		if (this.config.invert()) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(0x600C, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
							new FloatDoublewordElement(0x600C), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0x600E, 0x6011), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
							new FloatDoublewordElement(0x6012), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
							new FloatDoublewordElement(0x6014), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
							new FloatDoublewordElement(0x6016), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
							new FloatDoublewordElement(0x6018), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0x601A, 0x601D), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
							new FloatDoublewordElement(0x601E), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
							new FloatDoublewordElement(0x6020), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
							new FloatDoublewordElement(0x6022), //
							ElementToChannelConverter.SCALE_FACTOR_3) //
			));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(0x600C, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
							new FloatDoublewordElement(0x600C), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0x600E, 0x6011), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
							new FloatDoublewordElement(0x6012), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
							new FloatDoublewordElement(0x6014), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					m(AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
							new FloatDoublewordElement(0x6016), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
							new FloatDoublewordElement(0x6018), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					new DummyRegisterElement(0x601A, 0x601D), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
							new FloatDoublewordElement(0x601E), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
							new FloatDoublewordElement(0x6020), //
							ElementToChannelConverter.SCALE_FACTOR_3), //
					m(AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
							new FloatDoublewordElement(0x6022), //
							ElementToChannelConverter.SCALE_FACTOR_3) //
			));
		}

		// update the channel values based on ct-ratio.
		this.setUpdatedCurrentAndPowerChannels();

		// update the Sum current and avg Voltage
		this.calculateSumCurrent();
		this.calculateAverageVoltage();
		return modbusProtocol;
	}

	/**
	 * Updates the CT ratio when there is a value or change in the value in
	 * 'software version checksum' of the meter or, primary current or secondary
	 * current of the transformer.
	 */
	private void updateCtRatio() {

		Integer ctRatio = null;
		var currentVersionCheckSum = this.getSofwareVersionCheckSumChannel().getNextValue().get();

		if (currentVersionCheckSum != null) {
			// Calculates the 'CT_RATIO' only if the meter software version is older.
			if (currentVersionCheckSum == MeterKdk2puctImpl.OLD_SOFTWARE_VERSION_CHECKSUM) {
				var primaryCurrent = this.getPrimaryCurrentChannel().getNextValue().get();
				var secondaryCurrent = this.getSecondaryCurrentChannel().getNextValue().get();

				if (primaryCurrent != null && secondaryCurrent != null) {
					ctRatio = TypeUtils.divide(primaryCurrent, secondaryCurrent);
				}
			} else {
				/**
				 * If the software is newer, we can directly append the values read from the
				 * meter registers. So Initialized ctRatio with basic multiplier '1', to not
				 * change the value after multiplying.
				 */
				ctRatio = MeterKdk2puctImpl.CT_RATIO_NOT_NEEDED;
			}
		}

		// Set the channel for debugging.
		this._setCtRatio(ctRatio);
	}

	/**
	 * Sets and updates the power and current channels.
	 */
	private void setUpdatedCurrentAndPowerChannels() {

		this.getSofwareVersionCheckSumChannel().onSetNextValue(value -> {
			this.updateCtRatio();
		});

		this.getPrimaryCurrentChannel().onSetNextValue(value -> {
			this.updateCtRatio();
		});

		this.getSecondaryCurrentChannel().onSetNextValue(value -> {
			this.updateCtRatio();
		});

		this.getKdkMeterCurrentL1Channel().onSetNextValue(value -> {
			this.setValue(this::_setCurrentL1, value);
		});

		this.getKdkMeterCurrentL2Channel().onSetNextValue(value -> {
			this.setValue(this::_setCurrentL2, value);
		});

		this.getKdkMeterCurrentL3Channel().onSetNextValue(value -> {
			this.setValue(this::_setCurrentL3, value);
		});

		this.getKdkMeterActivePowerChannel().onSetNextValue(value -> {
			this.setValue(this::_setActivePower, value);
		});

		this.getKdkMeterActivePowerL1Channel().onSetNextValue(value -> {
			this.setValue(this::_setActivePowerL1, value);
		});

		this.getKdkMeterActivePowerL2Channel().onSetNextValue(value -> {
			this.setValue(this::_setActivePowerL2, value);
		});

		this.getKdkMeterActivePowerL3Channel().onSetNextValue(value -> {
			this.setValue(this::_setActivePowerL3, value);
		});

		this.getKdkMeterReactivePowerChannel().onSetNextValue(value -> {
			this.setValue(this::_setReactivePower, value);
		});

		this.getKdkMeterReactivePowerL1Channel().onSetNextValue(value -> {
			this.setValue(this::_setReactivePowerL1, value);
		});

		this.getKdkMeterReactivePowerL2Channel().onSetNextValue(value -> {
			this.setValue(this::_setReactivePowerL2, value);
		});

		this.getKdkMeterReactivePowerL3Channel().onSetNextValue(value -> {
			this.setValue(this::_setReactivePowerL3, value);
		});

	}

	/**
	 * Calculates and sets the updated value to appropriate channels.
	 * 
	 * @param consumer the consumer that gets executed for every instance.
	 * @param value    the updated channel value read from the meter.
	 */
	private void setValue(Consumer<Integer> consumer, Value<Integer> value) {
		// the CT_RATIO will be '1' if the software version is newer.
		var ctRatio = this.getCtRatioChannel().getNextValue().get();

		if (ctRatio == null) {
			// Values are not yet read.
			return;
		}
		consumer.accept(value.asOptional().map(t -> TypeUtils.multiply(t, ctRatio)).orElse(null));
	}

	/**
	 * Calculate Sum Current from Current L1, L2 and L3.
	 */
	private void calculateSumCurrent() {
		final Consumer<Value<Integer>> calculateSumCurrent = ignore -> {

			// Set Current
			this._setCurrent(TypeUtils.sum(//
					this.getCurrentL1().get(), //
					this.getCurrentL2().get(), //
					this.getCurrentL3().get() //
			));
		};

		this.getCurrentL1Channel().onSetNextValue(calculateSumCurrent);
		this.getCurrentL2Channel().onSetNextValue(calculateSumCurrent);
		this.getCurrentL3Channel().onSetNextValue(calculateSumCurrent);
	}

	/**
	 * Calculate Average Voltage from Voltage L1, L2 and L3.
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
	public MeterType getMeterType() {
		return this.config.type();
	}
}
