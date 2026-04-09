package io.openems.edge.evse.chargepoint.mennekes.common;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;

public class TestUtils {

	/**
	 * Creates a DummyModbusBridge with Registers.
	 * 
	 * @return the {@link DummyModbusBridge}
	 */
	public static DummyModbusBridge testModbus() {
		return new DummyModbusBridge("modbus0")//
				.withRegisters(104, 6) //
				.withRegisters(111, 1, 0) //
				.withRegisters(122, 3) //
				.withRegisters(131, new int[] { //
						6, // 131
						0, 0, 0, 0, 0, 0, // 132–137 (6 values)
						0, 0, 0, 0, 0, // 138–142 (5 values)
						0, 0, 0, 0, 0, 0, // 143–148 (6 values)
						0, 0, 0, 0, // 149–152 (4 values)
						1, 5, 22 })
				.withRegisters(200, 0) //
				.withRegisters(206, new int[] { //
						0, 1380, // 206–207 (L1 power)
						0, 1380, // 208–209
						0, 1380, // 210–211
						0, 6, // 212–213
						0, 6, // 214–215
						0, 6, // 216–217
						0, 0, 0, 0, // 218–221
						0, 230, // 222–223
						0, 230, // 224–225
						0, 230 // 226–227
				})//
				.withRegisters(712, 6, 0, 0, 16) //
				.withRegisters(1000, 16); //
	}

}
