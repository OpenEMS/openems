package io.openems.edge.fenecon.mini.gridmeter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
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

	public FeneconMiniGridMeter() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), FeneconMiniConstants.UNIT_ID,
				this.cm, "Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(4004, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(4004))), //
				new FC3ReadRegistersTask(5003, Priority.HIGH, //
						m(FeneconMiniGridMeter.ChannelId.SELL_TO_GRID_ENERGY, new UnsignedDoublewordElement(5003)), //
						m(FeneconMiniGridMeter.ChannelId.BUY_FROM_GRID_ENERGY, new UnsignedDoublewordElement(5005))));//
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {

		SELL_TO_GRID_ENERGY(new Doc().unit(Unit.WATT_HOURS)), //
		BUY_FROM_GRID_ENERGY(new Doc().unit(Unit.WATT_HOURS));//

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
		return MeterType.GRID;
	}

	@Override
	public String debugLog() {
		return "A:" + this.getActivePower().value().asString();
	}
}
