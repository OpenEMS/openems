package io.openems.edge.bridge.can.api.task;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.can.AbstractCanBridge;
import io.openems.edge.bridge.can.LogVerbosity;
import io.openems.edge.bridge.can.api.AbstractOpenemsCanComponent;
import io.openems.edge.bridge.can.api.BridgeCan;
import io.openems.edge.bridge.can.api.data.CanFrame;
import io.openems.edge.bridge.can.api.data.CanFrameImpl;
import io.openems.edge.bridge.can.api.element.AbstractCanChannelElement;
import io.openems.edge.bridge.can.api.element.CanChannelElement;

/**
 * An 'AbstractTask' is handling exactly one {@link CanFrame} and also all
 * Channel Elements which are bind to this {@link CanFrame}.
 */
public abstract class AbstractTask implements Task {

	private static final long DEFAULT_EXECUTION_DURATION = 300; // in ms
	protected final Logger logEvt = LoggerFactory.getLogger("Prototype.Can.Event");
	protected final Logger logCan = LoggerFactory.getLogger("Prototype.Can.Cyclic");
	protected CanFrameImpl canFrame;

	private long lastExecuteDuration = DEFAULT_EXECUTION_DURATION; // initialize to some default
	private final Stopwatch stopwatch = Stopwatch.createUnstarted();
	private AbstractOpenemsCanComponent parent = null;
	private boolean hasBeenExecutedSuccessfully = false;

	protected BridgeCan bridge;

	@SafeVarargs
	public AbstractTask(BridgeCan bridge, boolean isExtendedCanFrame, int canFrameAddress, int typicalCycleTimeInMs,
			AbstractCanChannelElement<?, ?>... elements) {

		this.bridge = bridge;
		/**
		 * NOTE: -a CAN frame holds a reference to all channel elements -each channel
		 * element references a single part of data within the 8 byte of CAN data (for
		 * example a UnsignedWord at byte 2,3)
		 */
		this.canFrame = new CanFrameImpl(this, isExtendedCanFrame, canFrameAddress, typicalCycleTimeInMs, elements);
	}

	@Override
	public void setParent(AbstractOpenemsCanComponent parent) {
		this.parent = parent;
	}

	@Override
	public AbstractOpenemsCanComponent getParent() {
		return this.parent;
	}

	/**
	 * Executes the tasks - i.e. sends the query of a ReadTask or writes a
	 * WriteTask.
	 *
	 * <p>
	 * Internally the _execute()-method of the specific subclass gets called.
	 *
	 * @param bridge the Modbus-Bridge
	 * @return the number of executed Sub-Tasks
	 * @throws OpenemsException on error
	 */
	@Override
	public final synchronized int execute(AbstractCanBridge bridge) throws OpenemsException {
		this.stopwatch.reset();
		this.stopwatch.start();
		try {
			var noOfSubTasksExecuted = this._execute(bridge);

			// no exception -> mark this task as successfully executed
			this.hasBeenExecutedSuccessfully = true;
			return noOfSubTasksExecuted;

		} finally {
			this.lastExecuteDuration = this.stopwatch.elapsed(TimeUnit.MILLISECONDS);
		}
	}

	protected abstract int _execute(AbstractCanBridge bridge) throws OpenemsException;

	@Override
	public boolean hasBeenExecuted() {
		return this.hasBeenExecutedSuccessfully;
	}

	@Override
	public void deactivate() {
		for (CanChannelElement<?> element : this.canFrame.getElements()) {
			element.deactivate();
		}
	}

	/**
	 * Invalidates the elements if possible.
	 *
	 * @param bridge the CAN bridge
	 * @return true if at least one element was invalidated, false if elements
	 *         already invalidated
	 */
	protected boolean invalidateElements(AbstractCanBridge bridge) {
		var elementsInvalidated = false;
		for (CanChannelElement<?> element : this.canFrame.getElements()) {
			if (!element.isInvalid()) {
				element.invalidate(bridge);
				elementsInvalidated = true;
				this.logEvt.error("Invalidated Channels for CAN Adr: " + this.canFrame.getAddress());
			}
		}
		return elementsInvalidated;
	}

	@Override
	public long getExecuteDuration() {
		return this.lastExecuteDuration;
	}

	@Override
	public CanChannelElement<?>[] getElements() {
		return this.canFrame.getElements();
	}

	protected abstract String getDescription();

	@Override
	public Integer getCanAddress() {
		return Integer.valueOf(this.canFrame.getAddress());
	}

	public int getCanExpectedCycleTime() {
		return this.canFrame.getExpectedCycleTime();
	}

	/*
	 * Enable Debug mode for this Element. Activates verbose logging.
	 */
	private boolean isDebug = false;

	/**
	 * Activates debug mode.
	 *
	 * @return itself
	 */
	public AbstractTask debug() {
		this.isDebug = true;
		return this;
	}

	public boolean isDebug() {
		return this.isDebug;
	}

	/**
	 * Combines the global and local (via {@link #isDebug} log verbosity.
	 *
	 * @return the combined LogVerbosity
	 */
	protected LogVerbosity getLogVerbosity() {
		if (this.isDebug) {
			return LogVerbosity.READS_AND_WRITES;
		}
		return this.bridge.getLogVerbosity();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(this.getDescription());
		sb.append(" [");
		sb.append(this.canFrame.toString());
		sb.append("]");
		return sb.toString();
	}

}
