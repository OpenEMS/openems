package io.openems.edge.meter.artemes.am2;

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
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Artemes.AM2", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterArtemesAM2 extends AbstractOpenemsModbusComponent
		implements ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private MeterType metertype = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterArtemesAM2() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.metertype;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this,
				new FC4ReadInputRegistersTask(0x0000, Priority.HIGH,
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0x0000)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0x0002)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0x0004)),
						new DummyRegisterElement(0x0006, 0x000B),
						m(ElectricityMeter.ChannelId.VOLTAGE, new UnsignedDoublewordElement(0x000C)),
						m(ElectricityMeter.ChannelId.CURRENT_L1, new SignedDoublewordElement(0x000E)),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new SignedDoublewordElement(0x0010)),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new SignedDoublewordElement(0x0012)),
						new DummyRegisterElement(0x0014, 0x0015),
						m(ElectricityMeter.ChannelId.CURRENT, new SignedDoublewordElement(0x0016)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedQuadruplewordElement(0x0018),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedQuadruplewordElement(0x001C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedQuadruplewordElement(0X0020),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedQuadruplewordElement(0X0024),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						new DummyRegisterElement(0x0028, 0x0037),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedQuadruplewordElement(0x0038),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedQuadruplewordElement(0x003C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedQuadruplewordElement(0x0040),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedQuadruplewordElement(0x0044),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						new DummyRegisterElement(0x0048, 0x0071),
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0x0072))),

				new FC4ReadInputRegistersTask(0x0418, Priority.LOW,
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedQuadruplewordElement(0x0418),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new UnsignedQuadruplewordElement(0x0041C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)));
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
