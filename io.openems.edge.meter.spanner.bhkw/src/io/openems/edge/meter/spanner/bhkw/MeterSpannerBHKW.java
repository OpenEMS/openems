package io.openems.edge.meter.spanner.bhkw;

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
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Implements the meter
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Spanner.BHKW", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterSpannerBHKW extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSpannerBHKW() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values()//
		);
		AsymmetricMeter.initializePowerSumChannels(this);
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

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(370, Priority.HIGH, //
						m(new UnsignedWordElement(370)) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.SCALE_FACTOR_2) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_2) //
								.build(), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(371),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(372),
								ElementToChannelConverter.SCALE_FACTOR_2), //
//						m(BHKWChannelId.FREQUENCY_L1, new UnsignedWordElement(373),
//								ElementToChannelConverter.SCALE_FACTOR_1), //
//						m(BHKWChannelId.FREQUENCY_L2, new UnsignedWordElement(374),
//								ElementToChannelConverter.SCALE_FACTOR_1), //
//						m(BHKWChannelId.FREQUENCY_L3, new UnsignedWordElement(375),
//								ElementToChannelConverter.SCALE_FACTOR_1), //
						new DummyRegisterElement(373,375), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(376),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(377),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(378),
								ElementToChannelConverter.SCALE_FACTOR_2), //
//						m(BHKWChannelId.COSPHI_L1, new SignedWordElement(379)), //
//						m(BHKWChannelId.COSPHI_L2, new SignedWordElement(380)), //
//						m(BHKWChannelId.COSPHI_L3, new SignedWordElement(381)), //
						new DummyRegisterElement(379,381),//
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(382)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(383)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(384))) //
		);//
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
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}
}
