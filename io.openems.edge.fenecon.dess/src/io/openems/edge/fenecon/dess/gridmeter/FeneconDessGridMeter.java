package io.openems.edge.fenecon.dess.gridmeter;

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
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.fenecon.dess.FeneconDessConstants;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Fenecon.Dess.GridMeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID" //
		})
public class FeneconDessGridMeter extends AbstractOpenemsModbusComponent
		implements AsymmetricMeter, SymmetricMeter, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public FeneconDessGridMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);

		// automatically calculate Active/ReactivePower from L1/L2/L3
		AsymmetricMeter.initializePowerSumChannels(this);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), FeneconDessConstants.UNIT_ID, this.cm,
				"Modbus", config.modbus_id());
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

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(11136, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(11136),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(11137),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new UnsignedWordElement(11138), DELTA_10000), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new UnsignedWordElement(11139), DELTA_10000)), //
				new FC3ReadRegistersTask(11166, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(11166),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(11167),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new UnsignedWordElement(11168), DELTA_10000), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new UnsignedWordElement(11169), DELTA_10000)), //
				new FC3ReadRegistersTask(11196, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(11196),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(11197),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new UnsignedWordElement(11198), DELTA_10000), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new UnsignedWordElement(11199), DELTA_10000)) //
		); //
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	private final static ElementToChannelConverter DELTA_10000 = new ElementToChannelConverter( //
			// element -> channel
			value -> {
				if (value == null) {
					return null;
				}
				int intValue = (Integer) value;
				if (intValue == 0) {
					return 0; // ignore '0'
				}
				return (intValue - 10_000) * -1; // apply delta of 10_000 and invert
			}, //
				// channel -> element
			value -> value);

}