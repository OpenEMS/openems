package io.openems.edge.controller.ess.sohcycle.statemachine;

import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_CELL_VOLTAGE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MIN_CELL_VOLTAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.ess.sohcycle.BatteryBalanceError;
import io.openems.edge.controller.ess.sohcycle.BatteryBalanceStatus;
import io.openems.edge.controller.ess.sohcycle.Config;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl;
import io.openems.edge.controller.ess.sohcycle.MyConfig;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ContextTest {

	private static final String ESS_ID = "ess0";
	private static final String CONTROLLER_ID = "ctrl0";
	private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
	private static final int DEFAULT_CAPACITY = 10_000;

	private ControllerEssSohCycleImpl controller;
	private DummyManagedSymmetricEss ess;
	private Config config;
	private Clock clock;

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
	}

	@Test
	public void testRefreshMeasurementChargingVoltageRange_Reset() {
		// Arrange
		var context = new Context(this.controller, this.config, this.clock, this.ess);
		TestUtils.withValue(this.ess, MIN_CELL_VOLTAGE, 3250);
		context.refreshMeasurementChargingVoltageRange();
		assertEquals(Integer.valueOf(3250), context.getMeasurementChargingMaxMinVoltage());

		// Act - reset (simulate end of measurement cycle)
		context.resetMeasurementChargingMaxMinVoltage();

		// Assert
		assertNull(context.getMeasurementChargingMaxMinVoltage());

		// Act - start new measurement cycle
		TestUtils.withValue(this.ess, MIN_CELL_VOLTAGE, 3180);
		context.refreshMeasurementChargingVoltageRange();

		// Assert - should start fresh with new value
		assertEquals(Integer.valueOf(3180), context.getMeasurementChargingMaxMinVoltage());
	}

	@Test
	public void testCalculateCellVoltageDelta_AfterRefresh() {
		// Test scenario: after at least one refresh call with defined minCellVoltage,
		// calculateCellVoltageDelta() returns non-null non-negative delta when maxCellVoltage is defined
		var context = new Context(this.controller, this.config, this.clock, this.ess);

		// Initially, delta should be null (baseline not captured)
		TestUtils.withValue(this.ess, MAX_CELL_VOLTAGE, 3300);
		var deltaBefore = context.calculateCellVoltageDelta();
		assertNull("Delta should be null before refresh", deltaBefore);

		// Refresh baseline from ESS
		TestUtils.withValue(this.ess, MIN_CELL_VOLTAGE, 3200);
		context.refreshMeasurementChargingVoltageRange();

		// After refresh, delta should be non-null and >= 0
		var deltaAfter = context.calculateCellVoltageDelta();
		assertEquals("Delta should be 100 mV after refresh", Integer.valueOf(100), deltaAfter);
	}

	@Test
	public void testCalculateCellVoltageDeltaWithReason_BothValuesPresent() {
		// Test: calculateCellVoltageDeltaWithReason uses stored max voltages
		var context = new Context(this.controller, this.config, this.clock, this.ess);

		// Set both stored max voltages
		TestUtils.withValue(this.ess, MIN_CELL_VOLTAGE, 3200);
		TestUtils.withValue(this.ess, MAX_CELL_VOLTAGE, 3300);
		context.refreshMeasurementChargingVoltageRange();

		// Calculate delta with reason
		var result = context.calculateCellVoltageDeltaWithReason();

		// Verify: delta should be 100 mV (3300 - 3200)
		assertEquals("Delta should be 100 mV", Integer.valueOf(100), result.delta());
		assertEquals("Error should be NONE", BatteryBalanceError.NONE, result.error());
	}

	@Test
	public void testCalculateCellVoltageDeltaWithReason_MissingMaxVoltage() {
		// Test: when max voltage is not stored, should return MAX_VOLTAGE_UNDEFINED
		var context = new Context(this.controller, this.config, this.clock, this.ess);

		// Set only min voltage (max not stored)
		TestUtils.withValue(this.ess, MIN_CELL_VOLTAGE, 3200);
		context.refreshMeasurementChargingVoltageRange();

		// Calculate delta with reason - max voltage is still null
		var result = context.calculateCellVoltageDeltaWithReason();

		// Verify: delta should be null with MAX_VOLTAGE_UNDEFINED error
		assertNull("Delta should be null when max voltage is missing", result.delta());
		assertEquals("Error should be MAX_VOLTAGE_UNDEFINED", BatteryBalanceError.MAX_VOLTAGE_UNDEFINED, result.error());
	}

	@Test
	public void testCalculateCellVoltageDeltaWithReason_MissingMinVoltage() {
		// Test: when min voltage is not stored, should return BASELINE_MIN_MISSING
		var context = new Context(this.controller, this.config, this.clock, this.ess);

		// Set only max voltage (min not stored)
		TestUtils.withValue(this.ess, MAX_CELL_VOLTAGE, 3300);
		context.refreshMeasurementChargingVoltageRange();

		// Calculate delta with reason - min voltage is still null
		var result = context.calculateCellVoltageDeltaWithReason();

		// Verify: delta should be null with BASELINE_MIN_MISSING error
		assertNull("Delta should be null when min voltage is missing", result.delta());
		assertEquals("Error should be BASELINE_MIN_MISSING", BatteryBalanceError.BASELINE_MIN_MISSING, result.error());
	}

	@Test
	public void testCalculateCellVoltageDeltaWithReason_NegativeDelta() {
		// Test: when stored max < stored min (unusual scenario)
		var context = new Context(this.controller, this.config, this.clock, this.ess);

		// Manually set inverted values (this shouldn't happen normally)
		context.setMeasurementChargingMaxMinVoltage(3300);
		context.setMeasurementChargingMaxVoltage(3200); // max < min

		// Calculate delta with reason
		var result = context.calculateCellVoltageDeltaWithReason();

		// Verify: delta should be null with DELTA_NEGATIVE error
		assertNull("Delta should be null when max < min", result.delta());
		assertEquals("Error should be DELTA_NEGATIVE", BatteryBalanceError.DELTA_NEGATIVE, result.error());
	}

	@Test
	public void testRefreshMeasurementChargingVoltageRange_TracksMaxOfMaxVoltage() {
		// Test: verify that refreshMeasurementChargingVoltageRange tracks the maximum MAX_CELL_VOLTAGE
		var context = new Context(this.controller, this.config, this.clock, this.ess);

		// First refresh with max voltage 3300
		TestUtils.withValue(this.ess, MIN_CELL_VOLTAGE, 3200);
		TestUtils.withValue(this.ess, MAX_CELL_VOLTAGE, 3300);
		context.refreshMeasurementChargingVoltageRange();

		assertEquals("Stored min should be 3200", Integer.valueOf(3200), context.getMeasurementChargingMaxMinVoltage());
		assertEquals("Stored max should be 3300", Integer.valueOf(3300), context.getMeasurementChargingMaxVoltage());

		// Second refresh with higher max voltage
		TestUtils.withValue(this.ess, MIN_CELL_VOLTAGE, 3210);
		TestUtils.withValue(this.ess, MAX_CELL_VOLTAGE, 3310);
		context.refreshMeasurementChargingVoltageRange();

		assertEquals("Stored min should be updated to 3210", Integer.valueOf(3210), context.getMeasurementChargingMaxMinVoltage());
		assertEquals("Stored max should be updated to 3310", Integer.valueOf(3310), context.getMeasurementChargingMaxVoltage());

		// Third refresh with lower max voltage (should not update)
		TestUtils.withValue(this.ess, MIN_CELL_VOLTAGE, 3190);
		TestUtils.withValue(this.ess, MAX_CELL_VOLTAGE, 3290);
		context.refreshMeasurementChargingVoltageRange();

		assertEquals("Stored min should remain 3210", Integer.valueOf(3210), context.getMeasurementChargingMaxMinVoltage());
		assertEquals("Stored max should remain 3310", Integer.valueOf(3310), context.getMeasurementChargingMaxVoltage());
	}

	@Test
	public void testResetMeasurementChargingMaxVoltage() {
		// Test: verify that resetMeasurementChargingMaxVoltage clears the max voltage
		var context = new Context(this.controller, this.config, this.clock, this.ess);

		// Set max voltage
		context.setMeasurementChargingMaxVoltage(3300);
		assertEquals("Stored max should be 3300", Integer.valueOf(3300), context.getMeasurementChargingMaxVoltage());

		// Reset
		context.resetMeasurementChargingMaxVoltage();

		// Verify it's cleared
		assertNull("Stored max should be null after reset", context.getMeasurementChargingMaxVoltage());
	}

	@Test
	public void testResetControllerClearsStateAndDiagnostics() {
		var context = new Context(this.controller, this.config, this.clock, this.ess);

		// Prime internal state and channels
		context.setMeasurementChargingMaxMinVoltage(3200);
		context.setMeasurementStartEnergyWh(12_345L);
		ChannelUtils.setValue(this.controller, ControllerEssSohCycle.ChannelId.MEASURED_CAPACITY, 15_000L);
		ChannelUtils.setValue(this.controller, ControllerEssSohCycle.ChannelId.VOLTAGE_DELTA, 42);
		ChannelUtils.setValue(this.controller, ControllerEssSohCycle.ChannelId.IS_BATTERY_BALANCED,
				BatteryBalanceStatus.ERROR);
		ChannelUtils.setValue(this.controller, ControllerEssSohCycle.ChannelId.BALANCING_DELTA_MV_DEBUG, 99L);
		ChannelUtils.setValue(this.controller, ControllerEssSohCycle.ChannelId.BALANCING_ERROR_DEBUG,
				BatteryBalanceError.INTERNAL_ERROR);
		ChannelUtils.setValue(this.controller, ControllerEssSohCycle.ChannelId.SOH_PERCENT, 85);
		ChannelUtils.setValue(this.controller, ControllerEssSohCycle.ChannelId.SOH_RAW_DEBUG, 87.5f);
		ChannelUtils.setValue(this.controller, ControllerEssSohCycle.ChannelId.IS_MEASURED, true);
		this.updateAllChannels();
		// Act
		context.resetController();
		this.updateAllChannels();
		// Assert internal state cleared
		assertNull(context.getMeasurementChargingMaxMinVoltage());
		assertNull(context.getMeasurementStartEnergyWh());

		// Assert channels cleared/reset
		assertNull(this.controller.getMeasuredCapacityChannel().value().get());
		assertNull(this.controller.channel(ControllerEssSohCycle.ChannelId.VOLTAGE_DELTA).value().get());
		assertEquals(BatteryBalanceStatus.NOT_MEASURED,
				this.controller.channel(ControllerEssSohCycle.ChannelId.IS_BATTERY_BALANCED).value().asEnum());
		assertNull(this.controller.channel(ControllerEssSohCycle.ChannelId.BALANCING_DELTA_MV_DEBUG).value().get());
		assertEquals(BatteryBalanceError.NONE,
				this.controller.channel(ControllerEssSohCycle.ChannelId.BALANCING_ERROR_DEBUG).value().asEnum());
		assertNull(this.controller.channel(ControllerEssSohCycle.ChannelId.SOH_PERCENT).value().get());
		assertNull(this.controller.channel(ControllerEssSohCycle.ChannelId.SOH_RAW_DEBUG).value().get());
		assertFalse((boolean) this.controller.channel(ControllerEssSohCycle.ChannelId.IS_MEASURED).value().get());
	}

	private void updateAllChannels() {
		this.controller.channels().forEach(Channel::nextProcessImage);
	}
}
