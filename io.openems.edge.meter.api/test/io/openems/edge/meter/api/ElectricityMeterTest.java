package io.openems.edge.meter.api;

import static io.openems.edge.common.test.TestUtils.activateNextProcessImage;
import static io.openems.edge.meter.api.ElectricityMeter.calculateCurrentsFromActivePowerAndVoltage;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.meter.test.DummyElectricityMeter;

public class ElectricityMeterTest {

	@Test
	public void testCalculateSumActivePowerFromPhases() {
		// Without calculateSumActivePowerFromPhases
		var sut = new DummyElectricityMeter("meter0") //
				.withActivePowerL1(1000) //
				.withActivePowerL2(200) //
				.withActivePowerL3(30);
		assertEquals(null, sut.getActivePower().get());
		assertEquals(1000, sut.getActivePowerL1().get().intValue());
		assertEquals(200, sut.getActivePowerL2().get().intValue());
		assertEquals(30, sut.getActivePowerL3().get().intValue());

		// Activate
		ElectricityMeter.calculateSumActivePowerFromPhases(sut);
		sut //
				.withActivePowerL1(2000) //
				.withActivePowerL2(300) //
				.withActivePowerL3(40);

		activateNextProcessImage(sut);
		assertEquals(2340, sut.getActivePower().get().intValue());
	}

	@Test
	public void testCalculateSumReactivePowerFromPhases() {
		// Without calculateSumReactivePowerFromPhases
		var sut = new DummyElectricityMeter("meter0") //
				.withReactivePowerL1(1000) //
				.withReactivePowerL2(200) //
				.withReactivePowerL3(30);
		assertEquals(null, sut.getReactivePower().get());
		assertEquals(1000, sut.getReactivePowerL1().get().intValue());
		assertEquals(200, sut.getReactivePowerL2().get().intValue());
		assertEquals(30, sut.getReactivePowerL3().get().intValue());

		// Activate
		ElectricityMeter.calculateSumReactivePowerFromPhases(sut);
		sut //
				.withReactivePowerL1(2000) //
				.withReactivePowerL2(300) //
				.withReactivePowerL3(40);

		activateNextProcessImage(sut);
		assertEquals(2340, sut.getReactivePower().get().intValue());
	}

	@Test
	public void testCalculateSumCurrentFromPhases() {
		// Without calculateSumCurrentFromPhasess
		var sut = new DummyElectricityMeter("meter0") //
				.withCurrentL1(1000) //
				.withCurrentL2(200) //
				.withCurrentL3(30);
		assertEquals(null, sut.getCurrent().get());
		assertEquals(1000, sut.getCurrentL1().get().intValue());
		assertEquals(200, sut.getCurrentL2().get().intValue());
		assertEquals(30, sut.getCurrentL3().get().intValue());

		// Activate
		ElectricityMeter.calculateSumCurrentFromPhases(sut);
		sut //
				.withCurrentL1(2000) //
				.withCurrentL2(300) //
				.withCurrentL3(40);

		activateNextProcessImage(sut);
		assertEquals(2340, sut.getCurrent().get().intValue());
	}

	@Test
	public void testCalculateAverageVoltageFromPhases() {
		// Without calculateAverageVoltageFromPhases
		var sut = new DummyElectricityMeter("meter0") //
				.withVoltageL1(1000) //
				.withVoltageL2(200) //
				.withVoltageL3(30);
		assertEquals(null, sut.getVoltage().get());
		assertEquals(1000, sut.getVoltageL1().get().intValue());
		assertEquals(200, sut.getVoltageL2().get().intValue());
		assertEquals(30, sut.getVoltageL3().get().intValue());

		// Activate
		ElectricityMeter.calculateAverageVoltageFromPhases(sut);
		sut //
				.withVoltageL1(2000) //
				.withVoltageL2(300) //
				.withVoltageL3(40);

		activateNextProcessImage(sut);
		assertEquals(780, sut.getVoltage().get().intValue());
	}

	@Test
	public void testCalculateSumActiveProductionEnergyFromPhases() {
		// Without calculateSumActiveProductionEnergyFromPhases
		var sut = new DummyElectricityMeter("meter0") //
				.withActiveProductionEnergyL1(1000) //
				.withActiveProductionEnergyL2(200) //
				.withActiveProductionEnergyL3(30);
		assertEquals(null, sut.getActiveProductionEnergy().get());
		assertEquals(1000, sut.getActiveProductionEnergyL1().get().intValue());
		assertEquals(200, sut.getActiveProductionEnergyL2().get().intValue());
		assertEquals(30, sut.getActiveProductionEnergyL3().get().intValue());

		// Activate
		ElectricityMeter.calculateSumActiveProductionEnergyFromPhases(sut);
		sut //
				.withActiveProductionEnergyL1(2000) //
				.withActiveProductionEnergyL2(300) //
				.withActiveProductionEnergyL3(40);

		activateNextProcessImage(sut);
		assertEquals(2340, sut.getActiveProductionEnergy().get().intValue());
	}

