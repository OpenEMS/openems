package io.openems.edge.ess.core.power.v1;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.SolverStrategy.OPTIMIZE_BY_PREFERRING_DC_POWER;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.core.power.EssPower;
import io.openems.edge.ess.core.power.EssPowerImpl;
import io.openems.edge.ess.core.power.MyConfig;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class PreferDcPowerTest {

	private static AtomicInteger openCallbacks;
	private static final int MAX_POWER = 10000; // in Watts

	@Before
	public void before() {
		openCallbacks = new AtomicInteger(0);
	}

	@After
	public void after() {
		assertEquals("Not all Callbacks were actually called", 0, openCallbacks.get());
	}

	@Test
	public void testPreferDcPowerStrategy() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(1000) //
				.withSoc(9);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(2000) //
				.withSoc(9);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(3000) //
				.withSoc(8);
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(4000) //
				.withSoc(13);
		var ess5 = new DummyManagedSymmetricEss("ess5") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(0) //
				.withSoc(50);
		var ess6 = new DummyManagedSymmetricEss("ess6") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(0) //
				.withSoc(60);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4, ess5, ess6) //
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
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(false) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test discharge with different limits and SOCs
		expect("#1.1", ess1, 1000, 0); // Pv Production ESS gets pv production as discharge
		expect("#1.2", ess2, 2000, 0); // Pv Production ESS gets pv production as discharge
		expect("#1.3", ess3, 1000, 0); // Pv Production ESS with lowest SOC get less pv production discharge
		expect("#1.4", ess4, 4000, 0); // Pv Production ESS gets pv production as discharge
		expect("#1.5", ess5, 0, 0); // ESS without pv production gets zero power, as pv production is sufficient
		expect("#1.6", ess6, 0, 0); // ESS without pv production gets zero power, as pv production is sufficient

		ess0.setActivePowerEqualsWithoutFilter(8000); // 8kW discharge
		componentTest.next(new TestCase("#1"));

		// Test discharge with different limits and SOCs & reactive power request -> Test USE_DISCHARGING_ESS WeightStrategy
		expect("#2.1", ess1, 1000, 520); // gets 1000/8000 -> 13% of 4000 -> 520W reactive power
		expect("#2.2", ess2, 2000, 1000); // gets 2000/8000 -> 25% of 4000 -> 1000W reactive power
		expect("#2.3", ess3, 1000, 480); // gets 1000/8000 -> 13% of 4000 -> 520W, reduced to remaining of 480W reactive power
		expect("#2.4", ess4, 4000, 2000); // gets 4000/8000 -> 50% of 4000 -> 2000W reactive power
		expect("#2.5", ess5, 0, 0); // idle inverter should not be used for reactive power
		expect("#2.6", ess6, 0, 0); // idle inverter should not be used for reactive power

		ess0.setActivePowerEqualsWithoutFilter(8000); // 8kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(4000); // 4kW reactive power -> should be distributed across all discharging inverters using feed-in ratio
		componentTest.next(new TestCase("#2"));

		// Test discharge with different limits and SOCs & reactive power request -> Test USE_DISCHARGING_ESS WeightStrategy
		expect("#3.1", ess1, 1000, -520); // gets 1000/8000 -> 13% of 4000 -> 520W reactive power
		expect("#3.2", ess2, 2000, -1000); // gets 2000/8000 -> 25% of 4000 -> 1000W reactive power
		expect("#3.3", ess3, 1000, -480); // gets 1000/8000 -> 13% of 4000 -> 520W, reduced to remaining of 480W reactive power
		expect("#3.4", ess4, 4000, -2000); // gets 4000/8000 -> 50% of 4000 -> 2000W reactive power
		expect("#3.5", ess5, 0, 0); // idle inverter should not be used for reactive power
		expect("#3.6", ess6, 0, 0); // idle inverter should not be used for reactive power

		ess0.setActivePowerEqualsWithoutFilter(8000); // 8kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(-4000); // -4kW reactive power -> should be distributed across all discharging inverters using feed-in ratio
		componentTest.next(new TestCase("#3"));

		// Test discharge with different limits and SOCs
		expect("#4.1", ess1, 4000, 0); // Pv Production ESS gets pv production as discharge + remaining discharge power (with lower priority due to lower SOC)
		expect("#4.2", ess2, 2000, 0); // Pv Production ESS gets pv production
		expect("#4.3", ess3, 3000, 0); // Pv Production ESS gets pv production
		expect("#4.4", ess4, 10000, 0); // Pv Production ESS gets pv production + remaining discharge power (as ess4 is already producing AC power with higher SOC) limited by max discharge power
		expect("#4.5", ess5, 0, 0); // High SOC ESS without pv production not used (inverter already producing AC power are preferred)
		expect("#4.6", ess6, 0, 0); // High SOC ESS without pv production not used (inverter already producing AC power are preferred)

		ess0.setActivePowerEqualsWithoutFilter(19000); // 19kW discharge
		componentTest.next(new TestCase("#4"));

		// Test discharge with different limits and SOCs & reactive power request
		expect("#5.1", ess1, 4000, 4000); // gets 4000/9000 -> 44% of 9000 -> 3960W reactive power + 44% of remaining 90W (rounding difference) -> 40W = 4000W reactive power
		expect("#5.2", ess2, 2000, 2000); // gets 2000/9000 -> 22% of 9000 -> 1980W reactive power + 22% of remaining 90W (rounding difference) -> 20W = 2000W reactive power
		expect("#5.3", ess3, 3000, 3000); // gets 3000/9000 -> 33% of 9000 -> 2970W reactive power + 33% of remaining 90W (rounding difference) -> 30W = 3000W reactive power
		expect("#5.4", ess4, 10000, 0); // running on full active power, no reactive power possible
		expect("#5.5", ess5, 0, 0); // idle inverter should not be used
		expect("#5.5", ess6, 0, 0); // idle inverter should not be used

		ess0.setActivePowerEqualsWithoutFilter(19000); // 19kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(9000); // 9000W reactive power -> should be distributed across all feed-in inverters using feed-in ratio
		componentTest.next(new TestCase("#5"));

		// Test discharge with different limits and SOCs & reactive power request
		expect("#6.1", ess1, 10000, 0); // Pv Production ESS gets pv production + remaining discharge power limited by max discharge power
		expect("#6.2", ess2, 10000, 0); // Pv Production ESS gets pv production + remaining discharge power limited by max discharge power
		expect("#6.3", ess3, 10000, 0); // Pv Production ESS gets pv production + remaining discharge power limited by max discharge power
		expect("#6.4", ess4, 10000, 0); // Pv Production ESS gets pv production + remaining discharge power limited by max discharge power
		expect("#6.5", ess5, 0, 0); // ESS without pv production
		expect("#6.6", ess6, 5000, 1000); // High SOC ESS without pv production gets remaining discharge power + reactive power

		ess0.setActivePowerEqualsWithoutFilter(45000); // 45kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(1000); // 1kW reactive power
		componentTest.next(new TestCase("#6"));

		// Test charge & reactive power request -> Test USE_IDLE_ESS WeightStrategy
		expect("#7.1", ess1, 0, -200); // reactive power distributed across all idle inverters using MaxApparentPower/MaxApparentPowerTotal
		expect("#7.2", ess2, 0, -200); // reactive power distributed across all idle inverters using MaxApparentPower/MaxApparentPowerTotal
		expect("#7.3", ess3, -8000, 0);
		expect("#7.4", ess4, 0, -200); // reactive power distributed across all idle inverters using MaxApparentPower/MaxApparentPowerTotal
		expect("#7.5", ess5, 0, -200); // reactive power distributed across all idle inverters using MaxApparentPower/MaxApparentPowerTotal
		expect("#7.6", ess6, 0, -200); // reactive power distributed across all idle inverters using MaxApparentPower/MaxApparentPowerTotal

		ess0.setActivePowerEqualsWithoutFilter(-8000); // 8kW charge
		ess0.setReactivePowerEqualsWithoutFilter(-1000); // -1kW reactive power
		componentTest.next(new TestCase("#7"));

		// Test discharge
		expect("#8.1", ess1, 10000, 0); // Pv production active
		expect("#8.2", ess2, 10000, 0); // Pv Production active
		expect("#8.3", ess3, 9996, 0); // Pv Production active and lowest SOC -> gets less discharge power
		expect("#8.4", ess4, 10000, 0); // Pv Production active
		expect("#8.5", ess5, 10000, 0); // No pv Production active
		expect("#8.6", ess6, 10000, 0); // No pv Production active

		ess0.setActivePowerEqualsWithoutFilter(59996); // Discharging with 4 W less than maximum power
		componentTest.next(new TestCase("#8"));

		// Test discharge after Pv production gets active on ess5 & ess6
		ess5.withPvProduction(1000);
		ess6.withPvProduction(1000);

		expect("#9.1", ess1, 10000, 0); // Pv Production active
		expect("#9.2", ess2, 10000, 0); // Pv Production active
		expect("#9.3", ess3, 9996, 0); // Pv Production active and lowest SOC -> gets less discharge power
		expect("#9.4", ess4, 10000, 0); // Pv Production active
		expect("#9.5", ess5, 10000, 0); // Pv Production active
		expect("#9.6", ess6, 10000, 0); // Pv Production active

		ess0.setActivePowerEqualsWithoutFilter(59996); // Discharging with 4 W less than maximum power
		componentTest.next(new TestCase("#9"));

		// Test discharge
		expect("#10.1", ess1, 10000, 0);
		expect("#10.2", ess2, 6996, 0);
		expect("#10.3", ess3, 3000, 0);
		expect("#10.4", ess4, 10000, 0);
		expect("#10.5", ess5, 10000, 0);
		expect("#10.6", ess6, 10000, 0);

		ess0.setActivePowerEqualsWithoutFilter(49996); // Discharging with 49996 W
		componentTest.next(new TestCase("#10"));

		// Test charge & reactive power request -> Test USE_IDLE_ESS + USE_ALL_ESS WeightStrategy
		expect("#11.1", ess1, 0, -10000); // MaxApparentPower/MaxApparentPowerTotal -> 10000/50000 -> 20% of -52000W -> -10400W limited by maxApparentPower of 10000 -> -10000W
		expect("#11.2", ess2, 0, -10000); // MaxApparentPower/MaxApparentPowerTotal -> 10000/50000 -> 20% of -52000W -> -10400W limited by maxApparentPower of 10000 -> -10000W
		expect("#11.3", ess3, -5000, -2000); // charging inverter should not be used -> gets only remaining power
		expect("#11.4", ess4, 0, -10000); // MaxApparentPower/MaxApparentPowerTotal -> 10000/50000 -> 20% of -52000W -> -10400W limited by maxApparentPower of 10000 -> -10000W
		expect("#11.5", ess5, 0, -10000); // MaxApparentPower/MaxApparentPowerTotal -> 10000/50000 -> 20% of -52000W -> -10400W limited by maxApparentPower of 10000 -> -10000W
		expect("#11.6", ess6, 0, -10000); // MaxApparentPower/MaxApparentPowerTotal -> 10000/50000 -> 20% of -52000W -> -10400W limited by maxApparentPower of 10000 -> -10000W

		ess0.setActivePowerEqualsWithoutFilter(-5000); // Charging with 5000 W
		ess0.setReactivePowerEqualsWithoutFilter(-52000); // -52kW reactive power request
		componentTest.next(new TestCase("#11"));

		// Test charge & reactive power request -> Test USE_IDLE_ESS + USE_ALL_ESS WeightStrategy
		expect("#12.1", ess1, 0, 10000); // MaxApparentPower/MaxApparentPowerTotal -> 10000/50000 -> 20% of 52000W -> 10400W limited by maxApparentPower of 10000 -> 10000W
		expect("#12.2", ess2, 0, 10000); // MaxApparentPower/MaxApparentPowerTotal -> 10000/50000 -> 20% of 52000W -> 10400W limited by maxApparentPower of 10000 -> 10000W
		expect("#12.3", ess3, -5000, 2000); // charging inverter should not be used -> gets only remaining power
		expect("#12.4", ess4, 0, 10000); // MaxApparentPower/MaxApparentPowerTotal -> 10000/50000 -> 20% of 52000W -> 10400W limited by maxApparentPower of 10000 -> 10000W
		expect("#12.5", ess5, 0, 10000); // MaxApparentPower/MaxApparentPowerTotal -> 10000/50000 -> 20% of 52000W -> 10400W limited by maxApparentPower of 10000 -> 10000W
		expect("#12.6", ess6, 0, 10000); // MaxApparentPower/MaxApparentPowerTotal -> 10000/50000 -> 20% of 52000W -> 10400W limited by maxApparentPower of 10000 -> 10000W

		ess0.setActivePowerEqualsWithoutFilter(-5000); // Charging with 5000 W
		ess0.setReactivePowerEqualsWithoutFilter(52000); // 52kW reactive power request
		componentTest.next(new TestCase("#12"));

		// Test charge & reactive power request -> Test USE_DISCHARGING_ESS WeightStrategy
		expect("#13.1", ess1, 0, 0);
		expect("#13.2", ess2, 0, 0);
		expect("#13.3", ess3, 0, 0);
		expect("#13.4", ess4, 0, 0);
		expect("#13.5", ess5, 0, 0);
		expect("#13.6", ess6, 1000, -2000);

		ess0.setActivePowerEqualsWithoutFilter(1000); // 1kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // 2kW reactive power request
		componentTest.next(new TestCase("#13"));

		// Test charge & reactive power request -> Test USE_DISCHARGING_ESS WeightStrategy
		expect("#14.1", ess1, 0, 0);
		expect("#14.2", ess2, 0, 0);
		expect("#14.3", ess3, 0, 0);
		expect("#14.4", ess4, 2000, -1000); // 2000/4000 -> 50% of -2kW = -1000W
		expect("#14.5", ess5, 1000, -500); // 1000/4000 -> 25% of -2kW = -500W
		expect("#14.6", ess6, 1000, -500); // 1000/4000 -> 25% of -2kW = -500W

		ess0.setActivePowerEqualsWithoutFilter(4000); // 4kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // -2kW reactive power request
		componentTest.next(new TestCase("#14"));

		// Test charge & reactive power request -> Test USE_ALL_ESS WeightStratey as all inverters are charging
		expect("#15.1", ess1, -10000, 0); // running on full active power, no reactive power possible
		expect("#15.2", ess2, -10000, 0); // running on full active power, no reactive power possible
		expect("#15.3", ess3, -10000, 0); // running on full active power, no reactive power possible
		expect("#15.4", ess4, -10000, 0); // running on full active power, no reactive power possible
		expect("#15.5", ess5, -10000, 0); // running on full active power, no reactive power possible
		expect("#15.6", ess6, -2000, -1000); // gets all reactive power (-1000 W)

		ess0.setActivePowerEqualsWithoutFilter(-52000); // Charging with 52000 W
		ess0.setReactivePowerEqualsWithoutFilter(-1000); // -1kW reactive power request
		componentTest.next(new TestCase("#15"));

		// Test charge & reactive power request -> Test solving using idle ess; distributed using order
		expect("#16.1", ess1, 0, 0);
		expect("#16.2", ess2, 0, 0);
		expect("#16.3", ess3, 0, 2); // power too small to distribute with weights, will be solved using order over idle ess
		expect("#16.4", ess4, 0, 0);
		expect("#16.5", ess5, 0, 0);
		expect("#16.6", ess6, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(0);
		ess0.setReactivePowerEqualsWithoutFilter(2); // 2W reactive power request
		componentTest.next(new TestCase("#16"));

		// Test charge & reactive power request -> Test solving using idle ess; distributed using order
		expect("#17.1", ess1, 0, 0);
		expect("#17.2", ess2, 0, 0);
		expect("#17.3", ess3, 0, -2); // power too small to distribute with weights, will be solved using order over idle ess
		expect("#17.4", ess4, 0, 0);
		expect("#17.5", ess5, 0, 0);
		expect("#17.6", ess6, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(0);
		ess0.setReactivePowerEqualsWithoutFilter(-2); // -2W reactive power request
		componentTest.next(new TestCase("#17"));

		// Test discharge & reactive power request -> Test solving using discharging ess; distributed using order
		expect("#18.1", ess1, 1000, 0);
		expect("#18.2", ess2, 1000, 0);
		expect("#18.3", ess3, 1000, 0);
		expect("#18.4", ess4, 1000, 0);
		expect("#18.5", ess5, 1000, 0);
		expect("#18.6", ess6, 1000, 2); // reactive power too small to distribute with weights, will be solved using order over discharging ess (highest SOC ESS first)

		ess1.withPvProduction(1000);
		ess2.withPvProduction(1000);
		ess3.withPvProduction(1000);
		ess4.withPvProduction(1000);
		ess5.withPvProduction(1000);
		ess6.withPvProduction(1000);
		ess0.setActivePowerEqualsWithoutFilter(6000);
		ess0.setReactivePowerEqualsWithoutFilter(2); // 2W reactive power request
		componentTest.next(new TestCase("#18"));

		// Test discharge & reactive power request -> Test solving using discharging ess; distributed using order
		expect("#19.1", ess1, 1000, 0);
		expect("#19.2", ess2, 1000, 0);
		expect("#19.3", ess3, 1000, 0);
		expect("#19.4", ess4, 1000, 0);
		expect("#19.5", ess5, 1000, 0);
		expect("#19.6", ess6, 1000, -2); // reactive power too small to distribute with weights, will be solved using order over discharging ess (highest SOC ESS first)

		ess0.setActivePowerEqualsWithoutFilter(6000);
		ess0.setReactivePowerEqualsWithoutFilter(-2); // -2W reactive power request
		componentTest.next(new TestCase("#19"));

		// Test discharge & reactive power request -> Test solving using discharging ess; distributed using order -> Test respect of lowerLimit
		expect("#20.1", ess1, 1000, 0);
		expect("#20.2", ess2, 1000, 0);
		expect("#20.3", ess3, 1000, 0);
		expect("#20.4", ess4, 1000, 0);
		expect("#20.5", ess5, 1000, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change -1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)
		expect("#20.6", ess6, 1000, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change -1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)

		ess6.setReactivePowerGreaterOrEquals(-1); // Check that lowerLimit will be respected
		ess0.setActivePowerEqualsWithoutFilter(6000);
		ess0.setReactivePowerEqualsWithoutFilter(-2); // -2W reactive power request
		componentTest.next(new TestCase("#20"));

		// Test discharge & reactive power request -> Test solving using discharging ess; distributed using order -> Test respect of upperLimit
		expect("#21.1", ess1, 1000, 0);
		expect("#21.2", ess2, 1000, 0);
		expect("#21.3", ess3, 1000, 0);
		expect("#21.4", ess4, 1000, 0);
		expect("#21.5", ess5, 1000, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change 1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)
		expect("#21.6", ess6, 1000, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change 1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)

		ess6.setReactivePowerLessOrEquals(1); // Check that upperLimit will be respected
		ess0.setActivePowerEqualsWithoutFilter(6000);
		ess0.setReactivePowerEqualsWithoutFilter(2); // 2W reactive power request
		componentTest.next(new TestCase("#21"));

		// Test charge & reactive power request -> Test solving using idle ess; distributed using order
		expect("#22.1", ess1, 0, 0);
		expect("#22.2", ess2, 0, -2); // reactive power too small to distribute with weights, will be solved using order over idle ess (order during charge -> lowest SOC ESS first)
		expect("#22.3", ess3, -4000, 0); // lowest soc ess will be skipped as not idle
		expect("#22.4", ess4, 0, 0);
		expect("#22.5", ess5, 0, 0);
		expect("#22.6", ess6, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(-4000);
		ess0.setReactivePowerEqualsWithoutFilter(-2); // -2W reactive power request
		componentTest.next(new TestCase("#22"));

		// Test charge & reactive power request -> Test solving using idle ess; distributed using order -> Test respect of lowerLimit
		expect("#23.1", ess1, 0, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change -1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)
		expect("#23.2", ess2, 0, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change -1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)
		expect("#23.3", ess3, -4000, 0); // lowest soc ess will be skipped as not idle
		expect("#23.4", ess4, 0, 0);
		expect("#23.5", ess5, 0, 0);
		expect("#23.6", ess6, 0, 0);

		ess2.setReactivePowerGreaterOrEquals(-1); // Check that lowerLimit will be respected
		ess0.setActivePowerEqualsWithoutFilter(-4000);
		ess0.setReactivePowerEqualsWithoutFilter(-2); // -2W reactive power request
		componentTest.next(new TestCase("#23"));

		// Test charge & reactive power request -> Test solving using idle ess; distributed using order -> Test respect of upperLimit
		expect("#24.1", ess1, 0, 0);
		expect("#24.2", ess2, 0, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change 1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)
		expect("#24.3", ess3, 0, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change 1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)
		expect("#24.4", ess4, 0, 0);
		expect("#24.5", ess5, 0, 0);
		expect("#24.6", ess6, 0, 0);

		ess1.setReactivePowerLessOrEquals(1); // Also set constraint to avoid solving using weights
		ess2.setReactivePowerLessOrEquals(1); // Also set constraint to avoid solving using weights
		ess3.setReactivePowerLessOrEquals(1); // Check that upperLimit will be respected
		ess4.setReactivePowerLessOrEquals(1); // Also set constraint to avoid solving using weights
		ess5.setReactivePowerLessOrEquals(1); // Also set constraint to avoid solving using weights
		ess6.setReactivePowerLessOrEquals(1); // Also set constraint to avoid solving using weights
		ess0.setActivePowerEqualsWithoutFilter(0);
		ess0.setReactivePowerEqualsWithoutFilter(2); // 2W reactive power request
		componentTest.next(new TestCase("#24"));

		// Test charge & reactive power request -> Test solving using all ess; distributed using order
		expect("#25.1", ess1, -1000, 0);
		expect("#25.2", ess2, -1000, 0);
		expect("#25.3", ess3, -1000, -2); // reactive power too small to distribute with weights, will be solved using order over all ess (order during charge -> lowest SOC ESS first)
		expect("#25.4", ess4, -1000, 0);
		expect("#25.5", ess5, -1000, 0);
		expect("#25.6", ess6, 0, 0); // FAULT state

		ess6.withState(Level.FAULT);
		ess1.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess2.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess3.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess4.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess5.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess0.setActivePowerEqualsWithoutFilter(-5000); // Charging with 5000 W
		ess0.setReactivePowerEqualsWithoutFilter(-2); // -2W reactive power request
		componentTest.next(new TestCase("#25"));

		// Test charge & reactive power request -> Test solving using all ess; distributed using order -> Test respect of lowerLimit
		expect("#26.1", ess1, -1000, 0);
		expect("#26.2", ess2, -1000, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change -1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)
		expect("#26.3", ess3, -1000, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change -1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)
		expect("#26.4", ess4, -1000, 0);
		expect("#26.5", ess5, -1000, 0);
		expect("#26.6", ess6, 0, 0); // FAULT state

		ess1.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess2.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess3.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess4.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess5.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess3.setReactivePowerGreaterOrEquals(-1); // Check that lowerLimit will be respected
		ess0.setActivePowerEqualsWithoutFilter(-5000); // Charging with 5000 W
		ess0.setReactivePowerEqualsWithoutFilter(-2); // -2W reactive power request
		componentTest.next(new TestCase("#26"));

		// Test charge & reactive power request -> Test solving using all ess; distributed using order -> Test respect of upperLimit
		expect("#27.1", ess1, -1000, 0);
		expect("#27.2", ess2, -1000, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change 1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)
		expect("#27.3", ess3, -1000, 0); // io.openems.edge.ess.core.power.data.InverterPrecision will change 1 to 0 (avoid unnecessary power settings on rounding 0.xxx to 1)
		expect("#27.4", ess4, -1000, 0);
		expect("#27.5", ess5, -1000, 0);
		expect("#27.6", ess6, -1000, 0);

		ess6.withState(Level.OK);
		ess1.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess2.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess3.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess4.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess5.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess6.setActivePowerGreaterOrEquals(-1000); // Limit charge to maximum 1000 W
		ess1.setReactivePowerLessOrEquals(1); // Also set constraint to avoid solving using weights
		ess2.setReactivePowerLessOrEquals(1); // Also set constraint to avoid solving using weights
		ess3.setReactivePowerLessOrEquals(1); // Check that upperLimit will be respected
		ess4.setReactivePowerLessOrEquals(1); // Also set constraint to avoid solving using weights
		ess5.setReactivePowerLessOrEquals(1); // Also set constraint to avoid solving using weights
		ess6.setReactivePowerLessOrEquals(1); // Also set constraint to avoid solving using weights
		ess0.setActivePowerEqualsWithoutFilter(-6000); // Charging with 5000 W
		ess0.setReactivePowerEqualsWithoutFilter(2); // 2W reactive power request
		componentTest.next(new TestCase("#27"));

		// Test charge & reactive power request -> Test USE_IDLE_ESS WeightStrategy with asymmetric MaxApparentPower ratio
		expect("#28.1", ess1, 0, -130); // MaxApparentPower/MaxApparentPowerTotal -> 5000/40000 -> 13% of -1000W -> -130W
		expect("#28.2", ess2, 0, -130); // MaxApparentPower/MaxApparentPowerTotal -> 5000/40000 -> 13% of -1000W -> -130W
		expect("#28.3", ess3, -5000, 0); // charging inverter should not be used
		expect("#28.4", ess4, 0, -250); // MaxApparentPower/MaxApparentPowerTotal -> 10000/40000 -> 25% of -1000W -> -250W
		expect("#28.5", ess5, 0, -250); // MaxApparentPower/MaxApparentPowerTotal -> 10000/40000 -> 25% of -1000W -> -250W
		expect("#28.6", ess6, 0, -240); // MaxApparentPower/MaxApparentPowerTotal -> 10000/40000 -> 25% of -1000W -> -250W limited to maximum remaining required reactive power of -240W

		ess1.withMaxApparentPower(5000);
		ess2.withMaxApparentPower(5000);
		ess0.setActivePowerEqualsWithoutFilter(-5000); // Charging with 5000 W
		ess0.setReactivePowerEqualsWithoutFilter(-1000); // -1kW reactive power request
		componentTest.next(new TestCase("#28"));
	}

	@Test
	public void testWithPowerPrecision() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(1000) //
				.withSoc(9);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(2000) //
				.withSoc(9);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(3000) //
				.withPowerPrecision(100) //
				.withSoc(8);
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(4000) //
				.withSoc(13);
		var ess5 = new DummyManagedSymmetricEss("ess5") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(1000) //
				.withSoc(50);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4, ess5) //
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
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test discharge with PowerPrecision 100 on ess3 (instead of 1)
		expect("#1.1", ess1, 10000, 0);
		expect("#1.2", ess2, 10000, 0);
		expect("#1.3", ess3, 9900, 0);
		expect("#1.4", ess4, 10000, 0);
		expect("#1.5", ess5, 10000, 0);

		ess0.setActivePowerEqualsWithoutFilter(49996); // Discharging with 4 W less then maximum power
		componentTest.next(new TestCase("#1"));

		// Test discharge with PowerPrecision 100 on ess2 & ess3 (instead of 1)
		ess2.withPowerPrecision(100);

		expect("#2.1", ess1, 10000, 0);
		expect("#2.2", ess2, 6900, 0);
		expect("#2.3", ess3, 3000, 0);
		expect("#2.4", ess4, 10000, 0);
		expect("#2.5", ess5, 10000, 0);

		ess0.setActivePowerEqualsWithoutFilter(39996); // Discharging with 39996 W
		componentTest.next(new TestCase("#2"));

		// Test discharge and reactive power request
		expect("#3.1", ess1, 0, 0);
		expect("#3.2", ess2, 0, 0);
		expect("#3.3", ess3, 0, 0);
		expect("#3.4", ess4, 0, 0);
		expect("#3.5", ess5, 1000, 800);

		ess0.setActivePowerEqualsWithoutFilter(1000); // Discharging with 1000 W
		ess0.setReactivePowerEqualsWithoutFilter(800); // Reactive Power request of 800 W
		componentTest.next(new TestCase("#3"));

		// Test discharge and reactive power request
		expect("#4.1", ess1, 10000, 0);  // running on full active power, no reactive power possible
		expect("#4.2", ess2, 6900, 2100); // gets 6900/9900 -> 70% of 3000W -> 2100W -> with precision 100 -> 2100W reactive power
		expect("#4.3", ess3, 3000, 900); // gets 3000/9900 -> 30% of 3000W -> 900W -> with precision 100 -> 900W reactive power
		expect("#4.4", ess4, 10000, 0);
		expect("#4.5", ess5, 10000, 0);

		ess0.setActivePowerEqualsWithoutFilter(39996); // Discharging with 39996 W
		ess0.setReactivePowerEqualsWithoutFilter(3000); // Reactive Power request of 3000 W
		componentTest.next(new TestCase("#4"));

		// Test charge and reactive power request
		expect("#5.1", ess1, 0, -750); // qMax 10000 -> 10000/40000 -> 25% of -3000W -> -750W
		expect("#5.2", ess2, 0, -800); // qMax 10000 -> 10000/40000 -> 25% of -3000W -> -750W -> power precision 100 -> -800W
		expect("#5.3", ess3, -8000, 0);  // charging inverter should not be used for reactive power
		expect("#5.4", ess4, 0, -750); // qMax 10000 -> 10000/40000 -> 25% of -3000W -> -750W
		expect("#5.5", ess5, 0, -750); // qMax 10000 -> 10000/40000 -> 25% of -3000W -> -750W

		ess0.setActivePowerEqualsWithoutFilter(-8000); // Charging Charging with 8000 W
		ess0.setReactivePowerEqualsWithoutFilter(-3000); // Reactive Power request of -3000 W
		componentTest.next(new TestCase("#5"));
	}

	@Test
	public void testForceDischarge() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		// Test force discharge
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(2000) // Cannot charge (100% SOC) -> PvProduction as forced discharge
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(2000) //
				.withSoc(100);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(0) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(0) //
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
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#1.1", ess1, 2000, 0); // Pv production as forced charge
		expect("#1.2", ess2, -7000, 0); // Gets 5kw charge + Pv production of ess1 as charge

		ess0.setActivePowerEqualsWithoutFilter(-5000); // Request 5kw charge
		componentTest.next(new TestCase("#1"));
	}

	@Test
	public void testForceCharge() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		// Test force charge
		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(0) //
				.withSoc(100);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(-100) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(0) //
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
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#1.1", ess1, 5100, 0);
		expect("#1.2", ess2, -100, 0);

		ess2.setActivePowerLessOrEquals(-100); // 100W forced charge on ess2 (e.g. due to ctrlLimitTotalDischarge force_charge_soc state)
		ess0.setActivePowerEqualsWithoutFilter(5000); // Request 5kw discharge
		componentTest.next(new TestCase("#1"));
	}

	@Test
	public void testFaultConditions() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(MAX_POWER) //
				.withMaxApparentPower(MAX_POWER) //
				.withSoc(20);
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
				.withSoc(30);
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
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		expect("#1.1", ess1, 0, 0);
		expect("#1.2", ess2, 0, 0);
		expect("#1.3", ess3, 0, 0);
		expect("#1.4", ess4, 0, 0);
		expect("#1.5", ess5, 0, 0);
		expect("#1.6", ess6, 0, 0);
		expect("#1.7", ess7, -8000, 0);
		expect("#1.8", ess8, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(-8000);
		componentTest.next(new TestCase("#1"));

		ess7.withState(Level.FAULT);
		expect("#2.1", ess1, 0, 0);
		expect("#2.2", ess2, 0, 0);
		expect("#2.3", ess3, -8000, 0);
		expect("#2.4", ess4, 0, 0);
		expect("#2.5", ess5, 0, 0);
		expect("#2.6", ess6, 0, 0);
		expect("#2.7", ess7, 0, 0);
		expect("#2.8", ess8, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(-8000);
		componentTest.next(new TestCase("#2"));

		ess3.withState(Level.FAULT);
		expect("#3.1", ess1, 0, 0);
		expect("#3.2", ess2, -8000, 0);
		expect("#3.3", ess3, 0, 0);
		expect("#3.4", ess4, 0, 0);
		expect("#3.5", ess5, 0, 0);
		expect("#3.6", ess6, 0, 0);
		expect("#3.7", ess7, 0, 0);
		expect("#3.8", ess8, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(-8000);
		componentTest.next(new TestCase("#3"));
	}

	@Test
	public void testExtremePowerLimits() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-10000) // Very limited charge power
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withPvProduction(0) //
				.withSoc(30);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(5000) // Very limited discharge power
				.withMaxApparentPower(50000) //
				.withPvProduction(0) //
				.withSoc(45);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withPvProduction(0) //
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
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test discharge with limited power on ess2
		expect("#1.1", ess1, 10000, 0); // Gets 5kW due to minimum constraint + remaining power due to lower SOC
		expect("#1.2", ess2, 0, 0); // Discharge of ess3 is not required
		expect("#1.3", ess3, 50000, 0); // Gets 5kW due to minimum constraint + max discharge power due to higher SOC

		ess0.setActivePowerEqualsWithoutFilter(60000); // 60kW discharge
		componentTest.next(new TestCase("#1"));

		// Test charge with limited power on ess1
		expect("#2.1", ess1, -10000, 0); // Low SOC ESS limited by max charge power
		expect("#2.2", ess2, -50000, 0); // Gets more charge due to ess1 limitation
		expect("#2.3", ess3, 0, 0); // Gets no power due to higher SOC

		ess0.setActivePowerEqualsWithoutFilter(-60000); // 60kW charge
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
				.withSoc(60);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(40);

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
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test zero power request
		expect("#1.1", ess1, 0, 0);
		expect("#1.2", ess2, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero power
		componentTest.next(new TestCase("#1"));

		// Test zero active power & positive reactive power request
		expect("#2.1", ess1, 0, 500);
		expect("#2.2", ess2, 0, 500);

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero active power
		ess0.setReactivePowerEqualsWithoutFilter(1000); // 1000W reactive power
		componentTest.next(new TestCase("#2"));

		// Test zero active power & negative reactive power request
		expect("#3.1", ess1, 0, -500);
		expect("#3.2", ess2, 0, -500);

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero active power
		ess0.setReactivePowerEqualsWithoutFilter(-1000); // -1000W reactive power
		componentTest.next(new TestCase("#3"));

		// Test very small power request
		expect("#4.1", ess1, 20, 0);
		expect("#4.2", ess2, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(20); // 20W discharge
		componentTest.next(new TestCase("#4"));

		// Test very small power & positive reactive power request
		expect("#5.1", ess1, 20, 1000); // feed-in inverters are preferred
		expect("#5.2", ess2, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(20); // 20W discharge
		ess0.setReactivePowerEqualsWithoutFilter(1000); // 1000W reactive power
		componentTest.next(new TestCase("#5"));

		// Test very small power & negative reactive power request
		expect("#6.1", ess1, 20, -1000); // feed-in inverters are preferred
		expect("#6.2", ess2, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(20); // 20W discharge
		ess0.setReactivePowerEqualsWithoutFilter(-1000); // 1000W reactive power
		componentTest.next(new TestCase("#6"));

		// Test negative very small power request
		expect("#7.1", ess1, 0, 0);
		expect("#7.2", ess2, -2, 0);

		ess0.setActivePowerEqualsWithoutFilter(-2); // 2W charge
		componentTest.next(new TestCase("#7"));

		// Test negative very small power request & positive reactive power request
		expect("#8.1", ess1, 0, 1000);
		expect("#8.2", ess2, -2, 0);

		ess0.setActivePowerEqualsWithoutFilter(-2); // 2W charge
		ess0.setReactivePowerEqualsWithoutFilter(1000); // 1000W reactive power
		componentTest.next(new TestCase("#8"));

		// Test negative very small power request & negative reactive power request
		expect("#9.1", ess1, 0, -1000);
		expect("#9.2", ess2, -2, 0);

		ess0.setActivePowerEqualsWithoutFilter(-2); // 2W charge
		ess0.setReactivePowerEqualsWithoutFilter(-1000); // -1000W reactive power
		componentTest.next(new TestCase("#9"));

		// Test zero power request with pv production on ess1
		ess1.withPvProduction(1000);

		expect("#10.1", ess1, 0, 0); // Zero Setpoint (AC) -> 1000W charge (DC)
		expect("#10.2", ess2, 0, 0);

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero power
		componentTest.next(new TestCase("#10"));

		// Test zero power request with pv production on ess1 & positive reactive power request
		ess1.withPvProduction(1000);

		expect("#11.1", ess1, 0, 1250); // Zero Setpoint (AC) -> 1000W charge (DC)
		expect("#11.2", ess2, 0, 1250);

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero power
		ess0.setReactivePowerEqualsWithoutFilter(2500); // 2500W reactive power
		componentTest.next(new TestCase("#11"));

		// Test zero power request with pv production on ess1 & negative reactive power request
		ess1.withPvProduction(1000);

		expect("#12.1", ess1, 0, -1250); // Zero Setpoint (AC) -> 1000W charge (DC)
		expect("#12.2", ess2, 0, -1250);

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero power
		ess0.setReactivePowerEqualsWithoutFilter(-2500); // -2500W reactive power
		componentTest.next(new TestCase("#12"));

		// Test zero power request with pv production on ess1 after battery gets full (100% SOC)
		ess1.withSoc(100);
		ess1.withAllowedChargePower(1000); // battery full -> force discharge active with 1000W pv production

		expect("#13.1", ess1, 1000, 0); // Setpoint (AC) of 1000W (force discharge active as battery full)
		expect("#13.2", ess2, -1000, 0); // AC Charge of 1000W (to keep ess0 on zero)

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero power
		componentTest.next(new TestCase("#13"));

		// Test zero power request with pv production on ess1 after battery gets full (100% SOC) & positive reactive power
		ess1.withSoc(100);
		ess1.withAllowedChargePower(1000); // battery full -> force discharge active with 1000W pv production

		expect("#14.1", ess1, 1000, 3000); // Setpoint (AC) of 1000W (force discharge active as battery full)
		expect("#14.2", ess2, -1000, 0); // AC Charge of 1000W (to keep ess0 on zero)

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero power
		ess0.setReactivePowerEqualsWithoutFilter(3000); // 3000W reactive power
		componentTest.next(new TestCase("#14"));

		// Test zero power request with pv production on ess1 after battery gets full (100% SOC) & negative reactive power
		ess1.withSoc(100);
		ess1.withAllowedChargePower(1000); // battery full -> force discharge active with 1000W pv production

		expect("#15.1", ess1, 1000, -3000); // Setpoint (AC) of 1000W (force discharge active as battery full)
		expect("#15.2", ess2, -1000, 0); // AC Charge of 1000W (to keep ess0 on zero)

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero power
		ess0.setReactivePowerEqualsWithoutFilter(-3000); // -3000W reactive power
		componentTest.next(new TestCase("#15"));

		// Test zero power request with pv production on ess1 after ess2 battery gets also full (100% SOC)
		//   -> Zero Power not possible. Power class will increase ess0 minimum power from 0W to 1000W
		ess2.withSoc(100);
		ess2.withAllowedChargePower(0); // battery full -> ess2 can not charge anymore

		assertEquals("#16.0", 1000, ess0.getPower().getMinPower(ess0, ALL, ACTIVE)); // minimum power gets increased from 0W to 1000W
		expect("#16.1", ess1, 1000, 0); // Setpoint (AC) of 1000W (force discharge active as battery full)
		expect("#16.2", ess2, 0, 0); // AC Charge not possible due to full battery

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero power
		componentTest.next(new TestCase("#16"));

		// Test zero power request with pv production on ess1 after ess2 battery gets also full (100% SOC) & positive reactive power
		//   -> Zero Power not possible. Power class will increase ess0 minimum power from 0W to 1000W
		ess2.withSoc(100);
		ess2.withAllowedChargePower(0); // battery full -> ess2 can not charge anymore

		assertEquals("#17.0", 1000, ess0.getPower().getMinPower(ess0, ALL, ACTIVE)); // minimum power gets increased from 0W to 1000W
		expect("#17.1", ess1, 1000, 8000); // Setpoint (AC) of 1000W (force discharge active as battery full)
		expect("#17.2", ess2, 0, 0); // AC Charge not possible due to full battery

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero power
		ess0.setReactivePowerEqualsWithoutFilter(8000); // 8000W reactive power
		componentTest.next(new TestCase("#17"));

		// Test zero power request with pv production on ess1 after ess2 battery gets also full (100% SOC) & negative reactive power
		//   -> Zero Power not possible. Power class will increase ess0 minimum power from 0W to 1000W
		ess2.withSoc(100);
		ess2.withAllowedChargePower(0); // battery full -> ess2 can not charge anymore

		assertEquals("#18.0", 1000, ess0.getPower().getMinPower(ess0, ALL, ACTIVE)); // minimum power gets increased from 0W to 1000W
		expect("#18.1", ess1, 1000, -8000); // Setpoint (AC) of 1000W (force discharge active as battery full)
		expect("#18.2", ess2, 0, 0); // AC Charge not possible due to full battery

		ess0.setActivePowerEqualsWithoutFilter(0); // Zero power
		ess0.setReactivePowerEqualsWithoutFilter(-8000); // -8000W reactive power
		componentTest.next(new TestCase("#18"));
	}

	@Test
	public void testEssConstraints() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-20000) //
				.withAllowedDischargePower(60000) //
				.withMaxApparentPower(60000) //
				.withPvProduction(1000) //
				.withSoc(30);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-60000) //
				.withAllowedDischargePower(20000) //
				.withMaxApparentPower(60000) //
				.withPvProduction(1000) //
				.withSoc(70);
		var ess3 = new DummyManagedSymmetricEss("ess3") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-40000) //
				.withAllowedDischargePower(40000) //
				.withMaxApparentPower(40000) //
				.withPvProduction(0) //
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
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test discharge
		expect("#1.1", ess1, 1000, 0); // gets 1kW due to pv production
		expect("#1.2", ess2, 1000, 0); // gets 1kW due to pv production
		expect("#1.3", ess3, 1000, 0); // fulfill minimum constraint

		ess3.setActivePowerGreaterOrEquals(1000); // minimum discharge of 1kW required
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		componentTest.next(new TestCase("#1"));

		// Test discharge & minimum constraint on ess with pv production
		expect("#2.1", ess1, 1500, 0); // gets 1.5kW due to minimum constraint
		expect("#2.2", ess2, 1000, 0); // gets remaining 1kW due to pv production
		expect("#2.3", ess3, 0, 0);

		ess1.setActivePowerGreaterOrEquals(1500); // minimum discharge of 1.5kW required
		ess0.setActivePowerEqualsWithoutFilter(2500); // 2.5kW discharge
		componentTest.next(new TestCase("#2"));

		// Test discharge & minimum constraint on ess with pv production
		expect("#3.1", ess1, 1500, 0); // gets 1.5kW due to minimum constraint
		expect("#3.2", ess2, 1500, 0); // gets 1kW due to pv production  + remaining 500W due to highest SOC
		expect("#3.3", ess3, 0, 0);

		ess1.setActivePowerGreaterOrEquals(1500); // minimum discharge of 1.5kW required
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		componentTest.next(new TestCase("#3"));

		// Test discharge & minimum constraint on ess with pv production
		expect("#4.1", ess1, 1500, 0); // gets 1.5kW due to minimum constraint
		expect("#4.2", ess2, 500, 0); // gets remaining 500 W due to pv production
		expect("#4.3", ess3, 0, 0);

		ess1.setActivePowerGreaterOrEquals(1500); // minimum discharge of 1.5kW required
		ess0.setActivePowerEqualsWithoutFilter(2000); // 2kW discharge
		componentTest.next(new TestCase("#4"));

		// Test discharge & maximum constraint on ess with pv production
		expect("#5.1", ess1, 500, 0); // gets only 0.5kW of 1kW pv production due to maximum constraint
		expect("#5.2", ess2, 1500, 0); // gets 1kW due to pv production + remaining 0.5kW due to already discharging
		expect("#5.3", ess3, 0, 0);

		ess1.setActivePowerLessOrEquals(500); // maximum discharge of 0.5kW allowed
		ess0.setActivePowerEqualsWithoutFilter(2000); // 2kW discharge
		componentTest.next(new TestCase("#5"));

		// Test discharge & minimum constraint on ess without pv production
		expect("#6.1", ess1, 1000, 0); // gets 1kW due to pv production
		expect("#6.2", ess2, 2000, 0); // gets 1kW due to pv production + remaining 1kW due to highest soc
		expect("#6.3", ess3, 1000, 0); // fulfill minimum constraint

		ess3.setActivePowerGreaterOrEquals(1000); // minimum discharge of 1kW required
		ess0.setActivePowerEqualsWithoutFilter(4000); // 4kW discharge
		componentTest.next(new TestCase("#6"));

		// Test discharge & minimum charge constraint on ess3
		expect("#7.1", ess1, 1000, 0); // gets 1kW due to pv production
		expect("#7.2", ess2, 3000, 0); // gets 1kW due to pv production + remaining 2kW due to higher SOC
		expect("#7.3", ess3, -1000, 0); // fulfill maximum constraint (other ess have to compensate this with +1000W)

		ess3.setActivePowerLessOrEquals(-1000); // minimum charge of 1kW required -> maximum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		componentTest.next(new TestCase("#7"));

		// Test discharge & positive reactive power request & positive reactive ess constraint on ess not planned to deliver reactive power
		expect("#8.1", ess1, 1000, 330); // 1000/3000 -> 33% of 2000-1000 (1000) W -> 330 W
		expect("#8.2", ess2, 2000, 670); // 2000/3000 -> 67% of 2000-1000 (1000) W -> 670 W
		expect("#8.3", ess3, 0, 1000); // fulfill minimum reactive power constraint

		ess3.setReactivePowerGreaterOrEquals(1000); // minimum reactive power of 1kW required
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#8"));

		// Test discharge & positive reactive power request & positive reactive ess constraint on ess not planned to deliver reactive power
		expect("#9.1", ess1, 1000, 500); // 1000/3000 -> 33% of 2000-1000 (1000) W -> 330 W -> increased with 170W due to max max constraint of ess2
		expect("#9.2", ess2, 2000, 500); // 2000/3000 -> 67% of 2000-1000 (1000) W -> 670 W -> limited to 500W due to max constraint
		expect("#9.3", ess3, 0, 1000); // fulfill minimum reactive power constraint

		ess2.setReactivePowerLessOrEquals(500); // maximum reactive power of 0.5kW allowed
		ess3.setReactivePowerGreaterOrEquals(1000); // minimum reactive power of 1kW required
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#9"));

		// Test discharge & positive reactive power request & positive reactive ess constraint on ess planned to deliver reactive power
		expect("#10.1", ess1, 1000, 500); // 1000/3000 -> 33% of 2000 W -> 660 W -> decreased to remaining power required of 500W
		expect("#10.2", ess2, 2000, 1500); // 2000/3000 -> 67% of 2000 W -> 1340 W -> increased to minimum of 1500W
		expect("#10.3", ess3, 0, 0);

		ess2.setReactivePowerGreaterOrEquals(1500); // minimum reactive power of 1.5kW required
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#10"));

		// Test discharge & positive reactive power request & positive reactive ess constraint on ess planned to deliver reactive power
		expect("#11.1", ess1, 1000, 500); // 1000/3000 -> 33% of 2000-1000 (1000) W -> 330 W -> increased with 170W due to max max constraint of ess2
		expect("#11.2", ess2, 2000, 500); // 2000/3000 -> 67% of 2000-1000 (1000) W -> 670 W -> limited to 500W due to max constraint
		expect("#11.3", ess3, 0, 0); // fulfill minimum reactive power constraint

		ess2.setReactivePowerLessOrEquals(500); // maximum reactive power of 0.5kW allowed
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(1000); // 1kW reactive power
		componentTest.next(new TestCase("#11"));

		// Test discharge & positive reactive power request & negative reactive ess constraint on ess not planned to deliver reactive power
		expect("#12.1", ess1, 1000, 990); // 1000/3000 -> 33% of 2000+1000 (3000) W -> 990 W
		expect("#12.2", ess2, 2000, 2010); // 2000/3000 -> 67% of 2000+1000 (3000) W -> 2010 W
		expect("#12.3", ess3, 0, -1000); // fulfill minimum reactive power constraint (other ess have to compensate this with +1000W)

		ess3.setReactivePowerLessOrEquals(-1000); // minimum reactive power of -1kW required -> maximum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#12"));

		// Test discharge & positive reactive power request & negative reactive ess constraint on ess planned to deliver reactive power
		expect("#13.1", ess1, 1000, -1000); // fulfill minimum reactive power constraint
		expect("#13.2", ess2, 2000, 3000); // gets all reactive power + additional 1000W to compensate constraint of ess1
		expect("#13.3", ess3, 0, 0); // idle ess should not be used for reactive power

		ess1.setReactivePowerLessOrEquals(-1000); // minimum reactive power of -1kW required -> maximum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#13"));

		// Test discharge & negative reactive power request & negative reactive ess constraint on ess planned to deliver reactive power
		expect("#14.1", ess1, 1000, -660); // 1000/3000 -> 33% of -2000W -> -660W
		expect("#14.2", ess2, 2000, -1340); // 2000/3000 -> 67% of -2000W -> -1340W
		expect("#14.3", ess3, 0, 0); // fulfill maximum reactive power constraint

		ess2.setReactivePowerLessOrEquals(-1000); // minimum reactive power of -1kW required -> maximum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // -2kW reactive power
		componentTest.next(new TestCase("#14"));

		// Test discharge & negative reactive power request & negative reactive ess constraint on ess planned to deliver reactive power
		expect("#15.1", ess1, 1000, -500); // 1000/3000 -> 33% of -2000W -> -660W -> decrease to remaining reactive power -500.0
		expect("#15.2", ess2, 2000, -1500); // 2000/3000 -> 67% of -2000W -> -1340W -> increase to maximum constraint -1500.0
		expect("#15.3", ess3, 0, 0); // fulfill maximum reactive power constraint

		ess2.setReactivePowerLessOrEquals(-1500); // minimum reactive power of -1.5kW required -> maximum constraint -1500.0
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // -2kW reactive power
		componentTest.next(new TestCase("#15"));

		// Test discharge & negative reactive power request & negative reactive ess constraint on ess planned to deliver reactive power
		expect("#16.1", ess1, 1000, -1000); // 1000/3000 -> 33% of -2000W -> -660W -> increase to remaining reactive power -1000.0
		expect("#16.2", ess2, 2000, -1000); // 2000/3000 -> 67% of -2000W -> -1340W -> decrease to minimum constraint -1000.0
		expect("#16.3", ess3, 0, 0); // fulfill maximum reactive power constraint

		ess2.setReactivePowerGreaterOrEquals(-1000); // maximum reactive power of -1kW allowed -> minimum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // -2kW reactive power
		componentTest.next(new TestCase("#16"));

		// Test discharge & negative reactive power request & negative reactive ess constraint on ess not planned to deliver reactive power
		expect("#17.1", ess1, 1000, -330); // 1000/3000 -> 33% of -1000 W -> -330 W
		expect("#17.2", ess2, 2000, -670); // 2000/3000 -> 67% of -1000 W -> -670 W
		expect("#17.3", ess3, 0, -1000); // fulfill maximum reactive power constraint

		ess3.setReactivePowerLessOrEquals(-1000); // minimum reactive power of -1kW required -> maximum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // -2kW reactive power
		componentTest.next(new TestCase("#17"));

		// Test discharge & negative reactive power request & positive reactive ess constraint on ess planned to deliver reactive power
		expect("#18.1", ess1, 1000, -3500); // gets all reactive power + additional -1500W to compensate constraint of ess2
		expect("#18.2", ess2, 2000, 1500); // fulfill minimum reactive power constraint
		expect("#18.3", ess3, 0, 0); // idle ess should not be used for reactive power

		ess2.setReactivePowerGreaterOrEquals(1500); // minimum reactive power of 1.5kW required -> minimum constraint 1500.0
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // -2kW reactive power
		componentTest.next(new TestCase("#18"));

		// Test discharge & negative reactive power request & positive reactive ess constraint on ess not planned to deliver reactive power
		expect("#19.1", ess1, 1000, -990); // 1000/3000 -> 33% of -2000-1000 (-3000) W -> -990 W
		expect("#19.2", ess2, 2000, -2010); // 2000/3000 -> 67% of -2000-1000 (-3000) W -> -2010 W
		expect("#19.3", ess3, 0, 1000); // fulfill minimum reactive power constraint (other ess have to compensate this with -1000W)

		ess3.setReactivePowerGreaterOrEquals(1000); // minimum reactive power of 1kW required -> minimum constraint 1000.0
		ess0.setActivePowerEqualsWithoutFilter(3000); // 3kW discharge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // -2kW reactive power
		componentTest.next(new TestCase("#19"));

		// Test charge & minimum charge constraint
		expect("#20.1", ess1, -2000, 0); // gets remaining charge power due to lowest SOC
		expect("#20.2", ess2, 0, 0);
		expect("#20.3", ess3, -1000, 0); // fulfill maximum constraint

		ess3.setActivePowerLessOrEquals(-1000); // minimum charge of 1kW required -> maximum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		componentTest.next(new TestCase("#20"));

		// Test charge & maximum charge constraint
		expect("#21.1", ess1, -1000, 0); // gets charge power due to lowest SOC limited by maximum constraint
		expect("#21.2", ess2, 0, 0);
		expect("#21.3", ess3, -2000, 0); // gets remaining charge power due to lower SOC

		ess1.setActivePowerGreaterOrEquals(-1000); // maximum charge of 1kW allowed -> minimum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		componentTest.next(new TestCase("#21"));

		// Test charge & minimum discharge constraint
		expect("#22.1", ess1, -4000, 0); // gets all charge power due to lowest SOC + 1kW compensate for ess2 discharge
		expect("#22.2", ess2, 1000, 0); // force discharge
		expect("#22.3", ess3, 0, 0);

		ess2.setActivePowerGreaterOrEquals(1000); // minimum discharge of 1kW required -> minimum constraint 1000.0
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		componentTest.next(new TestCase("#22"));

		// Test charge & negative reactive power request & negative reactive ess constraint on ess planned to deliver reactive power
		expect("#23.1", ess1, -3000, 0);
		expect("#23.2", ess2, 0, -1000); // gets remaining charge power due to idle state
		expect("#23.3", ess3, 0, -1000); // fulfill maximum reactive power constraint -1000.0

		ess3.setReactivePowerLessOrEquals(-1000); // minimum reactive power of -1kW required -> maximum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // -2kW reactive power
		componentTest.next(new TestCase("#23"));

		// Test charge & negative reactive power request & negative reactive ess constraint on ess planned to deliver reactive power
		expect("#24.1", ess1, -3000, 0);
		expect("#24.2", ess2, 0, -1000); // MaxApparentPower/MaxApparentPowerTotal -> 60000/100000 -> 60% of -2000W -> -1200W limited to max reactive power of -1000W
		expect("#24.3", ess3, 0, -1000); // MaxApparentPower/MaxApparentPowerTotal -> 40000/100000 -> 40% of -2000W -> -800W increased to remaining reactive power of -1000W

		ess3.setReactivePowerGreaterOrEquals(-1000); // maximum reactive power of -1kW allowed -> minimum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // -2kW reactive power
		componentTest.next(new TestCase("#24"));

		// Test charge & negative reactive power request & negative reactive ess constraint on ess not planned to deliver reactive power
		expect("#25.1", ess1, -3000, -1000); // fulfill maximum reactive power constraint -1000.0
		expect("#25.2", ess2, 0, -600); // MaxApparentPower/MaxApparentPowerTotal -> 60000/100000 -> 60% of -1000W -> -600W
		expect("#25.3", ess3, 0, -400); // MaxApparentPower/MaxApparentPowerTotal -> 40000/100000 -> 40% of -1000W -> -400W

		ess1.setReactivePowerLessOrEquals(-1000); // minimum reactive power of -1kW required -> maximum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		ess0.setReactivePowerEqualsWithoutFilter(-2000); // -2kW reactive power
		componentTest.next(new TestCase("#25"));

		// Test charge & positive reactive power request & positive reactive ess constraint on ess planned to deliver reactive power
		expect("#26.1", ess1, -3000, 0); // charging ess should not be used for reactive power
		expect("#26.2", ess2, 0, 1200); // MaxApparentPower/MaxApparentPowerTotal -> 60000/100000 -> 60% of 2000W -> 1200W (above minimum)
		expect("#26.3", ess3, 0, 800); // MaxApparentPower/MaxApparentPowerTotal -> 40000/100000 -> 40% of 2000W -> 800W

		ess2.setReactivePowerGreaterOrEquals(1000); // minimum reactive power of 1kW required
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#8"));

		// Test charge & positive reactive power request & positive reactive ess constraint on ess planned to deliver reactive power
		expect("#27.1", ess1, -3000, 0); // charging ess should not be used for reactive power
		expect("#27.2", ess2, 0, 1000); // MaxApparentPower/MaxApparentPowerTotal -> 60000/100000 -> 60% of 2000W -> 1200W limited to maximum of 1000W
		expect("#27.3", ess3, 0, 1000); // MaxApparentPower/MaxApparentPowerTotal -> 40000/100000 -> 40% of 2000W -> 800W increased to remaining required reactive power of 1000W

		ess2.setReactivePowerLessOrEquals(1000); // maximum reactive power of 1kW allowed
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#27"));

		// Test charge & positive reactive power request & positive reactive ess constraint on ess planned to deliver reactive power
		expect("#28.1", ess1, -3000, 0); // charging ess should not be used for reactive power
		expect("#28.2", ess2, 0, 1500); // fulfill minimum reactive power constraint
		expect("#28.3", ess3, 0, 500); // gets remaining reactive power

		ess2.setReactivePowerGreaterOrEquals(1500); // minimum reactive power of 1.5kW required
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#28"));

		// Test charge & positive reactive power request & positive reactive ess constraint on ess not planned to deliver reactive power
		expect("#29.1", ess1, -3000, 1000); // fulfill minimum reactive power constraint
		expect("#29.2", ess2, 0, 600); // MaxApparentPower/MaxApparentPowerTotal -> 60000/100000 -> 60% of 1000W -> 600W
		expect("#29.3", ess3, 0, 400); // MaxApparentPower/MaxApparentPowerTotal -> 40000/100000 -> 40% of 1000W -> 400W

		ess1.setReactivePowerGreaterOrEquals(1000); // minimum reactive power of 1kW required
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#29"));

		// Test charge & positive reactive power request & negative reactive ess constraint on ess planned to deliver reactive power
		expect("#30.1", ess1, -3000, 0); // charging ess should not be used for reactive power
		expect("#30.2", ess2, 0, -1500); // fulfill maximum reactive power constraint
		expect("#30.3", ess3, 0, 3500); // gets all reactive power + additional 1500W to compensate constraint of ess2

		ess2.setReactivePowerLessOrEquals(-1500); // minimum reactive power of -1.5kW required -> maximum constraint -1500.0
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#30"));

		// Test charge & positive reactive power request & negative reactive ess constraint on ess not planned to deliver reactive power
		expect("#31.1", ess1, -3000, -1000); // fulfill maximum reactive power constraint (other ess have to compensate this with +1000W)
		expect("#31.2", ess2, 0, 1800); // MaxApparentPower/MaxApparentPowerTotal -> 60000/100000 -> 60% of 2000+1000 (3000) W -> 1800W
		expect("#31.3", ess3, 0, 1200); // MaxApparentPower/MaxApparentPowerTotal -> 40000/100000 -> 40% of 1000+1000 (3000) W -> 1200W

		ess1.setReactivePowerLessOrEquals(-1000); // minimum reactive power of -1kW required -> maximum constraint -1000.0
		ess0.setActivePowerEqualsWithoutFilter(-3000); // 3kW charge
		ess0.setReactivePowerEqualsWithoutFilter(2000); // 2kW reactive power
		componentTest.next(new TestCase("#31"));
	}

	@Test
	public void testSymmetricPowerLimits() throws Exception {
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
		var ess4 = new DummyManagedSymmetricEss("ess4") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000) //
				.withSoc(80);

		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4) //
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
				.activate(MyConfig.create() //
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test discharge with max discharge power
		expect("#1.1", ess1, 60000, 0);
		expect("#1.2", ess2, 20000, 0);
		expect("#1.3", ess3, 40000, 0);
		expect("#1.4", ess4, 10000, 0);

		ess0.setActivePowerEqualsWithoutFilter(130000); // 130kw discharge
		componentTest.next(new TestCase("#1"));

		// Test discharge with more than maximum power
		expect("#2.1", ess1, 60000, 0);
		expect("#2.2", ess2, 20000, 0);
		expect("#2.3", ess3, 40000, 0);
		expect("#2.4", ess4, 10000, 0);

		ess0.setActivePowerEqualsWithoutFilter(150000); // 150kw discharge
		componentTest.next(new TestCase("#2"));

		// Test charge with maximum charger power
		expect("#3.1", ess1, -20000, 0);
		expect("#3.2", ess2, -60000, 0);
		expect("#3.3", ess3, -40000, 0);
		expect("#3.4", ess4, -10000, 0);

		ess0.setActivePowerEqualsWithoutFilter(-130000); // 130kw charge
		componentTest.next(new TestCase("#3"));

		// Test charge with more than maximum power
		expect("#4.1", ess1, -20000, 0);
		expect("#4.2", ess2, -60000, 0);
		expect("#4.3", ess3, -40000, 0);
		expect("#4.4", ess4, -10000, 0);

		ess0.setActivePowerEqualsWithoutFilter(-150000); // 150kw charge
		componentTest.next(new TestCase("#4"));
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
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test single ESS gets all requested power
		expect("#1.1", ess1, 30000, 0);

		ess0.setActivePowerEqualsWithoutFilter(30000); // 30kW discharge
		componentTest.next(new TestCase("#1"));

		// Test single ESS charge
		expect("#2.1", ess1, -30000, 0);

		ess0.setActivePowerEqualsWithoutFilter(-30000); // 30kW charge
		componentTest.next(new TestCase("#2"));

		// Test power beyond limits
		expect("#3.1", ess1, 50000, 0); // Limited to max discharge

		ess0.setActivePowerEqualsWithoutFilter(80000);
		componentTest.next(new TestCase("#3"));

		// Test power beyond limits
		expect("#4.1", ess1, -50000, 0); // Limited to max charge

		ess0.setActivePowerEqualsWithoutFilter(-80000);
		componentTest.next(new TestCase("#4"));
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
				.withPvProduction(0) //
				.withSoc(100);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-MAX_POWER) //
				.withAllowedDischargePower(0) //
				.withMaxApparentPower(MAX_POWER) //
				.withPvProduction(0) //
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
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test #1: Discharge scenario
		expect("#1.1", ess1, 0, 0);
		expect("#1.2", ess2, -MAX_POWER, 0);

		ess0.setActivePowerEqualsWithoutFilter(-MAX_POWER); // Request -MAX POWER as discharge
		componentTest.next(new TestCase("#1"));

		// Test #2: Charge scenario
		expect("#2.1", ess1, 0, 0); // Cannot charge, so gets 0
		expect("#2.2", ess2, -MAX_POWER, 0); // Should be limited to its max

		ess0.setActivePowerEqualsWithoutFilter(-100000); // Request 100kW charge (more than ess2 can handle)
		componentTest.next(new TestCase("#2"));

		// Test #3: After SOC changes - should still respect power limits
		ess1.withSoc(90);
		ess1.withAllowedChargePower(-MAX_POWER);

		expect("#3.1", ess1, -5000, 0); // Limited charge power due to high SOC
		expect("#3.2", ess2, -10000, 0); // Gets most of the charge power

		ess0.setActivePowerEqualsWithoutFilter(-15000); // Request 15kW charge
		componentTest.next(new TestCase("#3"));
	}

	@Test
	public void testReactivePowerOnly() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(60);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(40);

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
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test zero
		expect("#1.1", ess1, 0, 0);
		expect("#1.2", ess2, 0, 0);

		ess0.setReactivePowerEqualsWithoutFilter(0); // Zero power
		componentTest.next(new TestCase("#1"));

		// Test positive reactive power request
		expect("#2.1", ess1, 0, 500);
		expect("#2.2", ess2, 0, 500);

		ess0.setReactivePowerEqualsWithoutFilter(1000); // 1000W reactive power
		componentTest.next(new TestCase("#2"));

		// Test negative reactive power request
		expect("#3.1", ess1, 0, -500);
		expect("#3.2", ess2, 0, -500);

		ess0.setReactivePowerEqualsWithoutFilter(-1000); // -1000W reactive power
		componentTest.next(new TestCase("#3"));
	}

	@Test
	public void testAllNaN() throws Exception {
		EssPower powerComponent = new EssPowerImpl();

		var ess1 = new DummyManagedSymmetricEss("ess1") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(60);
		var ess2 = new DummyManagedSymmetricEss("ess2") //
				.setPower(powerComponent) //
				.withAllowedChargePower(-50000) //
				.withAllowedDischargePower(50000) //
				.withMaxApparentPower(50000) //
				.withSoc(40);

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
						.setStrategy(OPTIMIZE_BY_PREFERRING_DC_POWER) //
						.setSymmetricMode(true) //
						.setDebugMode(false) //
						.setEnablePid(false) //
						.build()); //

		// Test zero
		expect("#1.1", ess1, 0, 0);
		expect("#1.2", ess2, 0, 0);

		componentTest.next(new TestCase("#1"));
	}

	private static void expect(String description, DummyManagedSymmetricEss ess, int p, int q) {
		openCallbacks.incrementAndGet();
		ess.withSymmetricApplyPowerCallback(record -> {
			openCallbacks.decrementAndGet();
			assertEquals(description + " for " + ess.id(), p, record.activePower());
			assertEquals(description + " for " + ess.id(), q, record.reactivePower());
		});
	}

}