package io.openems.edge.bridge.modbus.api.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;

public abstract class AbstractDummyTask extends AbstractTask {

	protected final String name;
	protected final long delay;

	private final Logger log = LoggerFactory.getLogger(AbstractDummyTask.class);

	private Runnable onExecuteCallback;

	public AbstractDummyTask(String name, long delay) {
		super(0);
		this.name = name;
		this.delay = delay;
	}

	/**
	 * Callback on Execute.
	 * 
	 * @param onExecuteCallback the callback {@link Runnable}
	 */
	public void onExecute(Runnable onExecuteCallback) {
		this.onExecuteCallback = onExecuteCallback;
	}

	@Override
	public int execute(AbstractModbusBridge bridge) throws OpenemsException {
		if (this.onExecuteCallback != null) {
			this.onExecuteCallback.run();
		}

		try {
			Thread.sleep(this.delay);
		} catch (InterruptedException e) {
			this.log.warn(e.getMessage());
		}
		return 1;
	}

	@Override
	protected String getActiondescription() {
		return "";
	}
}