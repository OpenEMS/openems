package io.openems.edge.bridge.modbus.api.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;

/**
 * A ModbusElement represents one row of a Modbus definition table.
 *
 * @param <T> the target OpenemsType
 */
public abstract class AbstractModbusElement<T> implements ModbusElement<T> {

	protected final List<Consumer<Optional<T>>> onSetNextWriteCallbacks = new ArrayList<>();

	private final Logger log = LoggerFactory.getLogger(AbstractModbusElement.class);

	private final OpenemsType type;
	private final int startAddress;

	// Counts for how many cycles no valid value was
	private int invalidValueCounter = 0;

	protected AbstractTask abstractTask = null;

	public AbstractModbusElement(OpenemsType type, int startAddress) {
		this.type = type;
		this.startAddress = startAddress;
	}

	@Override
	public final void onSetNextWrite(Consumer<Optional<T>> callback) {
		this.onSetNextWriteCallbacks.add(callback);
	}

	@Override
	public OpenemsType getType() {
		return this.type;
	}

	private final List<Consumer<T>> onUpdateCallbacks = new CopyOnWriteArrayList<>();

	/**
	 * The onUpdateCallback is called on reception of a new value.
	 *
	 * <p>
	 * Be aware, that this is the original, untouched value.
	 * ChannelToElementConverters are not applied here yet!
	 *
	 * @param onUpdateCallback the Callback
	 * @return myself
	 */
	public AbstractModbusElement<T> onUpdateCallback(Consumer<T> onUpdateCallback) {
		this.onUpdateCallbacks.add(onUpdateCallback);
		return this;
	}

	@Override
	public int getStartAddress() {
		return this.startAddress;
	}

	@Override
	public void setModbusTask(AbstractTask abstractTask) {
		this.abstractTask = abstractTask;
	}

	public AbstractTask getModbusTask() {
		return this.abstractTask;
	}

	protected void setValue(T value) {
		if (this.isDebug) {
			this.log.info("Element [" + this + "] set value to [" + value + "].");
		}
		if (value != null) {
			this.invalidValueCounter = 0;
		}
		for (Consumer<T> callback : this.onUpdateCallbacks) {
			callback.accept(value);
		}
	}

	@Override
	public void invalidate(AbstractModbusBridge bridge) {
		this.invalidValueCounter++;
		if (bridge.invalidateElementsAfterReadErrors() <= this.invalidValueCounter) {
			this.setValue(null);
		}
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
	public AbstractModbusElement<T> debug() {
		this.isDebug = true;
		return this;
	}

	protected boolean isDebug() {
		return this.isDebug;
	}

	@Override
	public String toString() {
		return this.startAddress + "/0x" + Integer.toHexString(this.startAddress);
	}

	@Override
	public void deactivate() {
		this.onUpdateCallbacks.clear();
	}
}
