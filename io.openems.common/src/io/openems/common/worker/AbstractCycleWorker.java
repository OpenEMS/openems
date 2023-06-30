package io.openems.common.worker;

/**
 * Defines a generic Worker Thread.
 *
 * <p>
 * The business logic of the Worker is inside the {@link #forever()} method. It
 * is executed after triggered the {@link #triggerNextRun()} method.
 *
 * <p>
 * This implementation is helpful to execute logic synchronized with the OpenEMS
 * Edge Cycle. Be sure to call 'triggerNextCycle()' once per Cycle (using an
 * OSGi EventHandler).
 */
public abstract class AbstractCycleWorker extends AbstractWorker {

	@Override
	public void activate(String name) {
		super.activate(name);
	}

	@Override
	public void deactivate() {
		super.deactivate();
	}

	@Override
	protected final int getCycleTime() {
		return AbstractWorker.ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN;
	}

	@Override
	protected abstract void forever() throws Throwable;
}
