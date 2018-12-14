package io.openems.edge.common.worker;

/**
 * A helper worker for tasks that need to be executed forever without any
 * interruption or sleep.
 */
public abstract class AbstractWaitForTriggerWorker extends AbstractWorker {

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
	public void triggerNextCycle() {
		super.triggerNextCycle();
	}

	@Override
	protected abstract void forever();
}
