package io.openems.edge.io.weidmueller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class IoWeidmuellerUr20ImplTest {

	@Test
	public void test() throws Exception {
		var sut = new IoWeidmuellerUr20Impl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //

						// Module discovery: 2 modules (register 0x27FE = 2)
						.withRegister(0x27FE, 2) //

						// M0: UR20_16DI_P (module ID 0x00049FC2 = registers 4, 0x9FC2)
						.withRegisters(0x2A00, 4, 0x9FC2) //

						// M1: UR20_8DO_P (module ID 0x01022FC8 = registers 0x0102, 0x2FC8)
						.withRegisters(0x2A02, 0x0102, 0x2FC8) //

						// M0 setting input state: 0xAAAA = 1010_1010_1010_1010b
						.withRegister(0x8000, 0xAAAA) //

						// M1 output feedback at 0x9020 (= 0x9000 + 1*32): 0xA5 = 1010_0101b
						.withRegister(0x9020, 0xA5)) //

				.activate(MyConfig.create() //
						.setId("io0") //
						.setModbusId("modbus0") //
						.build()) //
				// Cycle 1: reads module count from 0x27FE
				.next(new TestCase()) //
				// Cycle 2: reads module list
				.next(new TestCase()) //
				// Cycle 3: reads channel data from 0x8000 (M0) and 0x9020 (M1)
				.next(new TestCase()); //

		// 16 channels from M0 (UR20_16DI_P) + 8 channels from M1 (UR20_8DO_P) = 24
		assertNotNull(sut.digitalInputChannels());
		assertEquals(24, sut.digitalInputChannels().length);

		// 8 output channels from M1 (UR20_8DO_P)
		assertNotNull(sut.digitalOutputChannels());
		assertEquals(8, sut.digitalOutputChannels().length);

		// M0: verify channel values from 0xAAAA (1010_1010_1010_1010b)
		assertEquals(false, sut.digitalInputChannels()[0].value().get());
		assertEquals(true, sut.digitalInputChannels()[1].value().get());
		assertEquals(false, sut.digitalInputChannels()[2].value().get());
		assertEquals(true, sut.digitalInputChannels()[3].value().get());

		// M1: verify output feedback values from 0xA5 (1010_0101b)
		assertEquals(true, sut.digitalInputChannels()[16].value().get());
		assertEquals(false, sut.digitalInputChannels()[17].value().get());
		assertEquals(true, sut.digitalInputChannels()[18].value().get());
		assertEquals(false, sut.digitalInputChannels()[19].value().get());
	}

	@Test
	public void testDuplicateModuleType() throws Exception {
		var sut = new IoWeidmuellerUr20Impl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //

						// Module discovery: 2 modules of the same type
						.withRegister(0x27FE, 2) //

						// M0: UR20_4DI_P (module ID 0x00091F84)
						.withRegisters(0x2A00, 0x0009, 0x1F84) //

						// M1: UR20_4DI_P duplicate
						.withRegisters(0x2A02, 0x0009, 0x1F84) //

						// M0 input state at 0x8000: 0b1010 = bits 1,3 true
						.withRegister(0x8000, 0b1010) //

						// M1 input state at 0x8020 (= 0x8000 + 1*32): 0b0101 = bits 0,2 true
						.withRegister(0x8020, 0b0101)) //

				.activate(MyConfig.create() //
						.setId("io0") //
						.setModbusId("modbus0") //
						.build()) //
				// Cycle 1: reads module count from 0x27FE
				.next(new TestCase()) //
				// Cycle 2: reads module list from 0x2A00-0x2A03
				.next(new TestCase()) //
				// Cycle 3: reads channel data from 0x8000 (M0) and 0x8020 (M1)
				.next(new TestCase()); //

		// 4 channels from M0 + 4 channels from M1 = 8 (old TreeMap code would give 4)
		assertNotNull(sut.digitalInputChannels());
		assertEquals(8, sut.digitalInputChannels().length);

		// M0 input values from 0b1010: bit0=0, bit1=1, bit2=0, bit3=1
		assertEquals(false, sut.digitalInputChannels()[0].value().get());
		assertEquals(true, sut.digitalInputChannels()[1].value().get());
		assertEquals(false, sut.digitalInputChannels()[2].value().get());
		assertEquals(true, sut.digitalInputChannels()[3].value().get());

		// M1 input values from 0b0101: bit0=1, bit1=0, bit2=1, bit3=0
		assertEquals(true, sut.digitalInputChannels()[4].value().get());
		assertEquals(false, sut.digitalInputChannels()[5].value().get());
		assertEquals(true, sut.digitalInputChannels()[6].value().get());
		assertEquals(false, sut.digitalInputChannels()[7].value().get());
	}

	@Test
	public void testDuplicateModuleTypeWithDifferentModule() throws Exception {
		var sut = new IoWeidmuellerUr20Impl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //

						// Module discovery: 3 modules
						.withRegister(0x27FE, 3) //

						// M0: UR20_4DI_P (module ID 0x00091F84)
						.withRegisters(0x2A00, 0x0009, 0x1F84) //

						// M1: UR20_4DI_P (same type repeated)
						.withRegisters(0x2A02, 0x0009, 0x1F84) //

						// M2: UR20_8DO_P (module ID 0x01022FC8)
						.withRegisters(0x2A04, 0x0102, 0x2FC8) //

						// M0 input state at 0x8000: 0b1010
						.withRegister(0x8000, 0b1010) //

						// M1 input state at 0x8020 (= 0x8000 + 1*32): 0b0101
						.withRegister(0x8020, 0b0101) //

						// M2 output feedback at 0x9040 (= 0x9000 + 2*32): 0b11001010
						.withRegister(0x9040, 0b11001010)) //

				.activate(MyConfig.create() //
						.setId("io0") //
						.setModbusId("modbus0") //
						.build()) //
				// Cycle 1: reads module count from 0x27FE
				.next(new TestCase()) //
				// Cycle 2: reads module list from 0x2A00-0x2A05
				.next(new TestCase()) //
				// Cycle 3: reads channel data from 0x8000 (M0), 0x8020 (M1), 0x9040 (M2)
				.next(new TestCase()); //

		// 4 (M0) + 4 (M1) + 8 (M2) = 16 input channels
		assertNotNull(sut.digitalInputChannels());
		assertEquals(16, sut.digitalInputChannels().length);

		// 8 output channels from M2
		assertNotNull(sut.digitalOutputChannels());
		assertEquals(8, sut.digitalOutputChannels().length);

		// M0 input values from 0b1010: bit0=0, bit1=1, bit2=0, bit3=1
		assertEquals(false, sut.digitalInputChannels()[0].value().get());
		assertEquals(true, sut.digitalInputChannels()[1].value().get());
		assertEquals(false, sut.digitalInputChannels()[2].value().get());
		assertEquals(true, sut.digitalInputChannels()[3].value().get());

		// M1 input values from 0b0101: bit0=1, bit1=0, bit2=1, bit3=0
		assertEquals(true, sut.digitalInputChannels()[4].value().get());
		assertEquals(false, sut.digitalInputChannels()[5].value().get());
		assertEquals(true, sut.digitalInputChannels()[6].value().get());
		assertEquals(false, sut.digitalInputChannels()[7].value().get());

		// M2 output feedback from 0b11001010: bit0=0, bit1=1, bit2=0, bit3=1, bit4=0,
		// bit5=0, bit6=1, bit7=1
		assertEquals(false, sut.digitalInputChannels()[8].value().get());
		assertEquals(true, sut.digitalInputChannels()[9].value().get());
		assertEquals(false, sut.digitalInputChannels()[10].value().get());
		assertEquals(true, sut.digitalInputChannels()[11].value().get());
		assertEquals(false, sut.digitalInputChannels()[12].value().get());
		assertEquals(false, sut.digitalInputChannels()[13].value().get());
		assertEquals(true, sut.digitalInputChannels()[14].value().get());
		assertEquals(true, sut.digitalInputChannels()[15].value().get());
	}
}
