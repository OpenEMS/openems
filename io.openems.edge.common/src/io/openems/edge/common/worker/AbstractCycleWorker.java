package io.openems.edge.common.worker;

/**
 * A helper worker for tasks that need to be executed synchronized with the
 * Cycle. Be sure to call 'triggerNextCycle()' once per Cycle (using an OSGi
 * EventHandler).
 */
public abstract class AbstractCycleWorker extends AbstractWorker {

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
		return -1;
	}

	@Override
	public void triggerNextCycle() {
		super.triggerNextCycle();
	}

	@Override
	protected abstract void forever();
}
