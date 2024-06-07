package io.openems.edge.bridge.modbus.sunspec;

import static io.openems.edge.bridge.modbus.sunspec.Utils.toLabel;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testToLabel() {
		assertEquals("Not Configured", toLabel("NOT_CONFIGURED"));
		assertEquals("Reserved 8", toLabel("RESERVED_8"));
		assertEquals("FixedPF", toLabel("FixedPF"));
		assertEquals("Volt-VAr", toLabel("Volt-VAr"));
		assertEquals("Freq-Watt-Param", toLabel("Freq-Watt-Param"));
		assertEquals("LVRT", toLabel("LVRT"));
		assertEquals("DC Over Volt", toLabel("DC_OVER_VOLT"));
	}

}
