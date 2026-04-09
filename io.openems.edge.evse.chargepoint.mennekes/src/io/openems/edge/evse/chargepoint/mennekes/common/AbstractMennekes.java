package io.openems.edge.evse.chargepoint.mennekes.common;

import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evse.chargepoint.bender.AbstractEvseChargePointBender;
import io.openems.edge.meter.api.ElectricityMeter;

public abstract class AbstractMennekes extends AbstractEvseChargePointBender implements Mennekes, ElectricityMeter {

	protected AbstractMennekes(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var modbusProtocol = super.defineModbusProtocol();
		modbusProtocol.addTask(new FC3ReadRegistersTask(1000, Priority.LOW,
				m(Mennekes.ChannelId.EMS_CURRENT_LIMIT, new UnsignedWordElement(1000))));

		if (!this.isReadOnly()) {
			modbusProtocol.addTasks(//
					new FC16WriteRegistersTask(1000,
							m(Mennekes.ChannelId.SET_CURRENT_LIMIT, new UnsignedWordElement(1000))));
		}

		return modbusProtocol;
	}

	/**
	 * Is the chargePoint readOnly.
	 * 
	 * @return config readOnly value
	 */
	public abstract boolean isReadOnly();
}
