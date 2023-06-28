package io.openems.edge.bridge.modbus.api.task;

import org.slf4j.Logger;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * An abstract Modbus 'AbstractTask' is holding references to one or more Modbus
 * {@link ModbusElement} which have register addresses in the same range.
 */
public non-sealed abstract class AbstractTask<//
		REQUEST extends ModbusRequest, //
		RESPONSE extends ModbusResponse> implements Task {

//	private final Logger log = LoggerFactory.getLogger(AbstractTask.class);

	protected final String name;
	protected final Class<RESPONSE> responseClazz;
	protected final int startAddress;
	protected final int length;
	protected final ModbusElement<?>[] elements;

	private AbstractOpenemsModbusComponent parent = null; // this is always set by ModbusProtocol.addTask()

	public AbstractTask(String name, Class<RESPONSE> responseClazz, int startAddress, ModbusElement<?>... elements) {
		this.name = name;
		this.responseClazz = responseClazz;
		this.startAddress = startAddress;
		this.elements = elements;
		for (var element : elements) {
			element.setModbusTask(this);
		}
		var length = 0;
		for (var element : elements) {
			length += element.getLength();
		}
		this.length = length;
	}

	// Override for Task.getElements()
	public ModbusElement<?>[] getElements() {
		return this.elements;
	}

	// Override for Task.getLength()
	public int getLength() {
		return this.length;
	}

	// Override for Task.getStartAddress()
	public int getStartAddress() {
		return this.startAddress;
	}

	public void setParent(AbstractOpenemsModbusComponent parent) {
		this.parent = parent;
	}

	public AbstractOpenemsModbusComponent getParent() {
		return this.parent;
	}

	/**
	 * Executes the tasks - i.e. sends the query of a ReadTask or writes a
	 * WriteTask.
	 *
	 * @param bridge the Modbus-Bridge
	 * @return the number of executed Sub-Tasks
	 */
	public abstract ExecuteState execute(AbstractModbusBridge bridge);

	/**
	 * Actually executes a {@link ModbusRequest} and returns its
	 * {@link ModbusResponse}.
	 * 
	 * <p>
	 * If first request fails, the implementation reconnects the Modbus connection
	 * and tries again.
	 * 
	 * @param bridge  the {@link AbstractModbusBridge}
	 * @param request the typed {@link ModbusRequest}
	 * @return the typed {@link ModbusResponse}
	 * @throws OpenemsException on error
	 */
	protected RESPONSE executeRequest(AbstractModbusBridge bridge, REQUEST request) throws OpenemsException {
		// TODO reicht BridgeModbus?
		var unitId = this.getParent().getUnitId();
		var logVerbosity = this.getLogVerbosity(bridge);
		try {
			// First try
			return sendRequest(bridge, unitId, this.responseClazz, request, logVerbosity);

		} catch (OpenemsException | ModbusException e) {

			try {
				// Second try; with new connection
				bridge.closeModbusConnection();
				return sendRequest(bridge, unitId, this.responseClazz, request, logVerbosity);

			} catch (ModbusException e2) {
				throw new OpenemsException("Transaction failed: " + e.getMessage(), e2);
			}
		}
	}

	/*
	 * Enable Debug mode for this Element. Activates verbose logging. TODO:
	 * implement debug write in all implementations (FC16 is already done)
	 */
	private boolean isDebug = false;

	/**
	 * Activate Debug-Mode.
	 * 
	 * @return myself
	 */
	public AbstractTask<REQUEST, RESPONSE> debug() {
		this.isDebug = true;
		return this;
	}

	public boolean isDebug() {
		return this.isDebug;
	}

	/**
	 * Combines the global and local (via {@link #isDebug} log verbosity.
	 *
	 * @param bridge the parent Bridge
	 * @return the combined LogVerbosity
	 */
	protected LogVerbosity getLogVerbosity(AbstractModbusBridge bridge) {
		if (this.isDebug) {
			return LogVerbosity.READS_AND_WRITES;
		}
		return bridge.getLogVerbosity();
	}

	/**
	 * Deactivate.
	 */
	public void deactivate() {
		for (ModbusElement<?> element : this.elements) {
			element.deactivate();
		}
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(this.name);
		sb.append(" [");
		sb.append(this.parent.id());
		sb.append(";unitid=");
		sb.append(this.parent.getUnitId());
		sb.append(";ref=");
		sb.append(this.startAddress);
		sb.append("/0x");
		sb.append(Integer.toHexString(this.startAddress));
		sb.append(";length=");
		sb.append(this.length);
		sb.append("]");
		return sb.toString();
	}

	protected void logError(AbstractModbusBridge bridge, Logger log, String message) {
		OpenemsComponent.logError(bridge, log, "[" + this.name + "] " + message);
	}

	protected void logInfo(AbstractModbusBridge bridge, Logger log, String message) {
		OpenemsComponent.logInfo(bridge, log, "[" + this.name + "] " + message);
	}

	/**
	 * Sends a {@link ModbusRequest} and returns the {@link ModbusResponse}.
	 * 
	 * @param <RESPONSE> the type of the response
	 * @param bridge     the {@link AbstractModbusBridge}
	 * @param unitId     the Modbus Unit-ID
	 * @param clazz      the class of the response
	 * @param request    the {@link ModbusRequest}
	 * @return the {@link ModbusResponse}
	 */
	private static <RESPONSE extends ModbusResponse> RESPONSE sendRequest(AbstractModbusBridge bridge, int unitId,
			Class<RESPONSE> clazz, ModbusRequest request, LogVerbosity logVerbosity)
			throws OpenemsException, ModbusException {
//		try {
		request.setUnitID(unitId);
		var transaction = bridge.getNewModbusTransaction();
		transaction.setRequest(request);
		transaction.execute();

		var response = transaction.getResponse();
		if (clazz.isInstance(response)) {
			return (RESPONSE) clazz.cast(response);
		}

		// TODO schöne Lösung für Logs; je nach Log-Level können unterschiedliche Infos
		// angezeigt werden (z. B. Dauer, detaillierte Hex Request/Response)
//			
//		} finally {
//			switch (logVerbosity) {
//			case READS_AND_WRITES, WRITES -> 
//			
//			}
////			case READS_AND_WRITES:
////				bridge.logInfo(this.log, this.name //
////						+ " [" + unitId + ":" + startAddress + "/0x" + Integer.toHexString(startAddress) + "]: " //
////						+ Arrays.stream(registers) //
////								.map(r -> String.format("%4s", Integer.toHexString(r.getValue())).replace(' ', '0')) //
////								.collect(Collectors.joining(" ")));
////				break;
////			case WRITES:
////			case DEV_REFACTORING:
////			case NONE:
////				break;
////			}
//		}
		throw new OpenemsException("Unexpected Modbus response. " //
				+ "Expected [" + clazz.getSimpleName() + "] " //
				+ "Got [" + response.getClass().getSimpleName() + "]");
	}
}
