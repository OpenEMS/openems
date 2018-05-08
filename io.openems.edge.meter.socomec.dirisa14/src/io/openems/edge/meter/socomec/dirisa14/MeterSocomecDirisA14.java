package io.openems.edge.meter.socomec.dirisa14;

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
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.Meter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.asymmetric.api.AsymmetricMeter;
import io.openems.edge.meter.symmetric.api.SymmetricMeter;

/**
 * Implements the SOCOMEC Diris A14 meter
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.SOCOMEC.DirisA14", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterSocomecDirisA14 extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSocomecDirisA14() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		// get Meter Type:
		try {
			this.meterType = MeterType.valueOf(config.type().toUpperCase());
		} catch (IllegalArgumentException e) {
			this.meterType = MeterType.PRODUCTION; // default
		}

		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
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
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						cm(new UnsignedDoublewordElement(0xc558)) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_1) //
								.build(), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(0xc55A),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(0xc55C),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(Meter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0xc560)),
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0xc562)),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0xc564)),
						m(SymmetricMeter.ChannelId.CURRENT, new UnsignedDoublewordElement(0xc566)),
						cm(new SignedDoublewordElement(0xc568)) //
								.m(SymmetricMeter.ChannelId.ACTIVE_POWER, ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(SymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_NEGATIVE_INVERT) //
								.m(SymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new SignedDoublewordElement(0xc56A)) //
								.m(SymmetricMeter.ChannelId.REACTIVE_POWER, ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(SymmetricMeter.ChannelId.CONSUMPTION_REACTIVE_POWER,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_NEGATIVE_INVERT) //
								.m(SymmetricMeter.ChannelId.PRODUCTION_REACTIVE_POWER,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_POSITIVE) //
								.build(), //
						new DummyRegisterElement(0xc56C, 0xc56F), cm(new FloatDoublewordElement(0xc570)) //
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER_L1,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER_L1,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(0xc572)) //
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER_L2,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER_L2,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(0xc574)) //
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER_L3,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER_L3,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(0xc576)) //
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1,
										ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_REACTIVE_POWER_L1,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_REACTIVE_POWER_L1,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(0xc578)) //
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2,
										ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_REACTIVE_POWER_L2,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_REACTIVE_POWER_L2,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(0xc57A)) //
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3,
										ElementToChannelConverter.SCALE_FACTOR_1) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_REACTIVE_POWER_L3,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_REACTIVE_POWER_L3,
										ElementToChannelConverter.SCALE_FACTOR_1_AND_CONVERT_POSITIVE) //
								.build() //
				));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}
}
