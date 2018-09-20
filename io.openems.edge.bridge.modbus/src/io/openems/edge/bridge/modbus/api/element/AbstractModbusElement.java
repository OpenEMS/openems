package io.openems.edge.bridge.modbus.api.element;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;

public abstract class AbstractModbusElement<T> implements ModbusElement<T> {

	private final Logger log = LoggerFactory.getLogger(AbstractModbusElement.class);

	private final OpenemsType type;
	private final int startAddress;
	private final boolean isIgnored;

	protected AbstractTask abstractTask = null;

	public AbstractModbusElement(OpenemsType type, int startAddress) {
		this(type, startAddress, false);
	}

	public AbstractModbusElement(OpenemsType type, int startAddress, boolean isIgnored) {
		this.type = type;
		this.startAddress = startAddress;
		this.isIgnored = isIgnored;
	}

	@Override
	public OpenemsType getType() {
		return this.type;
	}

	private final List<Consumer<T>> onUpdateCallbacks = new CopyOnWriteArrayList<>();

	/**
	 * The onUpdateCallback is called on reception of a new value.
	 * 
	 * Be aware, that this is the original, untouched value.
	 * ChannelToElementConverters are not applied here!
	 */
	public AbstractModbusElement<T> onUpdateCallback(Consumer<T> onUpdateCallback) {
		this.onUpdateCallbacks.add(onUpdateCallback);
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
	public void setModbusTask(AbstractTask abstractTask) {
		this.abstractTask = abstractTask;
	}

	public AbstractTask getModbusTask() {
		return abstractTask;
	}

	protected void setValue(T value) {
		if (this.isDebug) {
			log.info("Element [" + this + "] set value to [" + value + "].");
		}
		for (Consumer<T> callback : this.onUpdateCallbacks) {
			callback.accept(value);
		}
	}
	
	@Override
	public void invalidate() {		
		this.setValue(null); 
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

	@Override
	public String toString() {
		return this.startAddress + "/0x" + Integer.toHexString(this.startAddress);
	}
}
