package io.openems.edge.bridge.modbus.api;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.worker.ModbusWorker;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

/**
 * Abstract service for connecting to, querying and writing to a Modbus device.
 */
public abstract class AbstractModbusBridge extends AbstractOpenemsComponent implements BridgeModbus, EventHandler {

	/**
	 * Default Modbus timeout in [ms].
	 *
	 * <p>
	 * Modbus library default is 3000 ms
	 */
	protected static final int DEFAULT_TIMEOUT = 1000;

	/**
	 * Default Modbus retries.
	 *
	 * <p>
	 * Modbus library default is 5
	 */
	protected static final int DEFAULT_RETRIES = 1;

	private final AtomicReference<LogVerbosity> logVerbosity = new AtomicReference<>(LogVerbosity.NONE);
	private int invalidateElementsAfterReadErrors = 1;

	protected final ModbusWorker worker = new ModbusWorker(
			// Execute Task
			task -> task.execute(this),
			// Invalidate ModbusElements
			elements -> Stream.of(elements).forEach(e -> e.invalidate(this)),
			// Set ChannelId.CYCLE_TIME_IS_TOO_SHORT
			state -> this._setCycleTimeIsTooShort(state),
			// Set ChannelId.CYCLE_DELAY
			cycleDelay -> this._setCycleDelay(cycleDelay),
			// LogVerbosity
			this.logVerbosity //
	);

	protected AbstractModbusBridge(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	@Deprecated
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled,
			LogVerbosity logVerbosity, int invalidateElementsAfterReadErrors) {
		super.activate(context, id, alias, enabled);
		this.applyConfig(logVerbosity, invalidateElementsAfterReadErrors);
		if (enabled) {
			this.worker.activate(id);
		}
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
		this.closeModbusConnection();
	}

	@Override
	@Deprecated
	protected void modified(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other modified() method.");
	}

	protected void modified(ComponentContext context, String id, String alias, boolean enabled,
			LogVerbosity logVerbosity, int invalidateElementsAfterReadErrors) {
		super.modified(context, id, alias, enabled);
		this.applyConfig(logVerbosity, invalidateElementsAfterReadErrors);
		if (enabled) {
			this.worker.modified(id);
		} else {
			this.worker.deactivate();
		}
	}

	private void applyConfig(LogVerbosity logVerbosity, int invalidateElementsAfterReadErrors) {
		this.logVerbosity.set(logVerbosity);
		this.invalidateElementsAfterReadErrors = invalidateElementsAfterReadErrors;
	}

	/**
	 * Adds the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 * @param protocol the ModbusProtocol
	 */
	@Override
	public void addProtocol(String sourceId, ModbusProtocol protocol) {
		this.worker.addProtocol(sourceId, protocol);
		this.retryModbusCommunication(sourceId);
	}

	/**
	 * Removes the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 */
	@Override
	public void removeProtocol(String sourceId) {
		this.worker.removeProtocol(sourceId);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.worker.onBeforeProcessImage();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.worker.onExecuteWrite();
			break;
		}
	}

	@Override
	public String debugLog() {
		return switch (this.logVerbosity.get()) {
		case NONE -> //
			null;
		case DEBUG_LOG, READS_AND_WRITES, READS_AND_WRITES_DURATION, READS_AND_WRITES_VERBOSE,
				READS_AND_WRITES_DURATION_TRACE_EVENTS -> //
			"CycleDelay:" + this.getCycleDelay().asString(); //
		};
	}

	/**
	 * Creates a new Modbus Transaction on an open Modbus connection.
	 *
	 * @return the Modbus Transaction
	 * @throws OpenemsException on error
	 */
	public abstract ModbusTransaction getNewModbusTransaction() throws OpenemsException;

	/**
	 * Closes the Modbus connection.
	 */
	public abstract void closeModbusConnection();

	/**
	 * Gets the configured {@link LogVerbosity}.
	 * 
	 * @return {@link LogVerbosity}
	 */
	public LogVerbosity getLogVerbosity() {
		return this.logVerbosity.get();
	}

	/**
	 * Gets the configured max number of errors before an element should be
	 * invalidated?.
	 *
	 * @return value
	 */
	public int invalidateElementsAfterReadErrors() {
		return this.invalidateElementsAfterReadErrors;
	}

	@Override
	public void retryModbusCommunication(String sourceId) {
		this.worker.retryModbusCommunication(sourceId);
	}
}
