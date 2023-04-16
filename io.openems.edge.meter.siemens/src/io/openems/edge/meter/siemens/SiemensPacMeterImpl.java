package io.openems.edge.meter.siemens;

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
import io.openems.edge.bridge.modbus.api.element.FloatQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

/**
 * Implements the Siemens PAC2200/3200/4200 power meter.
 *
 * <p>
 * https://cache.industry.siemens.com/dl/files/150/26504150/att_906558/v1/A5E01168664B-04_EN-US_122016_201612221316360495.pdf
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Siemens", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SiemensPacMeterImpl extends AbstractOpenemsModbusComponent
		implements SiemensPacMeter, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;

	/*
	 * Invert power values
	 */
	private boolean invert = false;

	@Reference
	protected ConfigurationAdmin cm;

	public SiemensPacMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SiemensPacMeter.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		this.invert = config.invert();

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
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(1),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(3),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(5),
								ElementToChannelConverter.SCALE_FACTOR_3),
						new DummyRegisterElement(7, 12), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(13),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(15),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(17),
								ElementToChannelConverter.SCALE_FACTOR_3),
						new DummyRegisterElement(19, 24), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(25),
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(27),
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(29),
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(31),
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(33),
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(35),
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
						new DummyRegisterElement(37, 60), //
						m(ElectricityMeter.ChannelId.CURRENT, new FloatDoublewordElement(61),
								ElementToChannelConverter.SCALE_FACTOR_3),
						new DummyRegisterElement(63, 64), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(65),
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(67),
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert))));
		if (this.invert) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(801, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatQuadruplewordElement(801)),
					new DummyRegisterElement(805, 808), //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatQuadruplewordElement(809))));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(801, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatQuadruplewordElement(801)),
					new DummyRegisterElement(805, 808), //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatQuadruplewordElement(809))));
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
