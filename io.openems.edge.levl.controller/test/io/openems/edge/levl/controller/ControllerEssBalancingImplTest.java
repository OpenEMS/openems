package io.openems.edge.levl.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerEssBalancingImplTest {

	private ControllerEssBalancingImpl underTest;

	@Before
	public void setUp() {
		this.underTest = new ControllerEssBalancingImpl();
	}

	@Test
	public void testCalculateRequiredPower() throws OpenemsNamedException {
		this.underTest.cycle = new DummyCycle(1000);
		this.underTest.ess = new DummyManagedSymmetricEss("ess0").setPower(new DummyPower(0.3, 0.3, 0.1))
				.withActivePower(-100).withCapacity(500) // 1.800.000 Ws
				.withSoc(50) // 900.000 Ws
				.withMaxApparentPower(500);
		this.underTest.meter = new DummyElectricityMeter("meter0").withActivePower(200);

		this.setActiveChannelValue(this.underTest.getLevlSocChannel(), 2000L);
		this.setActiveChannelValue(this.underTest.getRemainingLevlEnergyChannel(), 200000L);
		this.setActiveChannelValue(this.underTest.getEfficiencyChannel(), 100.0);
		this.setActiveChannelValue(this.underTest.getSocLowerBoundLevlChannel(), 20.0);
		this.setActiveChannelValue(this.underTest.getSocUpperBoundLevlChannel(), 80.0);
		this.setActiveChannelValue(this.underTest.getBuyFromGridLimitChannel(), 1000L);
		this.setActiveChannelValue(this.underTest.getSellToGridLimitChannel(), -1000L);
		this.setActiveChannelValue(this.underTest.getInfluenceSellToGridChannel(), true);

		int result = this.underTest.calculateRequiredPower();

		Assert.assertEquals(500, result);
	}

	// Primary use case calculation
	@Test
	public void testApplyPucSocBounds() {
		Assert.assertEquals("good case discharge", 30, this.underTest.applyPucSocBounds(30, 50, 100, 100, 1));
		Assert.assertEquals("good case charge", -30, this.underTest.applyPucSocBounds(-30, 50, 100, 100, 1));
		Assert.assertEquals("minimum limit applies", 50, this.underTest.applyPucSocBounds(70, 50, 100, 100, 1));
		Assert.assertEquals("minimum limit applies due to cycleTime", 25,
				this.underTest.applyPucSocBounds(30, 50, 100, 100, 2));
		Assert.assertEquals("maximum limit applies", -50, this.underTest.applyPucSocBounds(-70, 50, 100, 100, 1));
		Assert.assertEquals("no charging allowed because soc is 100%", 0,
				this.underTest.applyPucSocBounds(-20, 100, 100, 100, 1));
		Assert.assertEquals("no discharging allowed because soc is 0%", 0,
				this.underTest.applyPucSocBounds(20, 0, 100, 100, 1));
		Assert.assertEquals("discharging allowed with soc 100%", 20,
				this.underTest.applyPucSocBounds(20, 100, 100, 100, 1));
		Assert.assertEquals("charging allowed with soc 0%", -20, this.underTest.applyPucSocBounds(-20, 0, 100, 100, 1));

		// efficiency 80%
		Assert.assertEquals("good case discharge /w efficiency", 30,
				this.underTest.applyPucSocBounds(30, 50, 100, 80, 1));
		Assert.assertEquals("good case charge /w efficiency", -30,
				this.underTest.applyPucSocBounds(-30, 50, 100, 80, 1));
		Assert.assertEquals("minimum limit applies /w efficiency", 40,
				this.underTest.applyPucSocBounds(70, 50, 100, 80, 1));
		Assert.assertEquals("maximum limit applies /w efficiency", -62,
				this.underTest.applyPucSocBounds(-70, 50, 100, 80, 1));
	}

	@Test
	public void testCalculatePucBatteryPower() {
		Assert.assertEquals("discharge within battery limit", 70,
				this.underTest.calculatePucBatteryPower(50, 20, 500, 1000, -150, 150, 100, 1));
		Assert.assertEquals("discharge outside battery limit", 150,
				this.underTest.calculatePucBatteryPower(200, 20, 500, 1000, -150, 150, 100, 1));
		Assert.assertEquals("charge outside battery limit", -150,
				this.underTest.calculatePucBatteryPower(-200, -20, 500, 1000, -150, 150, 100, 1));
	}

	// Levl Power calculation
	@Test
	public void testApplyBatteryPowerLimitsToLevlPower() {
		Assert.assertEquals(70, this.underTest.applyBatteryPowerLimitsToLevlPower(100, 30, -100, 100));
		Assert.assertEquals(50, this.underTest.applyBatteryPowerLimitsToLevlPower(50, 30, -100, 100));
		Assert.assertEquals(-100, this.underTest.applyBatteryPowerLimitsToLevlPower(-100, 30, -100, 100));
		Assert.assertEquals(-130, this.underTest.applyBatteryPowerLimitsToLevlPower(-150, 30, -100, 100));
	}

	@Test
	public void testApplySocBoundariesToLevlPower() {
		Assert.assertEquals(-22, this.underTest.applySocBoundariesToLevlPower(-100, 60, 0, 20, 80, 100, 90, 1));
		Assert.assertEquals(-10, this.underTest.applySocBoundariesToLevlPower(-10, 60, 0, 20, 80, 100, 90, 1));
		Assert.assertEquals(10, this.underTest.applySocBoundariesToLevlPower(10, 60, 0, 20, 80, 100, 90, 1));
		Assert.assertEquals(36, this.underTest.applySocBoundariesToLevlPower(100, 60, 0, 20, 80, 100, 90, 1));
	}

	@Test
	public void testApplyGridPowerLimitsToLevlPower() {
		Assert.assertEquals("levlPower within limits", 50,
				this.underTest.applyGridPowerLimitsToLevlPower(50, 0, 80, -70));
		Assert.assertEquals("levlPower within limits balancing grid", 100,
				this.underTest.applyGridPowerLimitsToLevlPower(100, 40, 80, -70));
		Assert.assertEquals("levlPower constraint by sellToGridLimit", 50,
				this.underTest.applyGridPowerLimitsToLevlPower(100, -20, 80, -70));
		Assert.assertEquals("levlPower constraint by buyFromGridLimit", -60,
				this.underTest.applyGridPowerLimitsToLevlPower(-100, 20, 80, -70));
	}

	@Test
	public void testInfluenceSellToGridConstraint() {
		Assert.assertEquals("influence allowed", 50, this.underTest.applyInfluenceSellToGridConstraint(50, 0, true));

		Assert.assertEquals("buy from grid is allowed", -50,
				this.underTest.applyInfluenceSellToGridConstraint(-50, 20, false));
		Assert.assertEquals("switch gridPower /w buy from grid to sell to grid not allowed", 20,
				this.underTest.applyInfluenceSellToGridConstraint(50, 20, false));
		Assert.assertEquals("do nothing because grid power sells to grid", 0,
				this.underTest.applyInfluenceSellToGridConstraint(-50, -20, false));
	}

	@Test
	public void testHandleEvent_before_currentActive() {
		Clock clock = Clock.fixed(Instant.ofEpochSecond(1729778400), ZoneOffset.UTC); // 2024-10-24T14:00:00
		ControllerEssBalancingImpl.clock = clock;

		LevlControlRequest currentRequest = new LevlControlRequest();
		currentRequest.start = LocalDateTime.of(2024, 10, 24, 14, 0, 0);
		currentRequest.deadline = LocalDateTime.of(2024, 10, 24, 14, 14, 59);
		this.underTest.currentRequest = currentRequest;

		LevlControlRequest nextRequest = new LevlControlRequest();
		nextRequest.start = LocalDateTime.of(2024, 10, 24, 14, 15, 0);
		nextRequest.deadline = LocalDateTime.of(2024, 10, 24, 14, 29, 59);
		this.underTest.nextRequest = nextRequest;

		Event event = new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, new HashMap<>());

		this.underTest.handleEvent(event);

		Assert.assertEquals(currentRequest, this.underTest.currentRequest);
	}

	@Test
	public void testHandleEvent_before_nextRequestIsActive() {
		// 2024-10-24T14:15:00
		Clock clock = Clock.fixed(Instant.ofEpochSecond(1729779300), ZoneOffset.UTC);
		ControllerEssBalancingImpl.clock = clock;

		LevlControlRequest currentRequest = new LevlControlRequest();
		currentRequest.start = LocalDateTime.of(2024, 10, 24, 14, 0, 0);
		currentRequest.deadline = LocalDateTime.of(2024, 10, 24, 14, 14, 59);
		this.underTest.currentRequest = currentRequest;

		LevlControlRequest nextRequest = new LevlControlRequest();
		nextRequest.start = LocalDateTime.of(2024, 10, 24, 14, 15, 0);
		nextRequest.deadline = LocalDateTime.of(2024, 10, 24, 14, 29, 59);
		this.underTest.nextRequest = nextRequest;

		this.setNextChannelValue(this.underTest.getRealizedEnergyGridChannel(), 100L);
		this.setNextChannelValue(this.underTest.getRealizedEnergyBatteryChannel(), 200L);

		Event event = new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, new HashMap<>());

		this.underTest.handleEvent(event);

		Assert.assertEquals(nextRequest, this.underTest.currentRequest);
		Assert.assertNull(this.underTest.nextRequest);
		Assert.assertEquals(0, this.underTest.getRealizedEnergyGridChannel().getNextValue().get().longValue());
		Assert.assertEquals(0, this.underTest.getRealizedEnergyBatteryChannel().getNextValue().get().longValue());
	}

	@Test
	public void testHandleEvent_before_gapBetweenRequests() {
		Clock clock = Clock.fixed(Instant.ofEpochSecond(1729779300), ZoneOffset.UTC); // 2024-10-24T14:15:00
		ControllerEssBalancingImpl.clock = clock;

		LevlControlRequest currentRequest = new LevlControlRequest();
		currentRequest.start = LocalDateTime.of(2024, 10, 24, 14, 0, 0);
		currentRequest.deadline = LocalDateTime.of(2024, 10, 24, 14, 14, 59);
		this.underTest.currentRequest = currentRequest;

		LevlControlRequest nextRequest = new LevlControlRequest();
		nextRequest.start = LocalDateTime.of(2024, 10, 24, 14, 16, 0);
		nextRequest.deadline = LocalDateTime.of(2024, 10, 24, 14, 29, 59);
		this.underTest.nextRequest = nextRequest;

		this.setNextChannelValue(this.underTest.getRealizedEnergyGridChannel(), 100L);
		this.setNextChannelValue(this.underTest.getRealizedEnergyBatteryChannel(), 200L);

		Event event = new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, new HashMap<>());

		this.underTest.handleEvent(event);

		Assert.assertNull(this.underTest.currentRequest);
		Assert.assertEquals(nextRequest, this.underTest.nextRequest);
		Assert.assertEquals(0, this.underTest.getRealizedEnergyGridChannel().getNextValue().get().longValue());
		Assert.assertEquals(0, this.underTest.getRealizedEnergyBatteryChannel().getNextValue().get().longValue());
	}

	@Test
	public void testHandleEvent_before_noNextRequest() {
		// 2024-10-24T14:15:00
		Clock clock = Clock.fixed(Instant.ofEpochSecond(1729779300), ZoneOffset.UTC);
		ControllerEssBalancingImpl.clock = clock;

		LevlControlRequest currentRequest = new LevlControlRequest();
		currentRequest.start = LocalDateTime.of(2024, 10, 24, 14, 0, 0);
		currentRequest.deadline = LocalDateTime.of(2024, 10, 24, 14, 14, 59);
		this.underTest.currentRequest = currentRequest;

		this.setNextChannelValue(this.underTest.getRealizedEnergyGridChannel(), 100L);
		this.setNextChannelValue(this.underTest.getRealizedEnergyBatteryChannel(), 200L);

		Event event = new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, new HashMap<>());

		this.underTest.handleEvent(event);

		Assert.assertNull(this.underTest.currentRequest);
		Assert.assertNull(this.underTest.nextRequest);
		Assert.assertEquals(0, this.underTest.getRealizedEnergyGridChannel().getNextValue().get().longValue());
		Assert.assertEquals(0, this.underTest.getRealizedEnergyBatteryChannel().getNextValue().get().longValue());
	}

	@Test
	public void testHandleEvent_before_noRequests() {
		Event event = new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, new HashMap<>());

		Clock clock = Clock.fixed(Instant.ofEpochSecond(1729779300), ZoneOffset.UTC); // 2024-10-24T14:15:00
		ControllerEssBalancingImpl.clock = clock;

		this.underTest.handleEvent(event);

		Assert.assertNull(this.underTest.currentRequest);
		Assert.assertNull(this.underTest.nextRequest);
	}

	@Test
	public void testHandleEvent_after() {
		this.underTest.ess = new DummyManagedSymmetricEss("ess0");
		this.underTest.cycle = new DummyCycle(1000);
		this.setNextChannelValue(this.underTest.ess.getDebugSetActivePowerChannel(), -100);
		this.setNextChannelValue(this.underTest.getPucBatteryPowerChannel(), 10L);
		this.setActiveChannelValue(this.underTest.getLevlSocChannel(), 40L);
		this.setActiveChannelValue(this.underTest.getRemainingLevlEnergyChannel(), -1000L);
		this.setActiveChannelValue(this.underTest.getEfficiencyChannel(), 80.0);
		this.underTest.currentRequest = new LevlControlRequest();

		this.setActiveChannelValue(this.underTest.getRealizedEnergyGridChannel(), -20L);
		this.setActiveChannelValue(this.underTest.getRealizedEnergyBatteryChannel(), -30L);

		Event event = new Event(EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE, new HashMap<>());

		this.underTest.handleEvent(event);

		Assert.assertEquals(-890, this.underTest.getRemainingLevlEnergyChannel().getNextValue().get().longValue());
		Assert.assertEquals(-130, this.underTest.getRealizedEnergyGridChannel().getNextValue().get().longValue());
		Assert.assertEquals(-118, this.underTest.getRealizedEnergyBatteryChannel().getNextValue().get().longValue());
		Assert.assertEquals(128, this.underTest.getLevlSocChannel().getNextValue().get().longValue());
	}

	@Test
	public void testHandleRequest() throws OpenemsNamedException {
		JsonObject params = new JsonObject();
		params.addProperty("levlRequestId", "id");
		params.addProperty("levlRequestTimestamp", "2024-10-24T14:15:00Z");
		params.addProperty("levlPowerW", 500);
		params.addProperty("levlChargeDelaySec", 900);
		params.addProperty("levlChargeDurationSec", 900);
		params.addProperty("levlSocWh", 10000);
		params.addProperty("levlSocLowerBoundPercent", 20);
		params.addProperty("levlSocUpperBoundPercent", 80);
		params.addProperty("sellToGridLimitW", 3000);
		params.addProperty("buyFromGridLimitW", 4000);
		params.addProperty("efficiencyPercent", 90);
		params.addProperty("influenceSellToGrid", true);
		JsonrpcRequest request = new GenericJsonrpcRequest("sendLevlControlRequest", params);
		Call<JsonrpcRequest, JsonrpcResponse> call = new Call<JsonrpcRequest, JsonrpcResponse>(request);

		Clock clock = Clock.fixed(Instant.ofEpochSecond(1729778400), ZoneOffset.UTC); // 2024-10-24T14:00:00
		LevlControlRequest.clock = clock;
		LevlControlRequest expectedNextRequest = new LevlControlRequest(3000, 4000, "id", "2024-10-24T14:15:00Z",
				(500 * 900), LocalDateTime.of(2024, 10, 24, 14, 15, 0), LocalDateTime.of(2024, 10, 24, 14, 30, 0),
				10000, 20, 80, 90, true);
		this.setActiveChannelValue(this.underTest.getRealizedEnergyBatteryChannel(), -100L);

		this.underTest.handleRequest(call);

		Assert.assertEquals(expectedNextRequest, this.underTest.nextRequest);
		Assert.assertEquals(36000100, this.underTest.getLevlSocChannel().getNextValue().get().longValue());
	}

	public void setActiveChannelValue(AbstractReadChannel<?, ?> channel, Object value) {
		channel.setNextValue(value);
		channel.nextProcessImage();
	}

	public void setNextChannelValue(AbstractReadChannel<?, ?> channel, Object value) {
		channel.setNextValue(value);
	}
}
