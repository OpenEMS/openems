package io.openems.edge.bridge.modbus.api.element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.modbus.api.task.Task;

public abstract class AbstractModbusElement<T> implements ModbusElement, ModbusRegisterElement {

	private final Logger log = LoggerFactory.getLogger(AbstractModbusElement.class);

	private final int startAddress;
	private final boolean isIgnored;

	protected Task task = null;

	/*
	 * The onUpdateCallback is called on reception of a new value
	 */
	private OnUpdate<T> onUpdateCallback;

	public AbstractModbusElement(int startAddress) {
		this(startAddress, false);
	}

	public AbstractModbusElement(int startAddress, boolean isIgnored) {
		this.startAddress = startAddress;
		this.onUpdateCallback = null;
		this.isIgnored = isIgnored;
	}

	public AbstractModbusElement<T> onUpdateCallback(OnUpdate<T> onUpdateCallback) {
		this.onUpdateCallback = onUpdateCallback;
		return this;
	}

	@Override
	public int getStartAddress() {
		return startAddress;
	}

	@Override
	public boolean isIgnored() {
		return isIgnored;
	}

	@Override
	public void setModbusTask(Task task) {
		this.task = task;
	}

	public Task getModbusTask() {
		return task;
	}

	protected void setValue(T value) {
		if (this.isDebug) {
			log.info("Element at [" + this.startAddress + "/0x" + Integer.toHexString(this.startAddress)
					+ "] set value to [" + value + "].");
		}
		if (this.onUpdateCallback != null) {
			this.onUpdateCallback.call(value);
		}
	}

	/**
	 * BuilderPattern. The received value is adjusted to the power of the
	 * scaleFactor (y = x * 10^scaleFactor).
	 * 
	 * Example: if the Register is in unit [0.1 V] use scaleFactor '2' to make the
	 * unit [1 mV]
	 */
	private int scaleFactor = 0;

	public AbstractModbusElement<T> scaleFactor(int scaleFactor) {
		this.scaleFactor = scaleFactor;
		return this;
	}

	public int getScaleFactor() {
		return scaleFactor;
	}

	/*
	 * Enable Debug mode for this Element. Activates verbose logging.
	 */
	private boolean isDebug = false;

	public AbstractModbusElement<T> debug() {
		this.isDebug = true;
		return this;
	}

	protected boolean isDebug() {
		return isDebug;
	}

	/*
	 * Handle high priority elements. Those are queried in every cycle.
	 */
	private Priority priority = Priority.LOW;

	public AbstractModbusElement<T> priority(Priority priority) {
		this.priority = priority;
		return this;
	}

	@Override
	public Priority getPriority() {
		return priority;
	}

	// protected void setValue(T value) {
	// if (channel == null) {
	// return;
	// } else if (channel instanceof ModbusReadChannel) {
	// ((ModbusReadChannel<T>) channel).updateValue(value);
	// } else if (channel instanceof ModbusWriteChannel) {
	// ((ModbusWriteChannel<T>) channel).updateValue(value);
	// } else {
	// log.error("Unable to set value [" + value + "]. Channel [" +
	// channel.address()
	// + "] is no ModbusChannel or WritableModbusChannel.");
	// new Throwable().printStackTrace();
	// }
	// }
	//
	// @Override
	// public String toString() {
	// return "ModbusElement: Implementation[" + this.getClass().getSimpleName() +
	// "], ModbusAddress[" + address + "]"
	// + (channel != null ? ", ChannelAddress[" + channel.address() + "]" : "");
	// }
}
