package io.openems.edge.bridge.modbus.api.task.hooks.mocks;

import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.hooks.TaskHook;
import org.junit.jupiter.api.Assertions;

public class DummyTaskHook extends TaskHook {
	private HookState state = HookState.NEW;

	private void expectHookState(HookState state) {
		Assertions.assertEquals(state, this.state);
	}

	public HookState getState() {
		return this.state;
	}

	protected void setState(HookState state) {
		this.state = state;
	}

	@Override
	public void preExecute(AbstractModbusBridge bridge, Task task) {
		this.expectHookState(HookState.NEW);
		this.setState(HookState.PRE_EXECUTE);
	}

	@Override
	public void execute(AbstractModbusBridge bridge, Task task, Task.ExecuteState state) {
		this.expectHookState(HookState.PRE_EXECUTE);
		this.setState(HookState.EXECUTE);
	}

	@Override
	public void postExecute(AbstractModbusBridge bridge, Task task) {
		this.expectHookState(HookState.EXECUTE);
		this.setState(HookState.POST_EXECUTE);
	}

	public enum HookState {
		NEW, PRE_EXECUTE, EXECUTE, POST_EXECUTE
	}
}
