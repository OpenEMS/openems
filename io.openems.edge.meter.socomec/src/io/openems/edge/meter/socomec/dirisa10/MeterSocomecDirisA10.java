package io.openems.edge.meter.socomec.dirisa10;

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
 * Implements the SOCOMEC Diris A10 meter
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.SOCOMEC.DirisA10", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterSocomecDirisA10 extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	/*
	 * Invert power values
	 */
	private boolean invert = false;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSocomecDirisA10() {
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
		ModbusProtocol protocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(new UnsignedDoublewordElement(0xc558)) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_1) //
								.build(), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(SymmetricMeter.ChannelId.CURRENT, new UnsignedDoublewordElement(0xc566), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0xc568),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.invert)), // evtl
																											// scalefactor
																											// -1 !?
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0xc56A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.invert)), //
						// TODO: add ApparentPower here
						new DummyRegisterElement(0xc56C, 0xc56F), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0xc570),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0xc572),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0xc574),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0xc576),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0xc578),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0xc57A),
								ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.invert)) //
				));

		if (this.invert) {
			protocol.addTask(new FC3ReadRegistersTask(0x0358, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0x0358)) //
//					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC708), ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
			));
		} else {
			protocol.addTask(new FC3ReadRegistersTask(0x0358, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0x0358)) //
//					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC708), ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
			));
		}

		return protocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}
}
