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
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
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
				BHKWChannelId.values());
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
				new FC3ReadRegistersTask(370, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(370),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(371, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(371),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(372, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(372),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(373, Priority.LOW, //
						m(BHKWChannelId.FREQUENCY_L1, new UnsignedDoublewordElement(373),
								ElementToChannelConverter.SCALE_FACTOR_1)), //
				new FC3ReadRegistersTask(374, Priority.LOW, //
						m(BHKWChannelId.FREQUENCY_L2, new UnsignedDoublewordElement(374),
								ElementToChannelConverter.SCALE_FACTOR_1)), //
				new FC3ReadRegistersTask(375, Priority.LOW, //
						m(BHKWChannelId.FREQUENCY_L3, new UnsignedDoublewordElement(375),
								ElementToChannelConverter.SCALE_FACTOR_1)), //
				new FC3ReadRegistersTask(376, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(376),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(377, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(377),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(378, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(378),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(379, Priority.LOW, //
						m(BHKWChannelId.COSPHI_L1, new SignedDoublewordElement(379))), //
				new FC3ReadRegistersTask(380, Priority.LOW, //
						m(BHKWChannelId.COSPHI_L2, new SignedDoublewordElement(380))), //
				new FC3ReadRegistersTask(381, Priority.LOW, //
						m(BHKWChannelId.COSPHI_L3, new SignedDoublewordElement(381))), //
				new FC3ReadRegistersTask(382, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(382))), //
				new FC3ReadRegistersTask(383, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(383))), //
				new FC3ReadRegistersTask(384, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(384)))//
		);//
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}
}
