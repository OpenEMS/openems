package io.openems.edge.goodwe.et.charger;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public abstract class AbstractGoodWeEtCharger extends AbstractOpenemsModbusComponent
		implements EssDcCharger, OpenemsComponent {

	protected AbstractGoodWeEtCharger() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				PvChannelId.values() //
		);
	}

	@Override
	protected final ModbusProtocol defineModbusProtocol() {
		int startAddress = this.getStartAddress();
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(startAddress, Priority.LOW, //
						m(PvChannelId.V, new UnsignedWordElement(startAddress),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(PvChannelId.I, new UnsignedWordElement(startAddress + 1),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(EssDcCharger.ChannelId.ACTUAL_POWER, new UnsignedDoublewordElement(startAddress + 2))));
	}

	@Override
	public final String debugLog() {
		return "L:" + this.getActualPower().value().asString();
	}

	protected abstract int getStartAddress();
}
