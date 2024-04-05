package io.openems.common.worker;

/**
 * Defines a generic Worker Thread.
 *
 * <p>
 * The business logic of the Worker is inside the {@link #forever()} method. It
 * is executed always called immediately without any delay.
 */
public abstract class AbstractImmediateWorker extends AbstractWorker {

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
		return AbstractWorker.DO_NOT_WAIT;
	}

	@Override
	protected abstract void forever() throws Throwable;
}
