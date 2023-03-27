package io.openems.edge.airconditioner.hydac4kw;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.openems.edge.common.startstoppratelimited.StartFrequency;

public class RestartStateMachineTest {
	
	@Test
	public void test_available_start_count () {
		StartFrequency freq = StartFrequency.builder() //
				.withOccurence(2) //
				.withDuration(Duration.ofSeconds(2)) //
				.build();
		var stateMachine = new RestartController(freq, null, null);
		assertEquals(stateMachine.remainingStartsAvaliable(), 2);
	}

	@Test
	public void test_starts_once_when_multiple_start_request() throws InterruptedException {
		var startCounter = new AtomicInteger(0);
		var stopCounter = new AtomicInteger(0);
		StartFrequency freq = StartFrequency.builder() //
				.withOccurence(2) //
				.withDuration(Duration.ofSeconds(10)) //
				.build();
		var stateMachine = new RestartController(freq, () -> startCounter.incrementAndGet(), () -> stopCounter.incrementAndGet());
		Thread.sleep(500);
		stateMachine.requestStart();
		Thread.sleep(500);
		stateMachine.requestStart();
		Thread.sleep(500);
		stateMachine.requestStart();
		Thread.sleep(500);
		stateMachine.requestStop();
		assertEquals(1, startCounter.get());
		assertEquals(1, stateMachine.remainingStartsAvaliable());
		assertEquals(1, stopCounter.get());
	}
	
	@Test
	public void test_multiple_start_request_without_stop_does_not_use_start_attempt() throws InterruptedException {
		var startCounter = new AtomicInteger(0);
		var stopCounter = new AtomicInteger(0);
		StartFrequency freq = StartFrequency.builder() //
				.withOccurence(5) //
				.withDuration(Duration.ofSeconds(2)) //
				.build();
		var stateMachine = new RestartController(freq, () -> startCounter.incrementAndGet(), () -> stopCounter.incrementAndGet());
		Thread.sleep(500);
		stateMachine.requestStart();
		stateMachine.requestStart();
		stateMachine.requestStart();
		stateMachine.requestStop();
		
		assertEquals(1, startCounter.get());
		assertEquals(1, stopCounter.get());
		assertEquals(4, stateMachine.remainingStartsAvaliable());
	}
	
	@Test
	public void test_start_inhibited_when_counter_reaches_limit() throws InterruptedException {
		var state = new AtomicBoolean(false);

		StartFrequency freq = StartFrequency.builder() //
				.withOccurence(2) //
				.withDuration(Duration.ofSeconds(10)) //
				.build();
		var stateMachine = new RestartController(freq, () -> state.set(true), () -> state.set(false));
		Thread.sleep(500);
		// Start for first time
		stateMachine.requestStart();
		assertTrue(state.get());
		Thread.sleep(100);
		
		// Stop for first time
		stateMachine.requestStop();
		assertFalse(state.get());
		Thread.sleep(100);
		
		// Start it again...
		stateMachine.requestStart();
		assertTrue(state.get());
		Thread.sleep(100);
		
		// Stop it again...
		stateMachine.requestStop();
		assertFalse(state.get());
		Thread.sleep(100);
		
		// Should not start, since because of threshold.
		stateMachine.requestStart();
		assertFalse(state.get());
		// Also, there should be no remaining start attempts available.
		assertEquals(0, stateMachine.remainingStartsAvaliable());
	}

}
