package io.openems.edge.fenecon.dess.charger;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public abstract class AbstractFeneconDessCharger extends AbstractOpenemsModbusComponent
		implements EssDcCharger, OpenemsComponent {

	public AbstractFeneconDessCharger() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
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
		final int ADDR = this.getOffset();
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(ADDR + 2, Priority.LOW, //
						m(EssDcCharger.ChannelId.ACTUAL_POWER, new UnsignedWordElement(ADDR + 2)), //
						new DummyRegisterElement(ADDR + 3, ADDR + 4),
						m(EssDcCharger.ChannelId.ACTUAL_ENERGY,
								new UnsignedDoublewordElement(ADDR + 5).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.SCALE_FACTOR_3)) //
		); //
	}

	@Override
	public String debugLog() {
		return "P:" + this.getActualPower().value().asString();
	}

	protected abstract int getOffset();

}