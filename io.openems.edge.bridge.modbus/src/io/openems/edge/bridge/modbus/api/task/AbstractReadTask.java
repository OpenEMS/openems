package io.openems.edge.bridge.modbus.api.task;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * An abstract Modbus 'AbstractTask' is holding references to one or more Modbus
 * {@link AbstractModbusElement} which have register addresses in the same
 * range.
 */
public abstract class AbstractReadTask<T> extends AbstractTask implements ReadTask {

	private final Logger log = LoggerFactory.getLogger(AbstractReadTask.class);
	private final Priority priority;
	private final Class<?> elementClazz;

	public AbstractReadTask(Class<?> elementClazz, int startAddress, Priority priority,
			AbstractModbusElement<?>... elements) {
		super(startAddress, elements);
		this.elementClazz = elementClazz;
		this.priority = priority;
	}

	@Override
	public int execute(AbstractModbusBridge bridge) throws OpenemsException {
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
				// Invalidate Elements
				Stream.of(this.elements).forEach(el -> el.invalidate(bridge));

				throw new OpenemsException("Transaction failed: " + e.getMessage(), e2);
			}
		}

		// Verify response length
		if (response.length < this.getLength()) {
			throw new OpenemsException("Received message is too short. Expected [" + this.getLength() + "], got ["
					+ response.length + "]");
		}

		this.fillElements(response);
		return 1;
	}

	protected T[] readElements(AbstractModbusBridge bridge) throws OpenemsException, ModbusException {
		var request = this.createModbusRequest(this.startAddress, this.length);
		int unitId = this.getParent().getUnitId();
		var response = Utils.getResponse(request, this.getParent().getUnitId(), bridge);

		var result = this.handleResponse(response);

		// debug output
		switch (this.getLogVerbosity(bridge)) {
		case READS_AND_WRITES:
			bridge.logInfo(this.log, this.getActiondescription() //
					+ " [" + unitId + ":" + this.startAddress + "/0x" + Integer.toHexString(this.startAddress) + "]: " //
					+ Arrays.stream(result).map(r -> {
						if (r instanceof InputRegister) {
							return String.format("%4s", Integer.toHexString(((InputRegister) r).getValue()))
									.replace(' ', '0');
						}
						if (r instanceof Boolean) {
							return (Boolean) r ? "x" : "-";
						} else {
							return r.toString();
						}
					}) //
							.collect(Collectors.joining(" ")));
			break;
		case WRITES:
		case DEV_REFACTORING:
		case NONE:
			break;
		}

		return result;
	}

	protected void fillElements(T[] response) {
		var position = 0;

		for (var element : this.elements) {
			if (this.elementClazz.isInstance(element)) {
				try {
					this.doElementSetInput(element, position, response);
				} catch (OpenemsException e) {
					this.log.warn("Unable to fill modbus element. UnitId [" + this.getParent().getUnitId()
							+ "] Address [" + this.getStartAddress() + "] Length [" + this.getLength() + "]: "
							+ e.getMessage());
				}

			} else {
				this.log.error("A " + this.elementClazz.getSimpleName() + " is required for a "
						+ this.getActiondescription() + "Task! Element [" + element + "]");
			}
			position = this.increasePosition(position, element);
		}
	}

	@Override
	public Priority getPriority() {
		return this.priority;
	}

	protected abstract int increasePosition(int position, ModbusElement<?> modbusElement);

	protected abstract void doElementSetInput(ModbusElement<?> modbusElement, int position, T[] response)
			throws OpenemsException;

	/**
	 * Factory for a {@link ModbusRequest}.
	 * 
	 * @param startAddress the startAddress of the modbus register
	 * @param length       the length
	 * @return a new {@link ModbusRequest}
	 */
	protected abstract ModbusRequest createModbusRequest(int startAddress, int length);

	protected abstract T[] handleResponse(ModbusResponse response) throws OpenemsException;

}
