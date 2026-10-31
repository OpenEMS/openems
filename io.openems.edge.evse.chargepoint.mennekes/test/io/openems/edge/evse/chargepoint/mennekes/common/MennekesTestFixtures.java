package io.openems.edge.evse.chargepoint.mennekes.common;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;

public class MennekesTestFixtures {

	private static final int[] REG_131_TO_153 = { //
			6, // 131
			0, 0, 0, 0, 0, 0, // 132-137
			0, 0, 0, 16717, 16706, // 138-142
			17220, 12594, 13108, 17734, 18248, 13622, // 143-148
			14136, 18762, 19276, 0, // 149-152
			1, 5, 22 // 153-155
	};

	private static final int[] REG_206_TO_227 = { //
			0, 1380, // 206-207 (L1 power)
			0, 1380, // 208-209
			0, 1380, // 210-211
			0, 6000, // 212-213
			0, 6000, // 214-215
			0, 6000, // 216-217
			0, 0, 0, 0, // 218-221
			0, 230, // 222-223
			0, 230, // 224-225
			0, 230 // 226-227
	};

	private static final int[] REG_206_TO_227_PROFESSIONAL_LEGACY = { //
			0, 1380, // 206-207 (L1 power = total power on legacy Professional)
			0xFFFF, 0xFFFF, // 208-209
			0xFFFF, 0xFFFF, // 210-211
			0, 6000, // 212-213
			0, 6000, // 214-215
			0, 6000, // 216-217
			0, 0, 0, 0, // 218-221
			0, 230, // 222-223
			0, 230, // 224-225
			0, 230 // 226-227
	};

	private static final int[] REG_2012_ONWARDS = { //
			4140, 11040, // 2012-2013
			0, 0, 0, 0, 0, 0, // 2014-2019
			2, 30, 1, 2 // 2020 onwards
	};

	private MennekesTestFixtures() {
		// Utility class
	}

	/**
	 * Creates a Mennekes-specific DummyModbusBridge fixture.
	 *
	 * @return the {@link DummyModbusBridge}
	 */
	public static DummyModbusBridge createMennekesModbusBridge() {
		return createMennekesModbusBridge(true);
	}

	private static DummyModbusBridge createMennekesModbusBridge(boolean includeTwoThousandRegisters) {
		var bridge = new DummyModbusBridge("modbus0") //
				.withRegisters(104, 3) //
				.withRegisters(111, 1, 0) //
				.withRegisters(122, 3) //
				.withRegisters(131, REG_131_TO_153) //
				.withRegisters(200, 0, 0) //
				.withRegisters(206, REG_206_TO_227) //
				.withRegisters(712, 6, 0, 0, 16) //
				.withRegisters(1000, 16);
		if (includeTwoThousandRegisters) {
			bridge.withRegisters(2012, REG_2012_ONWARDS);
		}
		return bridge;
	}

	/**
	 * Creates a Mennekes-specific DummyModbusBridge fixture for Professional
	 * devices without 2000er register blocks.
	 *
	 * @return the {@link DummyModbusBridge}
	 */
	public static DummyModbusBridge createMennekesProfessionalModbusBridge() {
		return createMennekesModbusBridge(false);
	}

	/**
	 * Creates a Mennekes-specific DummyModbusBridge fixture for legacy Professional
	 * devices where total power is exposed on the L1 register and L2/L3 return
	 * 0xFFFFFFFF.
	 *
	 * @return the {@link DummyModbusBridge}
	 */
	public static DummyModbusBridge createMennekesProfessionalLegacyModbusBridge() {
		return createMennekesProfessionalModbusBridge() //
				.withRegisters(200, 0, 42) //
				.withRegisters(206, REG_206_TO_227_PROFESSIONAL_LEGACY);
	}

}