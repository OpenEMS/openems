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
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

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
				new FC3ReadRegistersTask(0x480C, Priority.LOW, //
						m(new UnsignedDoublewordElement(0x480C)) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_1) //
								.build(), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0x480E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0x4810),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						new DummyRegisterElement(0x4812, 0x4819), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0x481A)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0x481C)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0x481E)), //
						m(SymmetricMeter.ChannelId.CURRENT, new UnsignedDoublewordElement(0x4820))), //
				new FC3ReadRegistersTask(0x482C, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0x482C)), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0x482E)), //
						new DummyRegisterElement(0x4830, 0x4837), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0x4838)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0x483A)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0x483C)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0x483E)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0x4840)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0x4842)) //
				));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}
}
