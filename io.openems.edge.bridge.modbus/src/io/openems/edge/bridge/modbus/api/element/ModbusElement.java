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
 * @param <SELF>   the subclass of myself
 * @param <BINARY> the binary type
 * @param <T>      the OpenEMS type
 */
public abstract class ModbusElement<SELF extends ModbusElement<?, ?, ?>, BINARY, T> {

	/** The start address of this Modbus element. */
	public final int startAddress;
	/** Number of Registers or Coils. */
	public final int length;

	// TODO private
	protected final List<Consumer<Optional<T>>> onSetNextWriteCallbacks = new ArrayList<>();

	private final Logger log = LoggerFactory.getLogger(ModbusElement.class);
	private final OpenemsType type;
	private final List<Consumer<T>> onUpdateCallbacks = new CopyOnWriteArrayList<>();

	/** Counts for how many cycles no valid value was read. */
	private int invalidValueCounter = 0;

	/** The next Write-Value. */
	private BINARY writeValue = null;

	protected Task task = null;

	protected ModbusElement(OpenemsType type, int startAddress, int length) {
		this.type = type;
		this.startAddress = startAddress;
		this.length = length;
	}

	/**
	 * Gets an instance of the correct subclass of myself.
	 *
	 * @return myself
	 */
	protected abstract SELF self();

	// TODO protected
	public final void setInputValue(BINARY binary) {
		var value = this.binaryToValue(binary);
		
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

	/**
	 * Sets a value that should be written to the Modbus device.
	 *
	 * @param value the value; possibly null
	 */
	// TODO protected
	// * @throws OpenemsException on error
	// * @throws IllegalArgumentException on error
	public final void setNextWriteValue(T value) {
		var binary = this.valueToBinary(value);
		this.writeValue = binary;
	}

	protected abstract BINARY valueToBinary(T value);

	protected abstract T binaryToValue(BINARY value);

	public final void setNextWriteValueFromObject(Object value) {
		this.setNextWriteValue(TypeUtils.getAsType(this.type, value));
	}

	/**
	 * Gets the type of this Register, e.g. INTEGER, BOOLEAN,..
	 *
	 * @return the OpenemsType
	 */
	public final OpenemsType getType() {
		return this.type;
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
	@Deprecated
	public final void invalidate(AbstractModbusBridge bridge) {
//		this.invalidValueCounter++;
//		if (bridge.invalidateElementsAfterReadErrors() <= this.invalidValueCounter) {
//			this.setValue(null);
//		}
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
