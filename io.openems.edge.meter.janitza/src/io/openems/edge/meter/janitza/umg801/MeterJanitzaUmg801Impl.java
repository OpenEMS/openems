package io.openems.edge.meter.janitza.umg801;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

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
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.common.types.MeterType;

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
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterJanitzaUmg801Impl extends AbstractOpenemsModbusComponent
		implements MeterJanitzaUmg801, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
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

	//wrapper method for base register
	private int calculateBaseRegister(int measurementNumberId) {
		if (measurementNumberId == 1) {
			return 19012;
		} else {
			return 19200 + (measurementNumberId - 2) * 100;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final int baseRegister = calculateBaseRegister(this.measurementNumberId);
		System.out.println("BaseRegister:" + baseRegister + "\n");
		var modbusProtocol = new ModbusProtocol(this,

			// --- Voltage
			new FC3ReadRegistersTask(19000, Priority.HIGH,
				m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(19000), SCALE_FACTOR_3),
				m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(19002), SCALE_FACTOR_3),
				m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(19004), SCALE_FACTOR_3),
				new DummyRegisterElement(19006, 19011)
			),

			// --- Current
			new FC3ReadRegistersTask(baseRegister, Priority.HIGH,
				m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(baseRegister), SCALE_FACTOR_3),
				m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(baseRegister + 2), SCALE_FACTOR_3),
				m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(baseRegister + 4), SCALE_FACTOR_3),
				new DummyRegisterElement(baseRegister + 6, baseRegister + 7)
			),

			// --- Active Power
			new FC3ReadRegistersTask(baseRegister + 8, Priority.HIGH,
				m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(baseRegister + 8), INVERT_IF_TRUE(this.invert)),
				m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(baseRegister + 10), INVERT_IF_TRUE(this.invert)),
				m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(baseRegister + 12), INVERT_IF_TRUE(this.invert)),
				m(ElectricityMeter.ChannelId.ACTIVE_POWER,    new FloatDoublewordElement(baseRegister + 14), INVERT_IF_TRUE(this.invert)),
				new DummyRegisterElement(baseRegister + 16, baseRegister + 23)
			),

			// --- Reactive Power
			new FC3ReadRegistersTask(baseRegister + 24, Priority.LOW,
				m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(baseRegister + 24), INVERT_IF_TRUE(this.invert)),
				m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(baseRegister + 26), INVERT_IF_TRUE(this.invert)),
				m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(baseRegister + 28), INVERT_IF_TRUE(this.invert)),
				m(ElectricityMeter.ChannelId.REACTIVE_POWER,    new FloatDoublewordElement(baseRegister + 30), INVERT_IF_TRUE(this.invert))
			),

			// --- Frequency
			new FC3ReadRegistersTask(baseRegister + 38, Priority.LOW,
				m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(baseRegister + 38), SCALE_FACTOR_3)
			)
		);

		// --- Energy: Consumption + Production
		if (this.invert) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(baseRegister + 50, Priority.LOW,
				m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(baseRegister + 50)),
				new DummyRegisterElement(baseRegister + 52, baseRegister + 57),
				m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(baseRegister + 58))
			));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(baseRegister + 50, Priority.LOW,
				m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(baseRegister + 50)),
				new DummyRegisterElement(baseRegister + 52, baseRegister + 57),
				m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(baseRegister + 58))
			));

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