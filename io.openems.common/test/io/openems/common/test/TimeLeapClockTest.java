package io.openems.common.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

public class TimeLeapClockTest {

	@Test
	public void testConstructor() {
		var instant = Instant.MIN;
		var zone = ZoneOffset.UTC;
		
		var clock = new TimeLeapClock(instant, zone);
		assertEquals(instant, clock.instant());
		assertEquals(zone, clock.getZone());
		
		var clockNow = new TimeLeapClock();
		assertEquals(clockNow.getZone(), ZoneOffset.UTC);
		assertEquals(clockNow.instant().toEpochMilli(), System.currentTimeMillis(), 100);
		
		var zoneBerlin = ZoneId.of("Europe/Berlin");
		var clock2 = clock.withZone(zoneBerlin);
		assertNotEquals(zone, zoneBerlin);
		assertEquals(instant, clock2.instant());
		assertEquals(zoneBerlin, clock2.getZone());
		
		var clock3 = clock.withZone(zone);
		assertEquals(clock, clock3);
		
		assertNotNull("ToString should return valid String!", clock.toString());
	}

	@Test
	public void testMillis() {
		var millis = 101L;
		var instant = Instant.ofEpochMilli(millis);
		
		var clock = new TimeLeapClock(instant);
		
		assertEquals(millis, clock.millis());
	}

	@Test
	public void testLeap() {
		var dateTime = ZonedDateTime.now();
		
		var instant = dateTime.toInstant();
		var zone = dateTime.getZone();
		
		var timeLeap = new TimeLeapClock(instant, zone);
		
		assertEquals(dateTime, timeLeap.now());
		
		timeLeap.leap(5, ChronoUnit.MINUTES);
		dateTime = dateTime.plusMinutes(5);
		
		assertEquals(dateTime, timeLeap.now());
		
		timeLeap.leap(2, ChronoUnit.DAYS);
		dateTime = dateTime.plusDays(2);
		
		assertEquals(dateTime, timeLeap.now());
		
		timeLeap.leap(3, ChronoUnit.HOURS);
		dateTime = dateTime.minusHours(3);
		
		assertNotEquals(dateTime, timeLeap.now());
	}

	@Test
	public void testEquals() {
		var instant = Instant.ofEpochMilli(512);
		var zone = ZoneOffset.UTC;
		
		var timeLeap = new TimeLeapClock(instant, zone);
		var timeLeapNow = new TimeLeapClock(zone);
		var timeLeapLater = new TimeLeapClock(instant.plusSeconds(20));
		
		var timeLeapZone = timeLeap.withZone(ZoneId.of("US/Hawaii"));
		var clock = Clock.fixed(instant, zone);
		
		assertEquals(timeLeap, timeLeap);
		assertEquals(timeLeap, clock);
		
		assertNotEquals(timeLeap, null);
		assertNotEquals(timeLeap, new Object());
		assertNotEquals(timeLeap, timeLeapZone);
		assertNotEquals(timeLeap, timeLeapNow);
		assertNotEquals(clock, timeLeapNow);
		assertNotEquals(timeLeapNow, timeLeapLater);
		
		assertEquals(timeLeap.hashCode(), clock.hashCode());
	}

}
