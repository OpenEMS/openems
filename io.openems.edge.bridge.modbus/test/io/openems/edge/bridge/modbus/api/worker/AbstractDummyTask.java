package io.openems.edge.bridge.modbus.api.worker;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;

import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.task.AbstractTask;

public abstract class AbstractDummyTask extends AbstractTask<ModbusRequest, ModbusResponse> {

	protected final String name;

	private final Logger log = LoggerFactory.getLogger(AbstractDummyTask.class);

	private long delay;
	private Exception defect = null;

	protected AbstractDummyTask(String name, long delay) {
		this(name, FunctionUtils::doNothing, delay);
	}

	protected AbstractDummyTask(String name, Consumer<ExecuteState> onExecute, long delay) {
		super(name, onExecute, ModbusResponse.class, 0);
		this.name = name;
		this.delay = delay;
	}

	public void setDefective(Exception defect, long delay) {
		this.defect = defect;
		this.delay = delay;
	}

	protected long getDelay() {
		return this.delay;
	}

	@Override
	protected String payloadToString(ModbusRequest request) {
		return "";
	}

	@Override
	protected String payloadToString(ModbusResponse response) {
		return "";
	}

	@Override
	public ExecuteState execute(AbstractModbusBridge bridge) {
		try {
			Thread.sleep(this.delay);
		} catch (InterruptedException e) {
			this.log.warn(e.getMessage());
		}

		if (this.defect != null) {
			return new ExecuteState.Error(this.defect);
		}
		return ExecuteState.OK;
	}
}