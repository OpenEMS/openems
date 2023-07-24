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
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.type.TypeUtils;

/**
 * A ModbusElement represents one row of a Modbus definition table.
 *
 * @param <SELF> the subclass of myself
 * @param <RAW>  the raw value type
 * @param <T>    the value type
 */
public abstract class ModbusElement<SELF extends ModbusElement<?, ?, ?>, RAW, T> {

	/** The start address of this Modbus element. */
	public final int startAddress;
	/** Number of Registers or Coils. */
	public final int length;
	/** The type of the read and write value. */
	public final OpenemsType type;

	private final Logger log = LoggerFactory.getLogger(ModbusElement.class);
	private final List<Consumer<T>> onUpdateCallbacks = new CopyOnWriteArrayList<>();
	private final List<Consumer<Optional<T>>> onSetNextWriteCallbacks = new ArrayList<>();

	/** Counts for how many cycles no valid value was read. */
	private int invalidValueCounter = 0;
	/** The next Write-Value. */
	private T nextWriteValue = null;
	/** The Task - set via {@link #setModbusTask(Task)}. */
	private Task task = null;

	protected ModbusElement(OpenemsType type, int startAddress, int length) {
		this.type = type;
		this.startAddress = startAddress;
		this.length = length;
	}

	protected abstract RAW valueToRaw(T value);

	/**
	 * Converts the RAW value from j2mod to the expected type.
	 * 
	 * @param value the raw value
	 * @return the typed/converted value
	 */
	protected abstract T rawToValue(RAW value);

	/**
	 * Gets an instance of the correct subclass of myself.
	 *
	 * @return myself
	 */
	protected abstract SELF self();

	/**
	 * Set the input/read value.
	 * 
	 * @param raw a value in raw format
	 */
	public final void setInputValue(RAW raw) {
		// Convert to type
		final T value;
		if (raw != null) {
			value = this.rawToValue(raw);
		} else {
			value = null;
		}
		// Log debug message
		if (this.isDebug) {
			this.log.info("Element [" + this + "] set value to [" + value + "].");
		}
		// Reset invalidValueCounter
		if (value != null) {
			this.invalidValueCounter = 0;
		}
		// Call Callbacks
		for (Consumer<T> callback : this.onUpdateCallbacks) {
			callback.accept(value);
		}
	}

	/**
	 * Sets a value that should be written to the Modbus device.
	 *
	 * @param value the value; possibly null
	 */
	public final void setNextWriteValue(T value) {
		// Log debug message
		if (this.isDebug()) {
			this.log.info("Element [" + this + "] set next write value to [" + value + "].");
		}
		this.nextWriteValue = value;
	}

	/**
	 * Sets a value that should be written to the Modbus device.
	 *
	 * @param value the value; possibly null
	 */
	public final void setNextWriteValueFromObject(Object value) {
		this.setNextWriteValue(TypeUtils.getAsType(this.type, value));
	}

	/**
	 * Gets the next write value.
	 *
	 * @return the next write value
	 */
	protected final T getNextWriteValue() {
		return this.nextWriteValue;
	}

	/**
	 * Resets the next write value to null.
	 */
	protected void resetNextWriteValue() {
		this.nextWriteValue = null;
	}

	/**
	 * Gets the next write value and resets it.
	 *
	 * <p>
	 * This method should be called once in every cycle on the
	 * TOPIC_CYCLE_EXECUTE_WRITE event. It makes sure, that the nextWriteValue gets
	 * initialized in every Cycle. If registers need to be written again in every
	 * cycle, next setNextWriteValue()-method needs to be called on every Cycle.
	 *
	 * @return the next write value
	 */
	public final RAW getNextWriteValueAndReset() {
		var value = this.nextWriteValue;
		if (value == null) {
			return null;
		}
		var result = this.valueToRaw(value);
		this.resetNextWriteValue();
		return result;
	}

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
	public final SELF onUpdateCallback(Consumer<T> onUpdateCallback) {
		this.onUpdateCallbacks.add(onUpdateCallback);
		return this.self();
	}

	/**
	 * Add an onSetNextWrite callback. It is called when a 'next write value' was
	 * set.
	 *
	 * @param callback the callback
	 */
	public final SELF onSetNextWrite(Consumer<Optional<T>> callback) {
		this.onSetNextWriteCallbacks.add(callback);
		return this.self();
	}

	/**
	 * Set the {@link Task}, where this Element belongs to.
	 *
	 * <p>
	 * This is called by the {@link AbstractTask} constructor.
	 *
	 * @param task the {@link Task}
	 */
	public final void setModbusTask(Task task) {
		this.task = task;
	}

	public final Task getModbusTask() {
		return this.task;
	}

	/**
	 * Invalidates the Channel in case it could not be read from the Modbus device,
	 * i.e. sets the value to 'UNDEFINED'/null. Applies the
	 * 'invalidateElementsAfterReadErrors' config setting of the bridge.
	 *
	 * @param bridge the {@link AbstractModbusBridge}
	 */
	public final void invalidate(AbstractModbusBridge bridge) {
		this.invalidValueCounter++;
		if (bridge.invalidateElementsAfterReadErrors() <= this.invalidValueCounter) {
			this.setInputValue(null);
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
	public SELF debug() {
		this.isDebug = true;
		return this.self();
	}

	protected boolean isDebug() {
		return this.isDebug;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName());
		sb.append("type=");
		sb.append(this.type.name());
		sb.append(";ref=");
		sb.append(this.startAddress);
		sb.append("/0x");
		sb.append(Integer.toHexString(this.startAddress));
		if (this.isDebug) {
			sb.append(";DEBUG");
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * This is called on deactivate of the Modbus-Bridge. It can be used to clear
	 * any references like listeners.
	 */
	public void deactivate() {
		this.onUpdateCallbacks.clear();
		this.onSetNextWriteCallbacks.clear();
	}
}
