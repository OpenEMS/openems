package io.openems.edge.meter.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.test.TestUtils;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ElectricityMeterTest {

	@Test
	public void testCalculateSumActivePowerFromPhases() {
		var sut = new DummyElectricityMeter("meter0"); //

		// Without calculateSumActivePowerFromPhases
		sut.withActivePowerL1(1000);
		sut.withActivePowerL2(200);
		sut.withActivePowerL3(30);
		assertEquals(null, sut.getActivePower().get());
		assertEquals(1000, sut.getActivePowerL1().get().intValue());
		assertEquals(200, sut.getActivePowerL2().get().intValue());
		assertEquals(30, sut.getActivePowerL3().get().intValue());

		// Activate
		ElectricityMeter.calculateSumActivePowerFromPhases(sut);
		sut.withActivePowerL1(2000);
		sut.withActivePowerL2(300);
		sut.withActivePowerL3(40);
		TestUtils.activateNextProcessImage(sut);
		assertEquals(2340, sut.getActivePower().get().intValue());
	}

	@Test
	public void testCalculateSumReactivePowerFromPhases() {
		var sut = new DummyElectricityMeter("meter0"); //

		// Without calculateSumReactivePowerFromPhases
		sut.withReactivePowerL1(1000);
		sut.withReactivePowerL2(200);
		sut.withReactivePowerL3(30);
		assertEquals(null, sut.getReactivePower().get());
		assertEquals(1000, sut.getReactivePowerL1().get().intValue());
		assertEquals(200, sut.getReactivePowerL2().get().intValue());
		assertEquals(30, sut.getReactivePowerL3().get().intValue());

		// Activate
		ElectricityMeter.calculateSumReactivePowerFromPhases(sut);
		sut.withReactivePowerL1(2000);
		sut.withReactivePowerL2(300);
		sut.withReactivePowerL3(40);
		TestUtils.activateNextProcessImage(sut);
		assertEquals(2340, sut.getReactivePower().get().intValue());
	}

	@Test
	public void testCalculateSumCurrentFromPhases() {
		var sut = new DummyElectricityMeter("meter0"); //

		// Without calculateSumCurrentFromPhasess
		sut.withCurrentL1(1000);
		sut.withCurrentL2(200);
		sut.withCurrentL3(30);
		assertEquals(null, sut.getCurrent().get());
		assertEquals(1000, sut.getCurrentL1().get().intValue());
		assertEquals(200, sut.getCurrentL2().get().intValue());
		assertEquals(30, sut.getCurrentL3().get().intValue());

		// Activate
		ElectricityMeter.calculateSumCurrentFromPhases(sut);
		sut.withCurrentL1(2000);
		sut.withCurrentL2(300);
		sut.withCurrentL3(40);
		TestUtils.activateNextProcessImage(sut);
		assertEquals(2340, sut.getCurrent().get().intValue());
	}

	@Test
	public void testCalculateAverageVoltageFromPhases() {
		var sut = new DummyElectricityMeter("meter0"); //

		// Without calculateAverageVoltageFromPhases
		sut.withVoltageL1(1000);
		sut.withVoltageL2(200);
		sut.withVoltageL3(30);
		assertEquals(null, sut.getVoltage().get());
		assertEquals(1000, sut.getVoltageL1().get().intValue());
		assertEquals(200, sut.getVoltageL2().get().intValue());
		assertEquals(30, sut.getVoltageL3().get().intValue());

		// Activate
		ElectricityMeter.calculateAverageVoltageFromPhases(sut);
		sut.withVoltageL1(2000);
		sut.withVoltageL2(300);
		sut.withVoltageL3(40);
		TestUtils.activateNextProcessImage(sut);
		assertEquals(780, sut.getVoltage().get().intValue());
	}

	@Test
	public void testCalculateSumActiveProductionEnergyFromPhases() {
		var sut = new DummyElectricityMeter("meter0"); //

		// Without calculateSumActiveProductionEnergyFromPhases
		sut.withActiveProductionEnergyL1(1000);
		sut.withActiveProductionEnergyL2(200);
		sut.withActiveProductionEnergyL3(30);
		assertEquals(null, sut.getActiveProductionEnergy().get());
		assertEquals(1000, sut.getActiveProductionEnergyL1().get().intValue());
		assertEquals(200, sut.getActiveProductionEnergyL2().get().intValue());
		assertEquals(30, sut.getActiveProductionEnergyL3().get().intValue());

		// Activate
		ElectricityMeter.calculateSumActiveProductionEnergyFromPhases(sut);
		sut.withActiveProductionEnergyL1(2000);
		sut.withActiveProductionEnergyL2(300);
		sut.withActiveProductionEnergyL3(40);
		TestUtils.activateNextProcessImage(sut);
		assertEquals(2340, sut.getActiveProductionEnergy().get().intValue());
	}

	@Test
	public void testCalculatePhasesFromActivePower() {
		var sut = new DummyElectricityMeter("meter0"); //

		// Without calculatePhasesFromActivePower
		sut.withActivePower(3000);
		assertEquals(3000, sut.getActivePower().get().intValue());
		assertEquals(null, sut.getActivePowerL1().get());
		assertEquals(null, sut.getActivePowerL2().get());
		assertEquals(null, sut.getActivePowerL3().get());

		// Activate
		ElectricityMeter.calculatePhasesFromActivePower(sut);
		sut.withActivePower(3000);
		TestUtils.activateNextProcessImage(sut);
		assertEquals(1000, sut.getActivePowerL1().get().intValue());
		assertEquals(1000, sut.getActivePowerL2().get().intValue());
		assertEquals(1000, sut.getActivePowerL3().get().intValue());
	}

	@Test
	public void testCalculatePhasesFromReactivePower() {
		var sut = new DummyElectricityMeter("meter0"); //

		// Without calculatePhasesFromReactivePower
		sut.withReactivePower(3000);
		assertEquals(3000, sut.getReactivePower().get().intValue());
		assertEquals(null, sut.getReactivePowerL1().get());
		assertEquals(null, sut.getReactivePowerL2().get());
		assertEquals(null, sut.getReactivePowerL3().get());

		// Activate
		ElectricityMeter.calculatePhasesFromReactivePower(sut);
		sut.withReactivePower(3000);
		TestUtils.activateNextProcessImage(sut);
		assertEquals(1000, sut.getReactivePowerL1().get().intValue());
		assertEquals(1000, sut.getReactivePowerL2().get().intValue());
		assertEquals(1000, sut.getReactivePowerL3().get().intValue());
	}

}
