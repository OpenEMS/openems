package io.openems.edge.ess.core.power;

import static io.openems.edge.ess.power.api.SolverStrategy.OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class KeepAllNearEqualTest {

	private static AtomicInteger openCallbacks;
	private static final int MAX_POWER = 92000; // in Watts

	@Before
	public void before() {
		openCallbacks = new AtomicInteger(0);
	}

	@After
	public void after() {
		assertEquals("Not all Callbacks were actually called", 0, openCallbacks.get());
	}

	@Test
	public void testNearEqualStrategy() throws Exception {
		EssPower powerComponent = new EssPowerImpl();
		

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(9);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(9);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(8);
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(13);
		var ess5 = new DummyManagedSymmetricEss("ess5") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(9);
		var ess6 = new DummyManagedSymmetricEss("ess6") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(17);
		var ess7 = new DummyManagedSymmetricEss("ess7") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(7);
		var ess8 = new DummyManagedSymmetricEss("ess8") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(17);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4, ess5, ess6, ess7, ess8) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.addReference("addEss", ess4) //
				.addReference("addEss", ess5) //
				.addReference("addEss", ess6) //
				.addReference("addEss", ess7) //
				.addReference("addEss", ess8) //
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(true) //
						.setEnablePid(false) //
						.build()); //

		expect("#2.1", ess1, -1024, 0);
		expect("#2.2", ess2, -1024, 0);
		expect("#2.3", ess3, -1036, 0);
		expect("#2.4", ess4, -979, 0);
		expect("#2.5", ess5, -1024, 0);
		expect("#2.6", ess6, -934, 0);
		expect("#2.7", ess7, -1047, 0);
		expect("#2.8", ess8, -934, 0);

		ess0.setActivePowerEquals(-8000);
		componentTest.next(new TestCase("#1"));

		expect("#3.1", ess1, -1024, 0);
		expect("#3.2", ess2, -1024, 0);
		expect("#3.3", ess3, -1036, 0);
		expect("#3.4", ess4, -979, 0);
		expect("#3.5", ess5, -1024, 0);
		expect("#3.6", ess6, -934, 0);
		expect("#3.7", ess7, -1047, 0);
		expect("#3.8", ess8, -934, 0);

		ess0.setActivePowerEquals(-8000);
		componentTest.next(new TestCase("#3"));

		expect("#4.1", ess1, -91999, 0);
		expect("#4.2", ess2, -91999, 0);
		expect("#4.3", ess3, -92000, 0);
		expect("#4.4", ess4, -92000, 0);
		expect("#4.5", ess5, -91999, 0);
		expect("#4.6", ess6, -92000, 0);
		expect("#4.7", ess7, -91999, 0);
		expect("#4.8", ess8, -91999, 0);

		// charging with 1 MW
		ess0.setActivePowerEquals(-1000000);
		componentTest.next(new TestCase("#4"));

		expect("#5.1", ess1, -91999, 0);
		expect("#5.2", ess2, -91999, 0);
		expect("#5.3", ess3, -92000, 0);
		expect("#5.4", ess4, -92000, 0);
		expect("#5.5", ess5, -91999, 0);
		expect("#5.6", ess6, -92000, 0);
		expect("#5.7", ess7, -91999, 0);
		expect("#5.8", ess8, -91999, 0);

		// Charging with 4 W less the maximum
		ess0.setActivePowerEquals(-735996);
		componentTest.next(new TestCase("#5"));

		expect("#6.1", ess1, -91999, 0);
		expect("#6.2", ess2, -91999, 0);
		expect("#6.3", ess3, -92000, 0);
		expect("#6.4", ess4, -92000, 0);
		expect("#6.5", ess5, -91999, 0);
		expect("#6.6", ess6, -92000, 0);
		expect("#6.7", ess7, -91999, 0);
		expect("#6.8", ess8, -91999, 0);

		// Charging with maximum power
		ess0.setActivePowerEquals(-736000);
		componentTest.next(new TestCase("#4"));

		expect("#7.1", ess1, 92000, 0);
		expect("#7.2", ess2, 92000, 0);
		expect("#7.3", ess3, 92000, 0);
		expect("#7.4", ess4, 92000, 0);
		expect("#7.5", ess5, 92000, 0);
		expect("#7.6", ess6, 92000, 0);
		expect("#7.7", ess7, 92000, 0);
		expect("#7.8", ess8, 92000, 0);

		// Discharging Charging with maximum power
		ess0.setActivePowerEquals(736000);
		componentTest.next(new TestCase("#7"));

		expect("#8.1", ess1, 92000, 0);
		expect("#8.2", ess2, 92000, 0);
		expect("#8.3", ess3, 92000, 0);
		expect("#8.4", ess4, 92000, 0);
		expect("#8.5", ess5, 92000, 0);
		expect("#8.6", ess6, 92000, 0);
		expect("#8.7", ess7, 92000, 0);
		expect("#8.8", ess8, 92000, 0);

		// Discharging with more than maximum power
		ess0.setActivePowerEquals(1000000);
		componentTest.next(new TestCase("#7"));

		expect("#9.1", ess1, 90857, 0);
		expect("#9.2", ess2, 90857, 0);
		expect("#9.3", ess3, 80761, 0);
		expect("#9.4", ess4, 92000, 0);
		expect("#9.5", ess5, 90857, 0);
		expect("#9.6", ess6, 92000, 0);
		expect("#9.7", ess7, 70666, 0);
		expect("#9.8", ess8, 92000, 0);

		// Discharging with maximum power
		ess0.setActivePowerEquals(700000);
		componentTest.next(new TestCase("#7"));

		expect("#10.1", ess1, 92000, 0);
		expect("#10.2", ess2, 92000, 0);
		expect("#10.3", ess3, 92000, 0);
		expect("#10.4", ess4, 92000, 0);
		expect("#10.5", ess5, 92000, 0);
		expect("#10.6", ess6, 92000, 0);
		expect("#10.7", ess7, 91996, 0);
		expect("#10.8", ess8, 92000, 0);

		// Discharging with 4 W less then maximum power
		ess0.setActivePowerEquals(735996);
		componentTest.next(new TestCase("#10"));

	}

	@Test
	public void testFaultConditions() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(9);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(9);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(8);
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(13);
		var ess5 = new DummyManagedSymmetricEss("ess5") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(9);
		var ess6 = new DummyManagedSymmetricEss("ess6") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(17);
		var ess7 = new DummyManagedSymmetricEss("ess7") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(7);
		var ess8 = new DummyManagedSymmetricEss("ess8") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(17);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4, ess5, ess6, ess7, ess8) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.addReference("addEss", ess4) //
				.addReference("addEss", ess5) //
				.addReference("addEss", ess6) //
				.addReference("addEss", ess7) //
				.addReference("addEss", ess8) //
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#0.1", ess1, -1024, 0);
		expect("#0.2", ess2, -1024, 0);
		expect("#0.3", ess3, -1036, 0);
		expect("#0.4", ess4, -979, 0);
		expect("#0.5", ess5, -1024, 0);
		expect("#0.6", ess6, -934, 0);
		expect("#0.7", ess7, -1047, 0);
		expect("#0.8", ess8, -934, 0);

		ess0.setActivePowerEquals(-8000);
		componentTest.next(new TestCase("#1"));

		ess1.withState(Level.FAULT);
		expect("#1.1", ess1, 0, 0);
		expect("#1.2", ess2, -1175, 0);
		expect("#1.3", ess3, -1188, 0);
		expect("#1.4", ess4, -1123, 0);
		expect("#1.5", ess5, -1175, 0);
		expect("#1.6", ess6, -1071, 0);
		expect("#1.7", ess7, -1200, 0);
		expect("#1.8", ess8, -1071, 0);

		ess0.setActivePowerEquals(-8000);
		componentTest.next(new TestCase("#1"));

		ess1.withState(Level.FAULT);
		ess2.withState(Level.FAULT);
		expect("#1.1", ess1, 0, 0);
		expect("#1.2", ess2, 0, 0);
		expect("#1.3", ess3, -1392, 0);
		expect("#1.4", ess4, -1316, 0);
		expect("#1.5", ess5, -1377, 0);
		expect("#1.6", ess6, -1256, 0);
		expect("#1.7", ess7, -1407, 0);
		expect("#1.8", ess8, -1256, 0);

		ess0.setActivePowerEquals(-8000);
		componentTest.next(new TestCase("#1"));

	}

	@Test
	public void testExtremePowerLimits() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-10000) // Very limited charge power
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(50);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(5000) // Very limited discharge power
				.withMaxApparentPower(50000) //
				.withSoc(45);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(50);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test discharge with limited power on ess2
		expect("#1.1", ess1, 47500, 0); // Gets more power due to ess2 limitation
		expect("#1.2", ess2, 5000, 0); // Limited to its max discharge power
		expect("#1.3", ess3, 47500, 0); // Gets more power due to ess2 limitation

		ess0.setActivePowerEquals(100000); // 100kW discharge
		componentTest.next(new TestCase("#1"));

		// Test charge with limited power on ess1
		expect("#2.1", ess1, -10000, 0); // Limited to its max charge power
		expect("#2.2", ess2, -40000, 0); // Gets more charge due to ess1 limitation
		expect("#2.3", ess3, -50000, 0); // Gets more charge due to ess1 limitation

		ess0.setActivePowerEquals(-100000); // 100kW charge
		componentTest.next(new TestCase("#2"));
	}

	@Test
	public void testZeroPowerScenarios() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(50);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(50);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test zero power request
		expect("#1.1", ess1, 0, 0);
		expect("#1.2", ess2, 0, 0);

		ess0.setActivePowerEquals(0); // Zero power
		componentTest.next(new TestCase("#1"));

		// Test very small power request
		expect("#2.1", ess1, 10, 0);
		expect("#2.2", ess2, 10, 0);

		ess0.setActivePowerEquals(20); // 2W discharge
		componentTest.next(new TestCase("#2"));

		// Test negative very small power request
		expect("#3.1", ess1, 0, 0);
		expect("#3.2", ess2, 0, 0);

		ess0.setActivePowerEquals(-2); // 2W charge
		componentTest.next(new TestCase("#3"));
	}

	@Test
	public void testAsymmetricPowerLimits() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-20000) //
				.withAllowedDischargePower(60000) //
				.withMaxApparentPower(60000) //
				.withSoc(30);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-60000) //
				.withAllowedDischargePower(20000) //
				.withMaxApparentPower(60000) //
				.withSoc(70);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-40000) //
				.withAllowedDischargePower(40000) //
				.withMaxApparentPower(40000) //
				.withSoc(50);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.addReference("addEss", ess3) //
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test discharge with different limits and SOCs
		expect("#1.1", ess1, 40000, 0); // High SOC ESS gets max discharge
		expect("#1.2", ess2, 20000, 0); // Limited by max discharge power
		expect("#1.3", ess3, 40000, 0); // Gets remaining power

		ess0.setActivePowerEquals(100000); // 100kW discharge
		componentTest.next(new TestCase("#1"));

		// Test charge with different limits and SOCs
		expect("#2.1", ess1, 0, 0); // Limited by max charge power
		expect("#2.2", ess2, -60000, 0); // Gets more charge due to low SOC
		expect("#2.3", ess3, -40000, 0); // Gets remaining charge power

		ess0.setActivePowerEquals(-100000); // 100kW charge
		componentTest.next(new TestCase("#2"));
	}

	@Test
	public void testSingleEssCluster() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(50);

		var ess0 = new DummyMetaEss("ess0", ess1) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test single ESS gets all requested power
		expect("#1.1", ess1, 30000, 0);

		ess0.setActivePowerEquals(30000); // 30kW discharge
		componentTest.next(new TestCase("#1"));

		// Test single ESS charge
		expect("#2.1", ess1, -30000, 0);

		ess0.setActivePowerEquals(-30000); // 30kW charge
		componentTest.next(new TestCase("#2"));

		// Test power beyond limits
		expect("#3.1", ess1, 50000, 0); // Limited to max discharge

		ess0.setActivePowerEquals(80000); // Request more than available
		componentTest.next(new TestCase("#3"));
	}

	@Test
	public void testScenarioOne() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		// Test extreme asymmetric limits
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-70800) //
				.withAllowedDischargePower(67260) //
				.withMaxApparentPower(92000) //
				.withSoc(76);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-10590) //
				.withAllowedDischargePower(92000) //
				.withMaxApparentPower(92000) //
				.withSoc(64);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#1.1", ess1, -6000, 0); //
		expect("#1.2", ess2, -9000, 0); //

		ess0.setActivePowerEquals(-15000); //
		componentTest.next(new TestCase("#1"));

	}

	@Test
	public void testExtremeCasesOfEssLimits() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		// Test extreme asymmetric limits
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(0) // Cannot charge (100% SOC)
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(100);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(0) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(0);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#1.1", ess1, 0, 0); //
		expect("#1.2", ess2, -MAX_POWER, 0); //

		ess0.setActivePowerEquals(-MAX_POWER); //
		componentTest.next(new TestCase("#1"));

		clearCallbacks(ess1, ess2);

		expect("#2.1", ess1, 0, 0); // Cannot charge, so gets 0
		expect("#2.2", ess2, -MAX_POWER, 0); // Should be limited to its max

		ess0.setActivePowerEquals(-100000); // Request 100kW charge (more than ess2 can handle)
		componentTest.next(new TestCase("#2"));

		// Test #3: After SOC changes - should still respect power limits
		clearCallbacks(ess1, ess2);

		expect("#3.1", ess1, 0, 0); // Still cannot charge
		expect("#3.2", ess2, -50000, 0); // Should get requested charge power

		ess0.setActivePowerEquals(-50000); // Request 50kW charge (within ess2 limits)
		componentTest.next(new TestCase("#3"));

		// Test #4: Discharge scenario
		clearCallbacks(ess1, ess2);

		expect("#4.1", ess1, 50000, 0); // Should get requested discharge power
		expect("#4.2", ess2, 0, 0); // Cannot discharge

		ess0.setActivePowerEquals(50000); // Request 50kW discharge
		componentTest.next(new TestCase("#4"));

		clearCallbacks(ess1, ess2);

		expect("#5.1", ess1, -0, 0); // Limited charge power due to high SOC
		expect("#5.2", ess2, -50000, 0); // Gets most of the charge power

		ess0.setActivePowerEquals(-50000); // Request 50kW charge
		componentTest.next(new TestCase("#5"));

		clearCallbacks(ess1, ess2);

	}

	@Test
	public void testEqualDistributionWithSimilarLimits() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var p1 = calculateAllowedPower(50); // [-46000, 46000]
		var p2 = calculateAllowedPower(50); // [-46000, 46000]

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(p1[0]) // -46kW
				.withAllowedDischargePower(p1[1]) // +46kW
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(50);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(p2[0]) // -46kW
				.withAllowedDischargePower(p2[1]) // +46kW
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(50);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2) //
				.setPower(powerComponent);

		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(EssPower.SINGLETON_SERVICE_PID);

		final var componentTest = new ComponentTest(powerComponent) //
				.addReference("cm", cm) //
				.addReference("addEss", ess0) //
				.addReference("addEss", ess1) //
				.addReference("addEss", ess2) //
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test equal distribution - both ESS should get -25kW each
		expect("Equal#1.1", ess1, -25000, 0);
		expect("Equal#1.2", ess2, -25000, 0);

		ess0.setActivePowerEquals(-50000); // Request 50kW charge
		componentTest.next(new TestCase("Equal#1"));

		clearCallbacks(ess1, ess2);
	}

	private static void expect(String description, DummyManagedSymmetricEss ess, int p, int q) {
		openCallbacks.incrementAndGet();
		ess.withSymmetricApplyPowerCallback(record -> {
			openCallbacks.decrementAndGet();
			assertEquals(description + " for " + ess.id(), p, record.activePower());
			assertEquals(description + " for " + ess.id(), q, record.reactivePower());
		});
	}

	private static void clearCallbacks(DummyManagedSymmetricEss... essList) {
		for (DummyManagedSymmetricEss ess : essList) {
			ess.withSymmetricApplyPowerCallback(null); // Clear callback
		}
	}

	/**
	 * Simple naive method to get the allowed charge and discharge power based on
	 * soc.
	 * 
	 * @param soc soc passed to calculated the powers
	 * @return int [] for allowed charge and discharge power
	 */
	public static int[] calculateAllowedPower(double soc) {
		soc = Math.max(0, Math.min(soc, 100));

		int allowedChargePower = (int) Math.round(-MAX_POWER * (100 - soc) / 100.0);
		int allowedDischargePower = (int) Math.round(MAX_POWER * soc / 100.0);

		return new int[] { allowedChargePower, allowedDischargePower };
	}

	@Test
	public void testCalculateAllowedPower() throws Exception {
		var p = calculateAllowedPower(50);
		var expected = new int[] { -46000, 46000 };
		assertArrayEquals(expected, p);
	}

}