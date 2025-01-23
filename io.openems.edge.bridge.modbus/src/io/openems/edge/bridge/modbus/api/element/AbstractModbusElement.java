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
import io.openems.edge.bridge.modbus.api.task.AbstractReadTask;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.common.type.TypeUtils;

/**
 * A ModbusElement represents one row of a Modbus definition table.
 *
 * @param <SELF> the subclass of myself
 * @param <RAW>  the raw value type
 * @param <T>    the value type
 */
public abstract non-sealed class AbstractModbusElement<SELF extends AbstractModbusElement<?, ?, ?>, RAW, T>
		extends ModbusElement {

	/** The type of the read and write value. */
	public final OpenemsType type;

	private final Logger log = LoggerFactory.getLogger(AbstractModbusElement.class);
	private final List<Consumer<T>> onUpdateCallbacks = new CopyOnWriteArrayList<>();
	private final List<Consumer<Optional<T>>> onSetNextWriteCallbacks = new ArrayList<>();

	/** The next Write-Value. */
	protected T nextWriteValue = null;

	/** Counts for how many cycles no valid value was read. */
	private int invalidValueCounter = 0;

	public static enum FillElementsPriority {
		DEFAULT, HIGH
	}

	/** Priority handling in {@link AbstractReadTask#fillElements()}. */
	private FillElementsPriority fillElementsPriority = FillElementsPriority.DEFAULT;

	protected AbstractModbusElement(OpenemsType type, int startAddress, int length) {
		super(startAddress, length);
		this.type = type;
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
	 * Sets the {@link FillElementsPriority}.
	 * 
	 * <p>
	 * By default ({@link FillElementsPriority#DEFAULT} all Element-to-Channel
	 * mappings are handled in array order of the {@link ReadTask}. Elements marked
	 * {@link FillElementsPriority#HIGH} are handled first.
	 * 
	 * <p>
	 * This feature is useful for SunSpec devices, where the dynamic ScaleFactor for
	 * a Element is only at the end of the SunSpec block. Without HIGH priority, the
	 * ScaleFactor would always get applied too late.
	 * 
	 * @param fillElementsPriority the {@link FillElementsPriority}
	 * @return myself
	 */
	public SELF fillElementsPriority(FillElementsPriority fillElementsPriority) {
		this.fillElementsPriority = fillElementsPriority;
		return this.self();
	}

	/**
	 * FillElementsPriority. Used internally.
	 * 
	 * @return the {@link FillElementsPriority}
	 */
	@Deprecated
	public FillElementsPriority _getFillElementsPriority() {
		return this.fillElementsPriority;
	}

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
		this.nextWriteValue = null;
		this.onNextWriteValueReset();
		return result;
	}

	protected void onNextWriteValueReset() {
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
	 * @return myself
	 */
	public final SELF onSetNextWrite(Consumer<Optional<T>> callback) {
		this.onSetNextWriteCallbacks.add(callback);
		return this.self();
	}

	@Override
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

	@Override
	public void deactivate() {
		this.onUpdateCallbacks.clear();
		this.onSetNextWriteCallbacks.clear();
	}
}
