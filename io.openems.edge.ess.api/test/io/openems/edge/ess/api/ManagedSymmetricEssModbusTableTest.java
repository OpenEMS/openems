package io.openems.edge.ess.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusRecordChannel;
import io.openems.edge.common.modbusslave.ModbusType;

public class ManagedSymmetricEssModbusTableTest {

	@Test
	public void testReadWriteTableContainsExpectedRecords() {
		var table = ManagedSymmetricEss.getModbusSlaveNatureTable(AccessMode.READ_WRITE);

		assertNotNull(table);
		assertEquals(ManagedSymmetricEss.class, table.getNatureClass());
		assertEquals(100, table.getLength());

		var minimumPowerSetPoint = getRecordByOffset(table, 0);
		assertEquals("Minimum Power Set-Point", minimumPowerSetPoint.getName());
		assertEquals(ModbusType.FLOAT32, minimumPowerSetPoint.getType());

		var maximumPowerSetPoint = getRecordByOffset(table, 2);
		assertEquals("Maximum Power Set-Point", maximumPowerSetPoint.getName());
		assertEquals(ModbusType.FLOAT32, maximumPowerSetPoint.getType());

		var setActivePowerEquals = getRecordByOffset(table, 4);
		assertTrue(setActivePowerEquals instanceof ModbusRecordChannel);
		assertEquals(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS,
				((ModbusRecordChannel) setActivePowerEquals).getChannelId());

		var setReactivePowerEquals = getRecordByOffset(table, 6);
		assertTrue(setReactivePowerEquals instanceof ModbusRecordChannel);
		assertEquals(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS,
				((ModbusRecordChannel) setReactivePowerEquals).getChannelId());

		var setActivePowerLessOrEquals = getRecordByOffset(table, 8);
		assertTrue(setActivePowerLessOrEquals instanceof ModbusRecordChannel);
		assertEquals(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS,
				((ModbusRecordChannel) setActivePowerLessOrEquals).getChannelId());

		var setReactivePowerLessOrEquals = getRecordByOffset(table, 10);
		assertTrue(setReactivePowerLessOrEquals instanceof ModbusRecordChannel);
		assertEquals(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_LESS_OR_EQUALS,
				((ModbusRecordChannel) setReactivePowerLessOrEquals).getChannelId());

		var setActivePowerGreaterOrEquals = getRecordByOffset(table, 12);
		assertTrue(setActivePowerGreaterOrEquals instanceof ModbusRecordChannel);
		assertEquals(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS,
				((ModbusRecordChannel) setActivePowerGreaterOrEquals).getChannelId());

		var setReactivePowerGreaterOrEquals = getRecordByOffset(table, 14);
		assertTrue(setReactivePowerGreaterOrEquals instanceof ModbusRecordChannel);
		assertEquals(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_GREATER_OR_EQUALS,
				((ModbusRecordChannel) setReactivePowerGreaterOrEquals).getChannelId());
	}

	private static ModbusRecord getRecordByOffset(io.openems.edge.common.modbusslave.ModbusSlaveNatureTable table,
			int offset) {
		return Arrays.stream(table.getModbusRecords()) //
				.filter(record -> record.getOffset() == offset) //
				.findFirst() //
				.orElseThrow(() -> new AssertionError("No Modbus record found at offset " + offset));
	}
}
