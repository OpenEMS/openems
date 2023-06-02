package io.openems.edge.meter.artemes.am2;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

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
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Artemes.AM2", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterArtemesAM2Impl extends AbstractOpenemsModbusComponent
		implements MeterArtemesAM2, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private MeterType metertype = MeterType.PRODUCTION;

	public MeterArtemesAM2Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				MeterArtemesAM2.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.metertype = config.type();

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
		return this.metertype;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, new FC4ReadInputRegistersTask(0x0000, Priority.HIGH,
				m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0x0000)),
				m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0x0002)),
				m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0x0004)),
				new DummyRegisterElement(0x0006, 0x000B),
				m(SymmetricMeter.ChannelId.VOLTAGE, new UnsignedDoublewordElement(0x000C)),
				m(AsymmetricMeter.ChannelId.CURRENT_L1, new SignedDoublewordElement(0x000E)),
				m(AsymmetricMeter.ChannelId.CURRENT_L2, new SignedDoublewordElement(0x0010)),
				m(AsymmetricMeter.ChannelId.CURRENT_L3, new SignedDoublewordElement(0x0012)),
				new DummyRegisterElement(0x0014, 0x0015),
				m(SymmetricMeter.ChannelId.CURRENT, new SignedDoublewordElement(0x0016)),
				m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedQuadruplewordElement(0x0018),
						SCALE_FACTOR_MINUS_3),
				m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedQuadruplewordElement(0x001C),
						SCALE_FACTOR_MINUS_3),
				m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedQuadruplewordElement(0X0020),
						SCALE_FACTOR_MINUS_3),
				m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedQuadruplewordElement(0X0024), SCALE_FACTOR_MINUS_3),
				new DummyRegisterElement(0x0028, 0x0037),
				m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedQuadruplewordElement(0x0038),
						SCALE_FACTOR_MINUS_3),
				m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedQuadruplewordElement(0x003C),
						SCALE_FACTOR_MINUS_3),
				m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedQuadruplewordElement(0x0040),
						SCALE_FACTOR_MINUS_3),
				m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedQuadruplewordElement(0x0044),
						SCALE_FACTOR_MINUS_3),
				new DummyRegisterElement(0x0048, 0x0071),
				m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0x0072))),

				new FC4ReadInputRegistersTask(0x0418, Priority.LOW,
						m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedQuadruplewordElement(0x0418),
								SCALE_FACTOR_MINUS_1),
						m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedQuadruplewordElement(0x0041C),
								SCALE_FACTOR_MINUS_1)));
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
