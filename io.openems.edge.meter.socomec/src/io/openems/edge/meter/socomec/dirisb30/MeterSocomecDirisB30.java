package io.openems.edge.meter.socomec.dirisb30;

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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Implements the SOCOMEC Diris B30 meter
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.SOCOMEC.DirisB30", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterSocomecDirisB30 extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;

	/*
	 * Invert power values
	 */
	private boolean invert = false;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSocomecDirisB30() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		this.invert = config.invert();

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
	}

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

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x480A, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0x480A))),//
				new FC3ReadRegistersTask(0x480C, Priority.HIGH, //
						m(new UnsignedDoublewordElement(0x480C)) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_1) //
								.build(), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0x480E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0x4810),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						new DummyRegisterElement(0x4812, 0x4819), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0x481A), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0x481C), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0x481E), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(SymmetricMeter.ChannelId.CURRENT, new UnsignedDoublewordElement(0x4820), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert))), //
				new FC3ReadRegistersTask(0x482C, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0x482C), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0x482E), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						new DummyRegisterElement(0x4830, 0x4837), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0x4838), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0x483A), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0x483C), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0x483E), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0x4840), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0x4842), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)))//
				);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}
	
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {		
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
