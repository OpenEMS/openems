package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.ModbusException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.taskmanager.ManagedTask;

/**
 * A Modbus 'ReadTask' is holding references to one or more Modbus
 * {@link AbstractModbusElement} which have register addresses in the same
 * range. The ReadTask handles the execution (query) on this range. @{link
 * WriteTask} inherits from ReadTask.
 * 
 * @author stefan.feilmeier
 */
public interface ReadTask extends Task, ManagedTask {

	/**
	 * Sends a query for this AbstractTask to the Modbus device
	 * 
	 * @param bridge
	 * @param unitId
	 * @throws ModbusException
	 */
	public abstract <T> void executeQuery(AbstractModbusBridge bridge) throws OpenemsException;
}
