package io.openems.edge.bridge.modbus.api.task;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusSlaveException;
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
 * {@link ModbusElement}s which have register addresses in the same range.
 */
public abstract non-sealed class AbstractTask<//
		REQUEST extends ModbusRequest, //
		RESPONSE extends ModbusResponse> implements Task {

	protected final String name;
	protected final Class<RESPONSE> responseClazz;
	protected final int startAddress;
	protected final int length;
	protected final ModbusElement[] elements;

	private final Logger log = LoggerFactory.getLogger(AbstractTask.class);

	private AbstractOpenemsModbusComponent parent = null; // this is always set by ModbusProtocol.addTask()

	public AbstractTask(String name, Class<RESPONSE> responseClazz, int startAddress, ModbusElement... elements) {
		this.name = name;
		this.responseClazz = responseClazz;
		this.startAddress = startAddress;
		this.elements = elements;
		var nextStartAddress = startAddress;
		var length = 0;
		for (var element : elements) {
			if (element.startAddress != nextStartAddress) {
				throw new IllegalArgumentException("StartAddress for Modbus Element wrong. " //
						+ "Got [" + element.startAddress + "/0x" + Integer.toHexString(element.startAddress) + "] " //
						+ "Expected [" + nextStartAddress + "/0x" + Integer.toHexString(nextStartAddress) + "]");
			}
			nextStartAddress += element.length;
			length += element.length;
			element.setModbusTask(this);
		}
		this.length = length;
	}

	// Override for Task.getElements()
	public ModbusElement[] getElements() {
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
	protected RESPONSE executeRequest(AbstractModbusBridge bridge, REQUEST request) throws Exception {
		var unitId = this.getParent().getUnitId();
		var logVerbosity = this.getLogVerbosity(bridge);
		try {
			// First try
			return this.logRequest(TryExecute.FIRST_TRY, bridge, logVerbosity, request,
					() -> sendRequest(bridge, unitId, this.responseClazz, request));

		} catch (Exception e) {
			// Second try; with new connection
			bridge.closeModbusConnection();
			return this.logRequest(TryExecute.SECOND_TRY, bridge, logVerbosity, request,
					() -> sendRequest(bridge, unitId, this.responseClazz, request));
		}
	}

	private static enum TryExecute {
		FIRST_TRY, SECOND_TRY
	}

	/**
	 * Logs the execution of a {@link ModbusRequest}.
	 * 
	 * @param tryExecute   marker for execute first/second try
	 * @param bridge       the {@link BridgeModbus}
	 * @param logVerbosity the {@link LogVerbosity}
	 * @param request      the {@link ModbusRequest}
	 * @param supplier     {@link ThrowingSupplier} that executes the Request and
	 *                     returns a Response
	 * @return typed {@link ModbusResponse}
	 * @throws Exception on error
	 */
	protected RESPONSE logRequest(TryExecute tryExecute, BridgeModbus bridge, LogVerbosity logVerbosity,
			REQUEST request, ThrowingSupplier<RESPONSE, Exception> supplier) throws Exception {
		return switch (logVerbosity) {
		case NONE, DEBUG_LOG -> {
			yield switch (tryExecute) {
			case FIRST_TRY ->
				// On first try: do not log error in low LogVerbosity
				supplier.get();
			case SECOND_TRY -> {
				// On second try: always log error
				try {
					yield supplier.get();
				} catch (Exception e) {
					this.logError(e, "Execute failed", this.toLogMessage(logVerbosity, request, e));
					throw e;
				}
			}
			};
		}

		case READS_AND_WRITES, READS_AND_WRITES_VERBOSE -> {
			try {
				var response = supplier.get();
				this.logInfo("  Execute", this.toLogMessage(logVerbosity, request, response));
				yield response;

			} catch (Exception e) {
				this.logError(e, "  Execute failed", this.toLogMessage(logVerbosity, request, e));
				throw e;
			}
		}

		case READS_AND_WRITES_DURATION, READS_AND_WRITES_DURATION_TRACE_EVENTS -> {
			var stopwatch = Stopwatch.createStarted();
			try {
				var response = supplier.get();
				stopwatch.stop();
				this.logInfo("  Execute", this.toLogMessage(logVerbosity, request, response),
						"Elapsed [" + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms]");
				yield response;

			} catch (Exception e) {
				stopwatch.stop();
				this.logError(e, "  Execute failed", this.toLogMessage(logVerbosity, request, e),
						"Elapsed [" + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms]");
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
		for (ModbusElement element : this.elements) {
			element.deactivate();
		}
	}

	private void logInfo(String... messages) {
		logInfo(this.log, messages);
	}

	protected static void logInfo(Logger log, String... messages) {
		log(log, Logger::info, messages);
	}

	private void logError(Exception e, String... messages) {
		logError(this.log, e, messages);
	}

	protected static void logError(Logger log, Exception e, String... messages) {
		messages = Arrays.copyOf(messages, messages.length + 1);
		messages[messages.length - 1] = e.getClass().getSimpleName() + ": " + e.getMessage();
		log(log, Logger::error, messages);
	}

	private static void log(Logger log, BiConsumer<Logger, String> logger, String... messages) {
		logger.accept(log, String.join(" ", messages));
	}

	protected final String toLogMessage(LogVerbosity logVerbosity, REQUEST request, Exception e) {
		return this.toLogMessage(logVerbosity, request, null, e);
	}

	protected final String toLogMessage(LogVerbosity logVerbosity, REQUEST request, RESPONSE response) {
		return this.toLogMessage(logVerbosity, request, response, null);
	}

	// This method has an @Override in FC16WriteRegistersTask
	protected String toLogMessage(LogVerbosity logVerbosity, REQUEST request, RESPONSE response, Exception e) {
		return this.toLogMessage(logVerbosity, this.startAddress, this.length, request, response, e);
	}

	/**
	 * Generates a log message for this task.
	 * 
	 * <p>
	 * StartAddress and length need to be provided explicitly, because FC16 task
	 * might be split to multiple requests.
	 * 
	 * <p>
	 * For certain Exceptions we internally increase the LogVerbosity to always show
	 * helpful information
	 * 
	 * @param logVerbosity the {@link LogVerbosity}
	 * @param startAddress the start address of the request
	 * @param length       the length of the request payload
	 * @param request      the {@link ModbusRequest}
	 * @param response     the {@link ModbusResponse}, possibly null
	 * @param exception    a {@link Exception}, possibly null
	 * @return a log message String
	 */
	protected final String toLogMessage(LogVerbosity logVerbosity, int startAddress, int length, REQUEST request,
			RESPONSE response, Exception exception) {
		// Handle Exception
		if (exception != null) {
			if (exception instanceof ModbusSlaveException e && e.isType(Modbus.ILLEGAL_VALUE_EXCEPTION)) {
				// In this case it is helpful to get see the detailed request payload
				logVerbosity = LogVerbosity.READS_AND_WRITES_VERBOSE;
			}
		}

		// Build log message
		var b = new StringBuilder() //
				.append(this.name) //
				.append(" [") //
				.append(this.parent.id()) //
				.append(";unitid=").append(this.parent.getUnitId()); //
		if (!(this instanceof WriteTask)) { // WriteTasks anyway default to HIGH priority
			b.append(";priority=").append(this.getPriority());
		}
		b //
				.append(";ref=").append(startAddress).append("/0x").append(Integer.toHexString(startAddress)) //
				.append(";length=").append(length); //
		switch (logVerbosity) {
		case NONE, DEBUG_LOG, READS_AND_WRITES, READS_AND_WRITES_DURATION, READS_AND_WRITES_DURATION_TRACE_EVENTS -> {
		}
		case READS_AND_WRITES_VERBOSE -> {
			if (request != null) {
				var hexString = this.payloadToString(request);
				if (!hexString.isBlank()) {
					b.append(";request=").append(hexString);
				}
			}
			if (response != null) {
				var hexString = this.payloadToString(response);
				if (!hexString.isBlank()) {
					b.append(";response=").append(hexString);
				}
			}
		}
		}
		return b //
				.append("]") //
				.toString();
	}

	/**
	 * Converts the actual payload of the REQUEST to a human readable format
	 * suitable for logs; without header data (like Unit-ID, function code,
	 * checksum, etc).
	 * 
	 * @param request the request
	 * @return a string
	 */
	protected abstract String payloadToString(REQUEST request);

	/**
	 * Converts the actual payload of the RESPONSE to a human readable format
	 * suitable for logs; without header data (like Unit-ID, function code,
	 * checksum, etc).
	 * 
	 * @param response the response
	 * @return a string
	 */
	protected abstract String payloadToString(RESPONSE response);

	/**
	 * Sends a {@link ModbusRequest} and returns the {@link ModbusResponse}.
	 * 
	 * @param <RESPONSE> the type of the response
	 * @param bridge     the {@link AbstractModbusBridge}
	 * @param unitId     the Modbus Unit-ID
	 * @param clazz      the class of the response
	 * @param request    the {@link ModbusRequest}
	 * @return the {@link ModbusResponse}
	 * @throws Exception on error
	 */
	private static <RESPONSE extends ModbusResponse> RESPONSE sendRequest(AbstractModbusBridge bridge, int unitId,
			Class<RESPONSE> clazz, ModbusRequest request) throws Exception {
		request.setUnitID(unitId);
		var transaction = bridge.getNewModbusTransaction();
		transaction.setRequest(request);
		transaction.execute();

		var response = transaction.getResponse();
		if (clazz.isInstance(response)) {
			return (RESPONSE) clazz.cast(response);
		}

		throw new OpenemsException("Unexpected Modbus response. " //
				+ "Expected [" + clazz.getSimpleName() + "] " //
				+ "Got [" + response.getClass().getSimpleName() + "]");
	}
}
