package io.openems.edge.meter.bcontrol.em300;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

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
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.BControl.EM300", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterBControlEM300Impl extends AbstractOpenemsModbusComponent
		implements MeterBControlEM300, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public MeterBControlEM300Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterBControlEM300.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		// Calculates required Channels from other existing Channels.
		this.addCalculateChannelListeners();
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
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0, Priority.HIGH, //
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_POS, new UnsignedDoublewordElement(0),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_NEG, new UnsignedDoublewordElement(2),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_POS, new UnsignedDoublewordElement(4),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_NEG, new UnsignedDoublewordElement(6),
								SCALE_FACTOR_MINUS_1),

						new DummyRegisterElement(8, 25),

						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(26))),

				new FC3ReadRegistersTask(40, Priority.HIGH,
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L1_POS, new UnsignedDoublewordElement(40),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L1_NEG, new UnsignedDoublewordElement(42),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L1_POS, new UnsignedDoublewordElement(44),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L1_NEG, new UnsignedDoublewordElement(46),
								SCALE_FACTOR_MINUS_1),

						new DummyRegisterElement(48, 59),

						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(60)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(62)),

						new DummyRegisterElement(64, 79), //

						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L2_POS, new UnsignedDoublewordElement(80),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L2_NEG, new UnsignedDoublewordElement(82),
								SCALE_FACTOR_MINUS_1),

						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L2_POS, new UnsignedDoublewordElement(84),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L2_NEG, new UnsignedDoublewordElement(86),
								SCALE_FACTOR_MINUS_1),

						new DummyRegisterElement(88, 99),

						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(100)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(102)),

						new DummyRegisterElement(104, 119),

						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L3_POS, new UnsignedDoublewordElement(120),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L3_NEG, new UnsignedDoublewordElement(122),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L3_POS, new UnsignedDoublewordElement(124),
								SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L3_NEG, new UnsignedDoublewordElement(126),
								SCALE_FACTOR_MINUS_1),

						new DummyRegisterElement(128, 139),

						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(140)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(142))));

		if (this.config.invert()) {
			modbusProtocol.addTasks(//
					new FC3ReadRegistersTask(512, Priority.LOW, //
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
									new UnsignedQuadruplewordElement(512), SCALE_FACTOR_MINUS_1),
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
									new UnsignedQuadruplewordElement(516), SCALE_FACTOR_MINUS_1)),
					new FC3ReadRegistersTask(592, Priority.LOW, //
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
									new UnsignedQuadruplewordElement(592), SCALE_FACTOR_MINUS_1), //
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
									new UnsignedQuadruplewordElement(596), SCALE_FACTOR_MINUS_1)), //
					new FC3ReadRegistersTask(672, Priority.LOW, //
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
									new UnsignedQuadruplewordElement(672), SCALE_FACTOR_MINUS_1), //
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
									new UnsignedQuadruplewordElement(676), SCALE_FACTOR_MINUS_1)), //
					new FC3ReadRegistersTask(752, Priority.LOW, //
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
									new UnsignedQuadruplewordElement(752), SCALE_FACTOR_MINUS_1), //
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
									new UnsignedQuadruplewordElement(756), SCALE_FACTOR_MINUS_1)));
		} else {
			modbusProtocol.addTasks(//
					new FC3ReadRegistersTask(512, Priority.LOW, //
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
									new UnsignedQuadruplewordElement(512), SCALE_FACTOR_MINUS_1),
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
									new UnsignedQuadruplewordElement(516), SCALE_FACTOR_MINUS_1)),
					new FC3ReadRegistersTask(592, Priority.LOW, //
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
									new UnsignedQuadruplewordElement(592), SCALE_FACTOR_MINUS_1), //
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
									new UnsignedQuadruplewordElement(596), SCALE_FACTOR_MINUS_1)), //
					new FC3ReadRegistersTask(672, Priority.LOW, //
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
									new UnsignedQuadruplewordElement(672), SCALE_FACTOR_MINUS_1), //
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
									new UnsignedQuadruplewordElement(676), SCALE_FACTOR_MINUS_1)), //
					new FC3ReadRegistersTask(752, Priority.LOW, //
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
									new UnsignedQuadruplewordElement(752), SCALE_FACTOR_MINUS_1), //
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
									new UnsignedQuadruplewordElement(756), SCALE_FACTOR_MINUS_1)));
		}

		return modbusProtocol;
	}

	/**
	 * Calculates a Power Channel from Pos and Neg values as required by B-Control
	 * modbus specification. Also applies the 'invert' config parameter.
	 */
	private static class CalculatePower implements Consumer<Value<Integer>> {

		private final IntegerReadChannel posChannel;
		private final IntegerReadChannel negChannel;
		private final IntegerReadChannel targetChannel;
		private final boolean invert;

		public static CalculatePower of(MeterBControlEM300Impl parent,
				io.openems.edge.common.channel.ChannelId posChannelId,
				io.openems.edge.common.channel.ChannelId negChannelId,
				io.openems.edge.common.channel.ChannelId targetChannelId) {
			return new CalculatePower(parent, posChannelId, negChannelId, targetChannelId);
		}

		private CalculatePower(MeterBControlEM300Impl parent, io.openems.edge.common.channel.ChannelId posChannelId,
				io.openems.edge.common.channel.ChannelId negChannelId,
				io.openems.edge.common.channel.ChannelId targetChannelId) {
			this.invert = parent.config.invert();

			// Get actual Channels
			this.posChannel = parent.channel(posChannelId);
			this.negChannel = parent.channel(negChannelId);
			this.targetChannel = parent.channel(targetChannelId);

			// Add Listeners
			this.posChannel.onSetNextValue(this);
			this.negChannel.onSetNextValue(this);
		}

		@Override
		public void accept(Value<Integer> ignore) {
			var posValue = this.posChannel.getNextValue();
			var negValue = this.negChannel.getNextValue();
			final Integer result;
			if (posValue.isDefined() && negValue.isDefined()) {
				if (this.invert) {
					result = (posValue.get() - negValue.get()) * -1;
				} else {
					result = posValue.get() - negValue.get();
				}
			} else {
				result = null;
			}
			this.targetChannel.setNextValue(result);
		}
	}

	/**
	 * Calculates required Channels from other existing Channels.
	 */
	private void addCalculateChannelListeners() {
		// Active Power
		CalculatePower.of(this, MeterBControlEM300.ChannelId.ACTIVE_POWER_POS,
				MeterBControlEM300.ChannelId.ACTIVE_POWER_NEG, ElectricityMeter.ChannelId.ACTIVE_POWER);
		CalculatePower.of(this, MeterBControlEM300.ChannelId.ACTIVE_POWER_L1_POS,
				MeterBControlEM300.ChannelId.ACTIVE_POWER_L1_NEG, ElectricityMeter.ChannelId.ACTIVE_POWER_L1);
		CalculatePower.of(this, MeterBControlEM300.ChannelId.ACTIVE_POWER_L2_POS,
				MeterBControlEM300.ChannelId.ACTIVE_POWER_L2_NEG, ElectricityMeter.ChannelId.ACTIVE_POWER_L2);
		CalculatePower.of(this, MeterBControlEM300.ChannelId.ACTIVE_POWER_L3_POS,
				MeterBControlEM300.ChannelId.ACTIVE_POWER_L3_NEG, ElectricityMeter.ChannelId.ACTIVE_POWER_L3);

		// Reactive Power
		CalculatePower.of(this, MeterBControlEM300.ChannelId.REACTIVE_POWER_POS,
				MeterBControlEM300.ChannelId.REACTIVE_POWER_NEG, ElectricityMeter.ChannelId.REACTIVE_POWER);
		CalculatePower.of(this, MeterBControlEM300.ChannelId.REACTIVE_POWER_L1_POS,
				MeterBControlEM300.ChannelId.REACTIVE_POWER_L1_NEG, ElectricityMeter.ChannelId.REACTIVE_POWER_L1);
		CalculatePower.of(this, MeterBControlEM300.ChannelId.REACTIVE_POWER_L2_POS,
				MeterBControlEM300.ChannelId.REACTIVE_POWER_L2_NEG, ElectricityMeter.ChannelId.REACTIVE_POWER_L2);
		CalculatePower.of(this, MeterBControlEM300.ChannelId.REACTIVE_POWER_L3_POS,
				MeterBControlEM300.ChannelId.REACTIVE_POWER_L3_NEG, ElectricityMeter.ChannelId.REACTIVE_POWER_L3);

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
