package io.openems.edge.evse.chargepoint.keba.modbus;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.chargepoint.PhaseRotation.L2_L3_L1;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testElectricityMeterChannels;
import static io.openems.edge.evse.chargepoint.keba.common.EvseKebaTest.testEvseKebaChannels;
import static io.openems.edge.evse.chargepoint.keba.common.KebaModbusTest.prepareKebaModbus;
import static io.openems.edge.evse.chargepoint.keba.common.KebaModbusTest.testKebaModbusChannels;
import static io.openems.edge.evse.chargepoint.keba.common.KebaTest.testKebaChannels;
import static io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity.DEBUG_LOG;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.evse.chargepoint.keba.common.EvseKeba;
import io.openems.edge.evse.chargepoint.keba.common.KebaModbus;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseChargePointKebaModbusImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new EvseKebaModbusImpl();
		final var tc = new TestCase() //
				.activateStrictMode();
		testElectricityMeterChannels(tc);
		testKebaChannels(tc);
		testKebaModbusChannels(tc);
		testEvseKebaChannels(tc);

		prepareKebaModbus(sut) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setWiring(THREE_PHASE) //
						.setP30hasS10PhaseSwitching(false) //
						.setPhaseRotation(L2_L3_L1) //
						.setLogVerbosity(DEBUG_LOG) //
						.build()) //
				.next(new TestCase(), 20) //
				.next(tc) //
				.deactivate();

		assertEquals("L:5678 W|SetCurrent:UNDEFINED|SetEnable:-1:Undefined", sut.debugLog());
	}

	@Test
	public void testFirmwareHandling() throws OpenemsException, Exception {
		final var sut = new EvseKebaModbusImpl();
		final var test = prepareKebaModbus(sut) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setWiring(THREE_PHASE) //
						.setP30hasS10PhaseSwitching(false) //
						.setPhaseRotation(L2_L3_L1) //
						.setLogVerbosity(DEBUG_LOG) //
						.build());
		final var bridge = (DummyModbusBridge) sut.getBridgeModbus();

		test //
				// Firmware is outdated (before 1.1.9)
				.next(new TestCase().onBeforeProcessImage(() -> bridge //
						.withRegisters(1018, // FIRMWARE: 1.1.8
								new int[] { 0x0000, 0x277C }))) //
				.next(new TestCase(), 14) //
				.next(new TestCase()//
						.output(KebaModbus.ChannelId.DEVICE_SOFTWARE_OUTDATED, true)) //

				// Firmware is NOT outdated (from 1.1.9)
				.next(new TestCase().onBeforeProcessImage(() -> bridge //
						.withRegisters(1018, // FIRMWARE: 1.1.99
								new int[] { 0x0000, 0x277D }))) //
				.next(new TestCase(), 14) //
				.next(new TestCase()//
						.output(KebaModbus.ChannelId.DEVICE_SOFTWARE_OUTDATED, false)) //

				// Firmware has energy scale factor bug (before 1.2.1) -> Scale-Factor is 1
				.next(new TestCase().onBeforeProcessImage(() -> bridge //
						.withRegisters(1018, // FIRMWARE: 1.2.0
								new int[] { 0x0000, 0x27D8 }))) //
				.next(new TestCase(), 14) //
				.next(new TestCase()//
						.output(EvseKeba.ChannelId.ENERGY_SESSION, 65300) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 7747835L) //
						.output(KebaModbus.ChannelId.DEVICE_SOFTWARE_OUTDATED, false)) //

				// Firmware fixed energy scale factor bug (from 1.2.1) -> Scale-Factor is 0.1
				.next(new TestCase()) //
				.next(new TestCase().onBeforeProcessImage(() -> bridge //
						.withRegisters(1018, // FIRMWARE: 1.2.1
								new int[] { 0x0000, 0x27D9 }))) //
				.next(new TestCase(), 25) //
				.next(new TestCase()//
						.output(EvseKeba.ChannelId.ENERGY_SESSION, 6530)
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 774784L));
	}
}