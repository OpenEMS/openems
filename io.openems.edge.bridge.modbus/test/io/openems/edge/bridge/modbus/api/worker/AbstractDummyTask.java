package io.openems.edge.bridge.modbus.api.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;

import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;

public abstract class AbstractDummyTask extends AbstractTask<ModbusRequest, ModbusResponse> {

	protected final String name;

	private final Logger log = LoggerFactory.getLogger(AbstractDummyTask.class);

	protected long delay;

	private Runnable onExecuteCallback;
	private boolean isDefective = false;

	public AbstractDummyTask(String name, long delay) {
		super(name, ModbusResponse.class, 0);
		this.name = name;
		this.delay = delay;
	}

	public void setDefective(boolean isDefective, long delay) {
		this.isDefective = isDefective;
		this.delay = delay;
	}

	@Override
	protected String payloadToString(ModbusRequest request) {
		return "";
	}

	@Override
	protected String payloadToString(ModbusResponse response) {
		return "";
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
	public ExecuteState execute(AbstractModbusBridge bridge) {
		if (this.onExecuteCallback != null) {
			this.onExecuteCallback.run();
		}

		try {
			Thread.sleep(this.delay);
		} catch (InterruptedException e) {
			this.log.warn(e.getMessage());
		}

		if (this.isDefective) {
			return ExecuteState.ERROR;
		}
		return ExecuteState.OK;
	}
}