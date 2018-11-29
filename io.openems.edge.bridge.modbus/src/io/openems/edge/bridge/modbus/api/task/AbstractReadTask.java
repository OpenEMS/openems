package io.openems.edge.bridge.modbus.api.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * An abstract Modbus 'AbstractTask' is holding references to one or more Modbus
 * {@link AbstractModbusElement} which have register addresses in the same
 * range.
 * 
 * @author stefan.feilmeier
 */
public abstract class AbstractReadTask<T> extends AbstractTask implements ReadTask {

	private final Logger log = LoggerFactory.getLogger(AbstractReadTask.class);

	private final Priority priority;

	public AbstractReadTask(int startAddress, Priority priority, AbstractModbusElement<?>... elements) {
		super(startAddress, elements);
		this.priority = priority;
	}

	public void executeQuery(AbstractModbusBridge bridge) throws OpenemsException {
		T[] response;
		try {
			/*
			 * First try
			 */
			response = this.readElements(bridge);
		} catch (OpenemsException | ModbusException e) {
			/*
			 * Second try: with new connection
			 */
			bridge.closeModbusConnection();
			try {
				response = this.readElements(bridge);
			} catch (ModbusException e2) {
				for (ModbusElement<?> elem : this.getElements()) {
					if (!elem.isIgnored()) {
						elem.invalidate();
					}
				}
				throw new OpenemsException("Transaction failed: " + e.getMessage(), e2);
			}
		}

		// Verify response length
		if (response.length < getLength()) {
			throw new OpenemsException(
					"Received message is too short. Expected [" + getLength() + "], got [" + response.length + "]");
		}

		fillElements(response);
	}

	protected T[] readElements(AbstractModbusBridge bridge) throws OpenemsException, ModbusException {
		ModbusRequest request = getRequest();
		ModbusResponse response = Utils.getResponse(request, this.getParent().getUnitId(), bridge);
		return handleResponse(response);
	}

	protected void fillElements(T[] response) {
		int position = 0;
		for (ModbusElement<?> modbusElement : this.getElements()) {
			if (!(isCorrectElementInstance(modbusElement))) {
				doErrorLog(modbusElement);
			} else {
				try {
					if (!modbusElement.isIgnored()) {
						doElementSetInput(modbusElement, position, response);
					}
				} catch (OpenemsException e) {
					doWarnLog(e);
				}
			}
			position = increasePosition(position, modbusElement);
		}
	}

	public Priority getPriority() {
		return priority;
	}

	protected abstract int increasePosition(int position, ModbusElement<?> modbusElement);

	protected abstract void doElementSetInput(ModbusElement<?> modbusElement, int position, T[] response)
			throws OpenemsException;

	protected abstract String getRequiredElementName();

	protected abstract boolean isCorrectElementInstance(ModbusElement<?> modbusElement);

	protected abstract ModbusRequest getRequest();

	protected abstract T[] handleResponse(ModbusResponse response) throws OpenemsException;

	private void doWarnLog(OpenemsException e) {
		log.warn("Unable to fill modbus element. UnitId [" + this.getParent().getUnitId() + "] Address ["
				+ getStartAddress() + "] Length [" + getLength() + "]: " + e.getMessage());
	}

	private void doErrorLog(ModbusElement<?> modbusElement) {
		log.error("A " + getRequiredElementName() + " is required for a " + getActiondescription() + "Task! Element ["
				+ modbusElement + "]");
	}
}
