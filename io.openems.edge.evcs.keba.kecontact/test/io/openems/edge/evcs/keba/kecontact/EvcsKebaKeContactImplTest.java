package io.openems.edge.evcs.keba.kecontact;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.keba.kecontact.core.EvcsKebaKeContactCoreImpl;
import io.openems.edge.evcs.test.DummyEvcsPower;

public class EvcsKebaKeContactImplTest {

	private static final String COMPONENT_ID = "evcs0";
	private EvcsKebaKeContactImpl evcs;

	@Before
	public void setUp() throws Exception {
		this.evcs = new EvcsKebaKeContactImpl();
		new ComponentTest(this.evcs) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("kebaKeContactCore", new EvcsKebaKeContactCoreImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setDebugMode(false) //
						.setIp("172.0.0.1") //
						.setMinHwCurrent(6000) //
						.setUseDisplay(false) //
						.setphaseSwitchActive(true) //
						.build()); //

		assertEquals("Phase should be initially set to ONE_PHASE", Phases.ONE_PHASE, this.evcs.getPhases());

	}

	@Test
	public void testAdjustPowerLimitsOnPhaseSwitching() {
		System.out.println("Starting testAdjustPowerLimitsOnPhaseSwitching");

		// Initialize the EVCS with one phase and a specific charging power
		System.out.println("Setting phases to ONE_PHASE and charging power to 3680W");
		this.evcs._setPhases(Phases.ONE_PHASE);
		this.evcs._setChargePower(3680); // EVCS charging 3680W on one phase

		// Verify initial min/max power based on one phase
		System.out.println("Verifying initial min/max power");
		assertEquals("Initial Min Power", (Integer) 1380, this.evcs.getMinimumHardwarePower().get());
		assertEquals("Initial Max Power", (Integer) 3680, this.evcs.getMaximumHardwarePower().get());

		// Simulate phase switching to three phases
		System.out.println("Simulating phase switch to THREE_PHASE");
		this.evcs._setPhases(Phases.THREE_PHASE);

		// Expected min/max range after phase switching
		Integer expectedMin = 1380; // The real minimum power for one to three phases
		Integer expectedMax = 11040; // The real maximum power with phase switching

		// Validate the adjusted min/max power limits after phase switching
		System.out.println("Validating adjusted min/max power limits");
		assertEquals("Adjusted Min Power should be " + expectedMin, expectedMin,
				this.evcs.getMinimumHardwarePower().get());
		assertEquals("Adjusted Max Power should be " + expectedMax, expectedMax,
				this.evcs.getMaximumHardwarePower().get());
	}

	@Test
	public void testHandlingIncreasedPowerDemandWithPhaseSwitching() throws OpenemsException {
		System.out.println("Starting testHandlingIncreasedPowerDemandWithPhaseSwitching");

		// Initialize the EVCS with one phase and a specific charging power
		System.out.println("Setting phases to ONE_PHASE and charging power to 3680W");
		this.evcs._setPhases(Phases.ONE_PHASE);
		this.evcs._setChargePower(3680); // Start with 3680W on one phase

		// Increase target power demand to 5000W, should trigger phase switching logic
		System.out.println("Applying charge power limit of 5000W, expecting phase switch");
		this.evcs.applyChargePowerLimit(5000);

		// Verify if phase switching to three phases occurred to meet the new power
		// demand
		System.out.println("Verifying if phase switching to THREE_PHASE occurred");
		assertEquals("Should switch to three phases", Phases.THREE_PHASE, this.evcs.getPhases());

		// Expected min/max power range after accommodating the increased power demand
		Integer expectedMin = 1380; // Min power remains the same as one phase
		Integer expectedMax = 22080; // Max power with phase switching for an EVCS capable of 32A

		// Validate the adjusted min/max power limits
		System.out.println("Validating adjusted min/max power limits after increased demand");
		assertEquals("Adjusted Min Power should be " + expectedMin, expectedMin,
				this.evcs.getMinimumHardwarePower().get());
		assertEquals("Adjusted Max Power should be " + expectedMax, expectedMax,
				this.evcs.getMaximumHardwarePower().get());
	}

	@Test
	public void testShouldSwitchToThreePhases() {
		// Scenario: High power demand on single phase
		int highPowerDemand = 5000; // Watts
		int currentPhases = 1;
		assertTrue("EVCS should switch to three phases for high power demand",
				this.evcs.shouldSwitchToThreePhases(highPowerDemand, currentPhases));
	}

	@Test
	public void testShouldNotSwitchToThreePhases() {
		// Scenario: Low power demand does not require three phases
		int lowPowerDemand = 3000; // Watts
		int currentPhases = 1;
		assertFalse("EVCS should not switch to three phases for low power demand",
				this.evcs.shouldSwitchToThreePhases(lowPowerDemand, currentPhases));
	}

	@Test
	public void testShouldSwitchToOnePhase() {
		// Scenario: Low power demand on three phases
		int lowPowerDemand = 2000; // Watts
		int currentPhases = 3;
		assertTrue("EVCS should switch to one phase for low power demand",
				this.evcs.shouldSwitchToOnePhase(lowPowerDemand, currentPhases));
	}

	@Test
	public void testShouldNotSwitchToOnePhase() {
		// Scenario: High power demand does not benefit from reducing to one phase
		int highPowerDemand = 7000; // Watts
		int currentPhases = 3;
		assertFalse("EVCS should not switch to one phase for high power demand",
				this.evcs.shouldSwitchToOnePhase(highPowerDemand, currentPhases));
	}

}
