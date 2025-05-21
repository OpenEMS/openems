package io.openems.edge.edge2edge.common;

import static io.openems.edge.edge2edge.common.AbstractEdge2Edge.generateModbusElement;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.modbusslave.ModbusType;

public class AbstractEdge2EdgeTest2 {

	@Test
	public void testGenerateModbusElement() {
		test(ModbusType.ENUM16, OpenemsType.INTEGER);
		test(ModbusType.UINT16, OpenemsType.INTEGER);
		test(ModbusType.UINT32, OpenemsType.LONG);
		test(ModbusType.UINT64, OpenemsType.LONG);
		test(ModbusType.FLOAT32, OpenemsType.FLOAT);
		test(ModbusType.FLOAT64, OpenemsType.DOUBLE);
		test(ModbusType.STRING16, OpenemsType.STRING);
	}

	private static void test(ModbusType modbusType, OpenemsType openemsType) {
		var element = generateModbusElement(modbusType, 0);
		assertEquals(modbusType.getWords(), element.length);
		assertEquals(openemsType, ((AbstractModbusElement<?, ?, ?>) element).type);
	}
}
