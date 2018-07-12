package io.openems.edge.common.worker;

/**
 * A helper worker for tasks that need to be executed forever without any
 * interruption or sleep.
 */
public abstract class AbstractImmediateWorker extends AbstractWorker {

	@Override
	protected void activate(String name) {
		super.activate(name);
	}

	@Override
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected final int getCycleTime() {
		return 0;
	}

	@Override
	public void triggerNextCycle() {
		super.triggerNextCycle();
	}

	@Override
	protected abstract void forever();
}
