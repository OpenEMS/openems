package io.openems.edge.fenecon.mini.gridmeter;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.fenecon.mini.FeneconMiniConstants;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Fenecon.Mini.GridMeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
)
public class FeneconMiniGridMeter extends AbstractOpenemsModbusComponent implements SymmetricMeter, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
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

	public FeneconMiniGridMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), FeneconMiniConstants.UNIT_ID, this.cm,
				"Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(4004, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(4004),
								SIGNED_POWER_CONVERTER_AND_INVERT)), //
				new FC3ReadRegistersTask(5003, Priority.LOW, //
						m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new UnsignedDoublewordElement(5003).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new UnsignedDoublewordElement(5005).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.SCALE_FACTOR_2)) //
		);
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

	private final static ElementToChannelConverter SIGNED_POWER_CONVERTER_AND_INVERT = new ElementToChannelConverter( //
			// element -> channel
			value -> {
				if (value == null) {
					return null;
				}
				int intValue = (Short) value;
				if (intValue == -10_000) {
					return 0; // ignore '-10_000'
				}
				return intValue * -1; // invert
			}, //
				// channel -> element
			value -> value);
}