	@Test
	public void testCalculatePhasesFromActivePower() {
		// Without calculatePhasesFromActivePower
		var sut = new DummyElectricityMeter("meter0") //
				.withActivePower(3000);
		assertEquals(3000, sut.getActivePower().get().intValue());
		assertEquals(null, sut.getActivePowerL1().get());
		assertEquals(null, sut.getActivePowerL2().get());
		assertEquals(null, sut.getActivePowerL3().get());

		// Activate
		ElectricityMeter.calculatePhasesFromActivePower(sut);
		sut.withActivePower(3000);
		activateNextProcessImage(sut);

		assertEquals(1000, sut.getActivePowerL1().get().intValue());
		assertEquals(1000, sut.getActivePowerL2().get().intValue());
		assertEquals(1000, sut.getActivePowerL3().get().intValue());
	}

	@Test
	public void testCalculatePhasesFromReactivePower() {
		// Without calculatePhasesFromReactivePower
		var sut = new DummyElectricityMeter("meter0") //
				.withReactivePower(3000);
		assertEquals(3000, sut.getReactivePower().get().intValue());
		assertEquals(null, sut.getReactivePowerL1().get());
		assertEquals(null, sut.getReactivePowerL2().get());
		assertEquals(null, sut.getReactivePowerL3().get());

		// Activate
		ElectricityMeter.calculatePhasesFromReactivePower(sut);
		sut.withReactivePower(3000);

		activateNextProcessImage(sut);
		assertEquals(1000, sut.getReactivePowerL1().get().intValue());
		assertEquals(1000, sut.getReactivePowerL2().get().intValue());
		assertEquals(1000, sut.getReactivePowerL3().get().intValue());
	}

	@Test
	public void testCalculatePhasesFromVoltage() {
		var sut = new DummyElectricityMeter("meter0") //
				.withVoltage(231_000);
		assertEquals(null, sut.getVoltageL1().get());
		assertEquals(null, sut.getVoltageL2().get());
		assertEquals(null, sut.getVoltageL3().get());

		ElectricityMeter.calculatePhasesFromVoltage(sut);
		sut.withVoltage(231_000);

		activateNextProcessImage(sut);
		assertEquals(231_000, sut.getVoltageL1().get().intValue());
		assertEquals(231_000, sut.getVoltageL2().get().intValue());
		assertEquals(231_000, sut.getVoltageL3().get().intValue());

	}

	@Test
	public void testCalculateCurrentsFromActivePowerAndVoltage() {
		var sut = prepareTestCalculateCurrentsStep1(233_000);
		prepareTestCalculateCurrentsStep2(sut, 233_000);

		assertEquals(4329, sut.getCurrentL1().get().intValue());
		assertEquals(8620, sut.getCurrentL2().get().intValue());
		assertEquals(12875, sut.getCurrentL3().get().intValue());
	}

	@Test
	public void testCalculateCurrentsFromActivePowerAndNullVoltage() {
		var sut = prepareTestCalculateCurrentsStep1(null);
		prepareTestCalculateCurrentsStep2(sut, null);

		assertEquals(4329, sut.getCurrentL1().get().intValue());
		assertEquals(8620, sut.getCurrentL2().get().intValue());
		assertEquals(null, sut.getCurrentL3().get());
	}

	private static DummyElectricityMeter prepareTestCalculateCurrentsStep1(Integer voltageL3) {
		var sut = new DummyElectricityMeter("meter0") //
				.withActivePowerL1(1000) //
				.withActivePowerL2(2000) //
				.withActivePowerL3(3000) //
				.withVoltageL1(231_000) //
				.withVoltageL2(232_000) //
				.withVoltageL3(voltageL3); //

		assertEquals(null, sut.getCurrentL1().get());
		assertEquals(null, sut.getCurrentL2().get());
		assertEquals(null, sut.getCurrentL3().get());
		return sut;
	}

	private static void prepareTestCalculateCurrentsStep2(DummyElectricityMeter sut, Integer voltageL3) {
		calculateCurrentsFromActivePowerAndVoltage(sut);
		sut //
				.withActivePowerL1(1000) //
				.withActivePowerL2(2000) //
				.withActivePowerL3(3000) //
				.withVoltageL1(231_000) //
				.withVoltageL2(232_000) //
				.withVoltageL3(voltageL3);

		activateNextProcessImage(sut);
	}

}
