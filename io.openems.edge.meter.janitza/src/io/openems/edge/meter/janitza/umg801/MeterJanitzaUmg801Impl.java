package io.openems.edge.meter.janitza.umg801;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Implements the Janitza UMG 801 power analyzer.
 *
 * <p>
 * https://www.janitza.de/umg-801-pro.html
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Janitza.UMG801", //
		immediate = true, //
		configurationPolicy = REQUIRE)
public class MeterJanitzaUmg801Impl extends AbstractOpenemsModbusComponent
		implements MeterJanitzaUmg801, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private MeterType meterType = MeterType.PRODUCTION;
	/** Invert power values. */
	private boolean invert = false;
	private int measurementNumberId = 1;

	public MeterJanitzaUmg801Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterJanitzaUmg801.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		this.invert = config.invert();
		this.measurementNumberId = config.measurementNumberId();

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
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var base = this.measurementNumberId == 1 //
				? 19012 //
				: 19200 + (this.measurementNumberId - 2) * 100;
		var modbusProtocol = new ModbusProtocol(this,

				// --- Voltage
				new FC3ReadRegistersTask(19000, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(19000), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(19002), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(19004), SCALE_FACTOR_3),
						new DummyRegisterElement(19006, 19011)),

				// --- Current
				new FC3ReadRegistersTask(base, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(base), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(base + 2), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(base + 4), SCALE_FACTOR_3),
						new DummyRegisterElement(base + 6, base + 7)),

				// --- Active Power
				new FC3ReadRegistersTask(base + 8, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, //
								new FloatDoublewordElement(base + 8), INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, //
								new FloatDoublewordElement(base + 10), INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, //
								new FloatDoublewordElement(base + 12), INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, //
								new FloatDoublewordElement(base + 14), INVERT_IF_TRUE(this.invert)),
						new DummyRegisterElement(base + 16, base + 23)),

				// --- Reactive Power
				new FC3ReadRegistersTask(base + 24, Priority.LOW, //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, //
								new FloatDoublewordElement(base + 24), INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, //
								new FloatDoublewordElement(base + 26), INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, //
								new FloatDoublewordElement(base + 28), INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, //
								new FloatDoublewordElement(base + 30), INVERT_IF_TRUE(this.invert))),

				// --- Frequency
				new FC3ReadRegistersTask(base + 38, Priority.LOW, //
						m(ElectricityMeter.ChannelId.FREQUENCY, //
								new FloatDoublewordElement(base + 38), SCALE_FACTOR_3)));

		// --- Energy: Consumption + Production
		if (this.invert) {
			modbusProtocol.addTask(//
					new FC3ReadRegistersTask(base + 50, Priority.LOW, //
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
									new FloatDoublewordElement(base + 50)),
							new DummyRegisterElement(base + 52, base + 57),
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
									new FloatDoublewordElement(base + 58))));
		} else {
			modbusProtocol.addTask(//
					new FC3ReadRegistersTask(base + 50, Priority.LOW, //
							m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
									new FloatDoublewordElement(base + 50)),
							new DummyRegisterElement(base + 52, base + 57),
							m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
									new FloatDoublewordElement(base + 58))));

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
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}