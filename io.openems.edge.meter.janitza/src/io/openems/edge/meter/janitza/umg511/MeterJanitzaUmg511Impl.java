package io.openems.edge.meter.janitza.umg511;

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
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

/**
 * Implements the Janitza UMG 511 power analyzer.
 *
 * <p>
 * https://www.janitza.de/betriebsanleitungen.html?file=files/download/manuals/current/UMG511/Modbus/janitza-mal-umg511-en.pdf
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Janitza.UMG511", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterJanitzaUmg511Impl extends AbstractOpenemsModbusComponent
		implements MeterJanitzaUmg511, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;

	/*
	 * Invert power values
	 */
	private boolean invert = false;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterJanitzaUmg511Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterJanitzaUmg511.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumCurrentFromPhases(this);
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
				new FC3ReadRegistersTask(3845, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(3845),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(3847),
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(3849),
								ElementToChannelConverter.SCALE_FACTOR_3), //
						new DummyRegisterElement(3851, 3852), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(3853),
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(3855), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(3857), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						new DummyRegisterElement(3859, 3860),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(3861), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(3863), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(3865), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						new DummyRegisterElement(3867, 3868),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(3869), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(3871), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(3873), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert))), //
				new FC3ReadRegistersTask(3925, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(3925)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(3927), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert))), //
				new FC3ReadRegistersTask(3995, Priority.LOW, //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(3995), //
								ElementToChannelConverter.SCALE_FACTOR_3)));

		if (this.invert) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(19068, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(19068)),
					new DummyRegisterElement(19070, 19075),
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(19076))));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(19068, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(19068)),
					new DummyRegisterElement(19070, 19075),
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(19076))));
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
