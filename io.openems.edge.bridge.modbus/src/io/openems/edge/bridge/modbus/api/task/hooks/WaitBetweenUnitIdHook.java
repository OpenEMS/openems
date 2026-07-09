package io.openems.edge.bridge.modbus.api.task.hooks;

import java.time.Duration;

import io.openems.common.logger.ContextLogger;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.ModbusTransferInfo;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.WaitTask;

/**
 * Some devices need a short settling time when switching Modbus Unit IDs.
 *
 * <p>
 * If a request to one Unit ID is immediately followed by a request to a
 * different Unit ID on the same bus, some devices may not respond. The exact
 * reason is unclear (e.g. internal buffering or timing constraints).
 *
 * <p>
 * This hook enforces a small delay between requests with different Unit IDs to
 * improve communication reliability.
 */
public class WaitBetweenUnitIdHook extends TaskHook {
	private final Duration duration;

	public WaitBetweenUnitIdHook(Duration duration) {
		this.duration = duration;
	}

	@Override
	public void preExecute(AbstractModbusBridge bridge, Task task) {
		var lastTransferInfo = bridge.getLastTransferInfo();
		if (lastTransferInfo != null && lastTransferInfo.unitId() != task.getUnitId()) {
			this.ensureDelayBetweenTransfers(lastTransferInfo, task, bridge);
		}
	}

	private void ensureDelayBetweenTransfers(ModbusTransferInfo lastTransferInfo, Task task,
			AbstractModbusBridge bridge) {
		var currentTime = bridge.getClock().instant();
		var delaySinceLastTransfer = Duration.between(lastTransferInfo.time(), currentTime);
		var remainingDelay = this.duration.minus(delaySinceLastTransfer);

		if (remainingDelay.isPositive()) {
			if (bridge.getLogVerbosity() == LogVerbosity.READS_AND_WRITES_DURATION_TRACE_EVENTS) {
				new ContextLogger(this.getClass(), bridge.id()).info(
						"Wait {}ms before sending request to unit {}, last request sent to unit {}",
						remainingDelay.toMillis(), task.getUnitId(), lastTransferInfo.unitId());
			}
			var delayTask = new WaitTask.Delay(remainingDelay.toMillis(), FunctionUtils::doNothing);
			delayTask.execute(bridge);
		}
	}
}
