package io.openems.edge.common.worker;

/**
 * A helper worker for tasks that always waits for a call to 'triggerNextRun()'
 * method before it executes 'forever()' again.
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
	public void triggerNextCycle() {
		super.triggerNextCycle();
	}

	@Override
	protected abstract void forever();
}
