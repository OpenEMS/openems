package io.openems.edge.meter.siemens;

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
import io.openems.edge.bridge.modbus.api.element.FloatQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

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
public class MeterSiemensImpl extends AbstractOpenemsModbusComponent
		implements MeterSiemens, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

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

	public MeterSiemensImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterSiemens.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
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
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(1), SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(3), SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(5), SCALE_FACTOR_3),
						new DummyRegisterElement(7, 12), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(13), SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(15), SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(17), SCALE_FACTOR_3),
						new DummyRegisterElement(19, 24), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(25),
								INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(27),
								INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(29),
								INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(31),
								INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(33),
								INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(35),
								INVERT_IF_TRUE(this.invert)),
						new DummyRegisterElement(37, 60), //
						m(SymmetricMeter.ChannelId.CURRENT, new FloatDoublewordElement(61), SCALE_FACTOR_3),
						new DummyRegisterElement(63, 64), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(65),
								INVERT_IF_TRUE(this.invert)),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(67),
								INVERT_IF_TRUE(this.invert))));
		if (this.invert) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(801, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatQuadruplewordElement(801)),
					new DummyRegisterElement(805, 808), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatQuadruplewordElement(809))));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(801, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatQuadruplewordElement(801)),
					new DummyRegisterElement(805, 808), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatQuadruplewordElement(809))));
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
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
