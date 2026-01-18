package io.openems.edge.edge2edge.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.common.modbusslave.ModbusRecordFloat32;
import io.openems.edge.common.modbusslave.ModbusRecordFloat64;
import io.openems.edge.common.modbusslave.ModbusRecordString16;
import io.openems.edge.common.modbusslave.ModbusRecordUint16;
import io.openems.edge.common.modbusslave.ModbusRecordUint32;
import io.openems.edge.common.modbusslave.ModbusRecordUint64;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.edge2edge.ess.Edge2EdgeEssImpl;

public class AbstractEdge2EdgeTest {

	@Test
	public void testIsHashEqual() {
		assertTrue(Edge2EdgeEssImpl.isHashEqual(0x6201, "OpenEMS"));
		assertTrue(Edge2EdgeEssImpl.isHashEqual(0xb3dc, "OpenemsComponent"));
		assertFalse(Edge2EdgeEssImpl.isHashEqual(null, "_sum"));
		assertFalse(Edge2EdgeEssImpl.isHashEqual(0x6201, "foobar"));
	}

	@Test
	public void testGetConverterForType() {
		{
			final var converter = AbstractEdge2Edge.getConverterForType(ModbusType.FLOAT32);
			assertNull(converter.elementToChannel(null));
			assertNull(converter.elementToChannel(Float.NaN));
			assertNull(converter.elementToChannel(ModbusRecordFloat32.UNDEFINED_VALUE));
			assertEquals(123.456F, (float) converter.elementToChannel(123.456), 0.001);
		}
		{
			final var converter = AbstractEdge2Edge.getConverterForType(ModbusType.FLOAT64);
			assertNull(converter.elementToChannel(null));
			assertNull(converter.elementToChannel(Double.NaN));
			assertNull(converter.elementToChannel(ModbusRecordFloat64.UNDEFINED_VALUE));
			assertEquals(123.456, (double) converter.elementToChannel(123.456), 0.001);
		}
		{
			final var converter = AbstractEdge2Edge.getConverterForType(ModbusType.STRING16);
			assertNull(converter.elementToChannel(null));
			assertEquals(16, ((String) converter.elementToChannel(ModbusRecordString16.UNDEFINED_VALUE)).length());
			assertEquals("OpenEMS", converter.elementToChannel("OpenEMS"));
		}
		{
			final var converter = AbstractEdge2Edge.getConverterForType(ModbusType.UINT16);
			assertNull(converter.elementToChannel(null));
			assertNull(converter.elementToChannel(0xFFFF));
			assertNull(converter.elementToChannel(ModbusRecordUint16.UNDEFINED_VALUE));
			assertEquals(0, converter.elementToChannel(0x0000));
		}
		{
			final var converter = AbstractEdge2Edge.getConverterForType(ModbusType.UINT32);
			assertNull(converter.elementToChannel(null));
			assertNull(converter.elementToChannel(0xFFFFFFFFL));
			assertNull(converter.elementToChannel(ModbusRecordUint32.UNDEFINED_VALUE));
			assertEquals(0L, converter.elementToChannel(0x00000000));
		}
		{
			final var converter = AbstractEdge2Edge.getConverterForType(ModbusType.UINT64);
			assertNull(converter.elementToChannel(null));
			assertNull(converter.elementToChannel(0xFFFFFFFFFFFFFFFFL));
			assertNull(converter.elementToChannel(ModbusRecordUint64.UNDEFINED_VALUE));
			assertEquals(0L, converter.elementToChannel(0x0000000000000000));
		}
	}
}
