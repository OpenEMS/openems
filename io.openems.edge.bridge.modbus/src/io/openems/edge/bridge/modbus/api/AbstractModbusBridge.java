package io.openems.edge.bridge.modbus.api;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.worker.ModbusWorker;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
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

	private LogVerbosity logVerbosity = LogVerbosity.NONE;
	private int invalidateElementsAfterReadErrors = 1;

	protected final ModbusWorker worker = new ModbusWorker(this);

	protected AbstractModbusBridge(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled,
			LogVerbosity logVerbosity, int invalidateElementsAfterReadErrors) {
		super.activate(context, id, alias, enabled);
		this.logVerbosity = logVerbosity;
		this.invalidateElementsAfterReadErrors = invalidateElementsAfterReadErrors;
		if (this.isEnabled()) {
			this.worker.activate(id);
		}
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
		this.closeModbusConnection();
	}

	/**
	 * Gets the {@link Cycle}.
	 * 
	 * @return the Cycle
	 */
	public abstract Cycle getCycle();

	/**
	 * Adds the protocol.
	 *
	 * @param sourceId Component-ID of the source
	 * @param protocol the ModbusProtocol
	 */
	@Override
	public void addProtocol(String sourceId, ModbusProtocol protocol) {
		this.worker.addProtocol(sourceId, protocol);
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

	public LogVerbosity getLogVerbosity() {
		return this.logVerbosity;
	}

	@Override
	public void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	public int invalidateElementsAfterReadErrors() {
		return this.invalidateElementsAfterReadErrors;
	}
}
