package io.openems.edge.goodwe.common;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;

public final class GridModeTestHelper {

	private GridModeTestHelper() {
	}

	public static void setGridModeClock(AbstractGoodWe goodWe, Clock clock) {
		setGridModeReader(goodWe, clock, goodWe::readC50MeterCommunicateStatus);
	}

	public static void setGridModeReader(AbstractGoodWe goodWe, Clock clock,
			Supplier<CompletableFuture<Integer>> readMeterCommunicateStatus) {
		ReflectionUtils.setAttributeViaReflection(goodWe, "gridModeStabilityFilter",
				new GridModeStabilityFilter(clock, readMeterCommunicateStatus));
	}

	/**
	 * Delivers a register-35136 response to the component's Modbus element.
	 *
	 * @param goodWe the GoodWe component
	 * @param value  the register value, or null for an invalid read
	 */
	public static void receiveGridMode(AbstractGoodWe goodWe, Integer value) {
		ModbusProtocol protocol = ReflectionUtils.getValueViaReflection(goodWe, "protocol");
		var element = (UnsignedWordElement) protocol.getTaskManager().getTasks().stream() //
				.filter(task -> task.getStartAddress() == 35136) //
				.findFirst().orElseThrow().getElements()[0];
		element.setInputValue(value == null ? null : new Register[] { new SimpleRegister(value) });
	}
}
