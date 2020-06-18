package io.openems.edge.fenecon.dess.charger;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public abstract class AbstractFeneconDessCharger extends AbstractOpenemsModbusComponent
		implements EssDcCharger, OpenemsComponent {

	public AbstractFeneconDessCharger() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				ChannelId.values() //
		);
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
		final int offset = this.getOffset();
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(offset + 2, Priority.LOW, //
						m(EssDcCharger.ChannelId.ACTUAL_POWER, new UnsignedWordElement(offset + 2)), //
						new DummyRegisterElement(offset + 3, offset + 4),
						m(EssDcCharger.ChannelId.ACTUAL_ENERGY,
								new UnsignedDoublewordElement(offset + 5).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.SCALE_FACTOR_3)) //
		);
	}

	@Override
	public String debugLog() {
		return "P:" + this.getActualPower().asString();
	}

	protected abstract int getOffset();

}