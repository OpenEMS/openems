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

//import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Artemes.AM2", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)

public class MeterArtemesAM2 extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private MeterType metertype = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterArtemesAM2() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.metertype = config.type();

		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deativate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
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
		return this.metertype;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(0x0000, Priority.HIGH,
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(0x0000)),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0x0002)),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0x0004)),
						//new DummyRegisterElement(0x0006, 0x000C),
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new SignedWordElement(0x000E)),
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new SignedWordElement(0x0010)),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new SignedWordElement(0x0012)),
						//new DummyRegisterElement(0x0014, 0x0016),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(0x0018)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(0x001C)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(0X0020)),
						//new DummyRegisterElement(0x0024, 0x0034),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedWordElement(0x0038)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedWordElement(0x003C)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedWordElement(0x0040))
						));
	}

	@Override
	public String debugLog() {
		return " V1:" + this.getVoltageL1().value().asString() + " | " + 
				" V2:" + this.getVoltageL2().value().asString() + " | " + 
				" V2:" + this.getVoltageL3().value().asString() + " | " + 
				" C1:" + this.getCurrentL1().value().asString() + " | " + 
				" C2:" + this.getCurrentL2().value().asString() + " | " + 
				" C3:" + this.getCurrentL3().value().asString() + " | " +
				" AP1:" + this.getActivePowerL1().value().asString() + " | " +
				" AP2:" + this.getActivePowerL2().value().asString() + " | " +
				" AP3:" + this.getActivePowerL3().value().asString() + " | " +
				" RP1:" + this.getReactivePowerL1().value().asString() + " | " +
				" RP2:" + this.getReactivePowerL2().value().asString() + " | " +
				" RP3:" + this.getReactivePowerL3().value().asString() ;
	}

}
