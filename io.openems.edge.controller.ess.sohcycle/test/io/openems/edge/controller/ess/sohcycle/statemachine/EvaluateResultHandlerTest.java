package io.openems.edge.controller.ess.sohcycle.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.ess.sohcycle.Config;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl;
import io.openems.edge.controller.ess.sohcycle.MyConfig;
import io.openems.edge.controller.ess.sohcycle.Utils;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class EvaluateResultHandlerTest {

	private static final String ESS_ID = "ess0";
	private static final String CONTROLLER_ID = "ctrl0";
	private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
	private static final int DEFAULT_CAPACITY = 10_000;

	private ControllerEssSohCycleImpl controller;
	private DummyManagedSymmetricEss ess;
	private Config config;
	private Clock clock;
	private Context context;

	@Before
	public void setup() {
		this.controller = new ControllerEssSohCycleImpl();
		this.ess = new DummyManagedSymmetricEss(ESS_ID) //
				.withMaxApparentPower(DEFAULT_MAX_APPARENT_POWER) //
				.withCapacity(DEFAULT_CAPACITY);
		this.clock = Clock.fixed(Instant.parse("2000-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		this.config = MyConfig.create() //
				.setId(CONTROLLER_ID) //
				.setEssId(ESS_ID) //
				.setRunning(true) //
				.build();
		this.context = new Context(this.controller, this.config, this.clock, this.ess);
	}

	@Test
	public void testSuccessfulEvaluationSetsAllChannels() {
		// Arrange
		final long startWh = 50_000L;
		final long endWh = 65_000L;
		final long expectedMeasuredCapacity = endWh - startWh; // 15_000 Wh
		final int soc = 10;

		// Set up the measurement baseline
		this.controller.setMeasurementStartEnergyWh(startWh);

		// Set up the current energy and SoC
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, endWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.SOC, soc);

		// Act
		var handler = new EvaluateResultHandler();
		var nextState = handler.runAndGetNextState(this.context);
		this.controller.channels().forEach(Channel::nextProcessImage);
		// Assert
		assertEquals("Handler should transition to DONE state", StateMachine.State.DONE, nextState);

		// Verify SOH_PERCENT is set
		var sohPercent = (Integer) this.controller.channel(ControllerEssSohCycle.ChannelId.SOH_PERCENT).value().get();
		assertNotNull("SOH_PERCENT should be set", sohPercent);

		// Calculate expected SoH
		var sohResult = this.context.calculateSoh(expectedMeasuredCapacity);
		assertTrue("SoH calculation should succeed", sohResult.isPresent());
		var expectedSoh = sohResult.get().soh();
		assertEquals("SOH_PERCENT should match calculated value", expectedSoh, (int) sohPercent);

		// Verify SOH_RAW_DEBUG is set
		var sohRawDebug = (Float) this.controller.channel(ControllerEssSohCycle.ChannelId.SOH_RAW_DEBUG).value().get();
		assertNotNull("SOH_RAW_DEBUG should be set", sohRawDebug);
		var expectedSohRaw = Utils.round2(sohResult.get().sohRaw());
		assertEquals("SOH_RAW_DEBUG should match rounded calculated value", expectedSohRaw, (float) sohRawDebug,
				0.001f);

		// Verify MEASURED_CAPACITY is set
		var measuredCapacity = (Long) this.controller.channel(ControllerEssSohCycle.ChannelId.MEASURED_CAPACITY).value()
				.get();
		assertNotNull("MEASURED_CAPACITY should be set", measuredCapacity);
		assertEquals("MEASURED_CAPACITY should match delta between end and start", expectedMeasuredCapacity,
				(long) measuredCapacity);

		// Verify IS_MEASURED is set
		var isMeasured = (Boolean) this.controller.channel(ControllerEssSohCycle.ChannelId.IS_MEASURED).value().get();
		assertNotNull("IS_MEASURED should be set", isMeasured);
		assertTrue("IS_MEASURED should be true", (boolean) isMeasured);
	}

	@Test
	public void testEvaluationWithDifferentCapacities() {
		// Arrange - Test with 8000 Wh capacity (80% SoH)
		final long startWh = 100_000L;
		final long endWh = 108_000L;
		final long expectedMeasuredCapacity = 8_000L;
		final int soc = 5;

		this.controller.setMeasurementStartEnergyWh(startWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, endWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.SOC, soc);

		// Act
		var handler = new EvaluateResultHandler();
		var nextState = handler.runAndGetNextState(this.context);
		this.controller.channels().forEach(Channel::nextProcessImage);
		// Assert
		assertEquals("Handler should transition to DONE state", StateMachine.State.DONE, nextState);

		// Verify MEASURED_CAPACITY
		var measuredCapacity = (Long) this.controller.channel(ControllerEssSohCycle.ChannelId.MEASURED_CAPACITY).value()
				.get();
		assertEquals("MEASURED_CAPACITY should be 8000 Wh", expectedMeasuredCapacity, (long) measuredCapacity);

		// Verify SOH_PERCENT (8000/10000 = 80%)
		var sohPercent = (Integer) this.controller.channel(ControllerEssSohCycle.ChannelId.SOH_PERCENT).value().get();
		assertEquals("SOH_PERCENT should be 80%", 80, (int) sohPercent);

		// Verify IS_MEASURED
		var isMeasured = (Boolean) this.controller.channel(ControllerEssSohCycle.ChannelId.IS_MEASURED).value().get();
		assertTrue("IS_MEASURED should be true", (boolean) isMeasured);
	}

	@Test
	public void testMissingStartWhReturnsErrorAbort() {
		// Arrange - No measurement baseline set
		final long endWh = 65_000L;
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, endWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.SOC, 10);

		// Act
		var handler = new EvaluateResultHandler();
		var nextState = handler.runAndGetNextState(this.context);
		this.controller.channels().forEach(Channel::nextProcessImage);
		// Assert
		assertEquals("Handler should transition to ERROR_ABORT state", StateMachine.State.ERROR_ABORT, nextState);
	}

	@Test
	public void testUndefinedEndEnergyReturnsErrorAbort() {
		// Arrange - measurement baseline set but end energy is undefined
		final long startWh = 50_000L;
		this.controller.setMeasurementStartEnergyWh(startWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.SOC, 10);
		// Don't set ACTIVE_DISCHARGE_ENERGY, leave it undefined

		// Act
		var handler = new EvaluateResultHandler();
		var nextState = handler.runAndGetNextState(this.context);
		this.controller.channels().forEach(Channel::nextProcessImage);
		// Assert
		assertEquals("Handler should transition to ERROR_ABORT state", StateMachine.State.ERROR_ABORT, nextState);
	}

	@Test
	public void testNegativeMeasuredCapacityReturnsErrorAbort() {
		// Arrange - end energy is less than start energy (invalid)
		final long startWh = 65_000L;
		final long endWh = 50_000L; // Less than start
		this.controller.setMeasurementStartEnergyWh(startWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, endWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.SOC, 10);

		// Act
		var handler = new EvaluateResultHandler();
		var nextState = handler.runAndGetNextState(this.context);
		this.controller.channels().forEach(Channel::nextProcessImage);
		// Assert
		assertEquals("Handler should transition to ERROR_ABORT state", StateMachine.State.ERROR_ABORT, nextState);
	}

	@Test
	public void testZeroMeasuredCapacityReturnsErrorAbort() {
		// Arrange - start and end are the same (zero capacity)
		final long startWh = 50_000L;
		final long endWh = 50_000L;
		this.controller.setMeasurementStartEnergyWh(startWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, endWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.SOC, 10);

		// Act
		var handler = new EvaluateResultHandler();
		var nextState = handler.runAndGetNextState(this.context);
		this.controller.channels().forEach(Channel::nextProcessImage);
		// Assert
		assertEquals("Handler should transition to ERROR_ABORT state", StateMachine.State.ERROR_ABORT, nextState);
	}

	@Test
	public void testSohCalculationFailureReturnsErrorAbort() {
		// Arrange - ESS capacity is undefined, causing SoH calculation to fail
		final long startWh = 50_000L;
		final long endWh = 65_000L;
		this.controller.setMeasurementStartEnergyWh(startWh);

		// Create ESS without capacity
		this.ess = new DummyManagedSymmetricEss(ESS_ID).withMaxApparentPower(DEFAULT_MAX_APPARENT_POWER);
		this.context = new Context(this.controller, this.config, this.clock, this.ess);

		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, endWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.SOC, 10);

		// Act
		var handler = new EvaluateResultHandler();
		var nextState = handler.runAndGetNextState(this.context);
		this.controller.channels().forEach(Channel::nextProcessImage);
		// Assert
		assertEquals("Handler should transition to ERROR_ABORT state", StateMachine.State.ERROR_ABORT, nextState);
	}

	@Test
	public void testSohRawIsRoundedCorrectly() {
		// Arrange - Test that sohRaw is rounded to 2 decimal places
		final long startWh = 50_000L;
		final long endWh = 65_333L; // Will result in fractional SoH
		final long expectedMeasuredCapacity = 15_333L;
		this.controller.setMeasurementStartEnergyWh(startWh);

		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, endWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.SOC, 10);

		// Act
		var handler = new EvaluateResultHandler();
		var nextState = handler.runAndGetNextState(this.context);
		this.controller.channels().forEach(Channel::nextProcessImage);
		// Assert
		assertEquals("Handler should transition to DONE state", StateMachine.State.DONE, nextState);

		// Calculate expected values
		var sohResult = this.context.calculateSoh(expectedMeasuredCapacity);
		assertTrue("SoH calculation should succeed", sohResult.isPresent());

		var expectedSohRaw = Utils.round2(sohResult.get().sohRaw());

		var sohRawDebug = (Float) this.controller.channel(ControllerEssSohCycle.ChannelId.SOH_RAW_DEBUG).value().get();
		assertNotNull("SOH_RAW_DEBUG should be set", sohRawDebug);
		assertEquals("SOH_RAW_DEBUG should be rounded to 2 decimal places", expectedSohRaw, sohRawDebug, 0.001f);
	}

	@Test
	public void testHighSohValue() {
		// Arrange - Test with capacity greater than nominal (>100% SoH)
		final long startWh = 50_000L;
		final long endWh = 62_000L; // 12000 Wh measured, but nominal is only 10000

		// Create ESS with lower nominal capacity to test >100% SoH
		this.ess = new DummyManagedSymmetricEss(ESS_ID) //
				.withMaxApparentPower(DEFAULT_MAX_APPARENT_POWER) //
				.withCapacity(10_000);
		this.context = new Context(this.controller, this.config, this.clock, this.ess);

		this.controller.setMeasurementStartEnergyWh(startWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, endWh);
		TestUtils.withValue(this.ess, SymmetricEss.ChannelId.SOC, 10);

		// Act
		var handler = new EvaluateResultHandler();
		var nextState = handler.runAndGetNextState(this.context);
		this.controller.channels().forEach(Channel::nextProcessImage);
		// Assert
		assertEquals("Handler should transition to DONE state", StateMachine.State.DONE, nextState);

		// SOH_PERCENT should be capped at 100
		var sohPercent = (Integer) this.controller.channel(ControllerEssSohCycle.ChannelId.SOH_PERCENT).value().get();
		assertEquals("SOH_PERCENT should be capped at 100", 100, (int) sohPercent);

		// SOH_RAW_DEBUG should show the actual raw value (>100)
		var sohRawDebug = (Float) this.controller.channel(ControllerEssSohCycle.ChannelId.SOH_RAW_DEBUG).value().get();
		assertTrue("SOH_RAW_DEBUG should be > 100 for this test", (float) sohRawDebug > 100f);
	}

}