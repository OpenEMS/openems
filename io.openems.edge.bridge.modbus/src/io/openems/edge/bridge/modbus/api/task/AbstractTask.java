package io.openems.edge.bridge.modbus.api.task;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingSupplier;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * An abstract Modbus 'AbstractTask' is holding references to one or more Modbus
 * {@link ModbusElement} which have register addresses in the same range.
 */
public abstract non-sealed class AbstractTask<//
		REQUEST extends ModbusRequest, //
		RESPONSE extends ModbusResponse> implements Task {

	protected final String name;
	protected final Class<RESPONSE> responseClazz;
	protected final int startAddress;
	protected final int length;
	protected final ModbusElement<?>[] elements;

	private final Logger log = LoggerFactory.getLogger(AbstractTask.class);

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
	 * <p>
	 * Successful execution is produces a log message if {@link LogVerbosity} !=
	 * 'NONE' was configured. Errors are always logged.
	 * 
	 * @param bridge  the {@link AbstractModbusBridge}
	 * @param request the typed {@link ModbusRequest}
	 * @return the typed {@link ModbusResponse}
	 * @throws OpenemsException on error
	 */
	protected RESPONSE executeRequest(AbstractModbusBridge bridge, REQUEST request) throws OpenemsException {
		var unitId = this.getParent().getUnitId();
		var logVerbosity = this.getLogVerbosity(bridge);
		try {
			// First try
			return this.logRequest(bridge, logVerbosity, request,
					() -> sendRequest(bridge, unitId, this.responseClazz, request));

		} catch (OpenemsException e) {

			// Second try; with new connection
			bridge.closeModbusConnection();
			return this.logRequest(bridge, logVerbosity, request,
					() -> sendRequest(bridge, unitId, this.responseClazz, request));
		}
	}

	/**
	 * Logs the execution of a {@link ModbusRequest}.
	 * 
	 * @param bridge       the {@link BridgeModbus}
	 * @param logVerbosity the {@link LogVerbosity}
	 * @param request      the {@link ModbusRequest}
	 * @param supplier     {@link ThrowingSupplier} that executes the Request and
	 *                     returns a Response
	 * @return typed {@link ModbusResponse}
	 * @throws OpenemsException on error
	 */
	protected RESPONSE logRequest(BridgeModbus bridge, LogVerbosity logVerbosity, REQUEST request,
			ThrowingSupplier<RESPONSE, OpenemsException> supplier) throws OpenemsException {
		return switch (logVerbosity) {
		case NONE, DEBUG_LOG -> {
			try {
				yield supplier.get();

			} catch (OpenemsException e) {
				this.logError("Execute failed", "Error: " + e.getMessage());
				throw e;
			}
		}

		case READS_AND_WRITES -> {
			try {
				var response = supplier.get();
				this.logInfo("  Execute", "");
				yield response;

			} catch (OpenemsException e) {
				this.logError("  Execute", "Error: " + e.getMessage());
				throw e;
			}
		}

		case READS_AND_WRITES_DURATION, READS_AND_WRITES_DURATION_TRACE_EVENTS -> {
			var stopwatch = Stopwatch.createStarted();
			try {
				var response = supplier.get();
				stopwatch.stop();
				this.logInfo("  Execute", "Elapsed: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
				yield response;

			} catch (OpenemsException e) {
				stopwatch.stop();
				this.logError("  Execute",
						"Elapsed: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " Error: " + e.getMessage());
				throw e;
			}
		}

		case READS_AND_WRITES_VERBOSE -> {
			try {
				var response = supplier.get();
				this.logInfo("  Execute",
						"Request [" + request.getHexMessage() + "] Response [" + response.getHexMessage() + "]");
				yield response;

			} catch (OpenemsException e) {
				this.logError("  Execute", "Request [" + request.getHexMessage() + "] Error: " + e.getMessage());
				throw e;
			}
		}
		};
	}

	/*
	 * Enable Debug mode for this Element. Activates verbose logging.
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

	/**
	 * Combines the global and local (via {@link #isDebug} log verbosity.
	 *
	 * @param bridge the parent Bridge
	 * @return the combined LogVerbosity
	 */
	protected LogVerbosity getLogVerbosity(AbstractModbusBridge bridge) {
		if (this.isDebug) {
			return LogVerbosity.READS_AND_WRITES_VERBOSE;
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

	private void logInfo(String prefix, String suffix) {
		this.logInfo(this.log, prefix, suffix);
	}

	protected void logInfo(Logger log, String prefix, String suffix) {
		log.info(generateLogMessage(prefix, this.toString(), suffix));
	}

	private void logError(String prefix, String suffix) {
		this.logError(this.log, prefix, suffix);
	}

	protected void logError(Logger log, String prefix, String suffix) {
		log.error(generateLogMessage(prefix, this.toString(), suffix));
	}

	/**
	 * Generates a Log-Message string.
	 * 
	 * @param prefix a prefix
	 * @param id     identification string (e.g. toString())
	 * @param suffix a suffix
	 * @return the Log-Message
	 */
	protected static String generateLogMessage(String prefix, String id, String suffix) {
		var b = new StringBuilder();
		if (!prefix.isEmpty()) {
			b.append(prefix).append(" ");
		}
		b.append(id);
		if (!suffix.isEmpty()) {
			b.append(" ").append(suffix);
		}
		return b.toString();
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
	 * @throws OpenemsException on error
	 */
	private static <RESPONSE extends ModbusResponse> RESPONSE sendRequest(AbstractModbusBridge bridge, int unitId,
			Class<RESPONSE> clazz, ModbusRequest request) throws OpenemsException {
		request.setUnitID(unitId);
		var transaction = bridge.getNewModbusTransaction();
		transaction.setRequest(request);
		try {
			transaction.execute();
		} catch (ModbusException e) {
			throw new OpenemsException("Transaction failed: " + e.getMessage());
		}

		var response = transaction.getResponse();
		if (clazz.isInstance(response)) {
			return (RESPONSE) clazz.cast(response);
		}

		throw new OpenemsException("Unexpected Modbus response. " //
				+ "Expected [" + clazz.getSimpleName() + "] " //
				+ "Got [" + response.getClass().getSimpleName() + "]");
	}
}
