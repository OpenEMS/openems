package io.openems.backend.alerting.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Consumer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.backend.alerting.Dummy.TimeLeapMinuteTimer;
import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.Message;
import io.openems.common.event.EventReader;

public class SchedulerTest {

	/**
	 * Validate dummy methods.
	 */
	@BeforeClass
	public static void dummyClass() {
		var dummyMsg = new DummyMessage("", Instant.now(), 0);
		var dummyHan = new DummyHandler();
		Runnable[] methods = { () -> dummyHan.getEventHandler(null), () -> dummyHan.stop(), () -> dummyHan.getGeneric(),
				() -> dummyMsg.getParams(), };
		assertNotNull(dummyMsg.getNotifyStamp());
		for (var method : methods) {
			assertThrows(UnsupportedOperationException.class, () -> method.run());
		}
	}

	@Test
	public void testSchedule() {
		/* Prepare */
		final var now = Instant.now();
		final var handler = new DummyHandler();
		final var scheduler = new Scheduler(new TimeLeapMinuteTimer(now));

		final var msgScheduler = scheduler.register(handler);

		final var msg1 = new DummyMessage("0", now, -1);
		final var msg2 = new DummyMessage("1", now, 1);
		final var msg3 = new DummyMessage("2", now, -2);

		/* TEST */
		assertFalse(scheduler.isScheduled(msg1));
		assertFalse(scheduler.isScheduled(msg2));
		assertFalse(scheduler.isScheduled(msg3));

		msgScheduler.schedule(msg1);

		assertTrue(scheduler.isScheduled(msg1));
		assertFalse(scheduler.isScheduled(msg2));
		assertFalse(scheduler.isScheduled(msg3));

		msgScheduler.schedule(msg2);
		msgScheduler.schedule(msg3);

		assertTrue(scheduler.isScheduled(msg1));
		assertTrue(scheduler.isScheduled(msg2));
		assertTrue(scheduler.isScheduled(msg3));
	}

	@Test
	public void testRemove() {
		/* Prepare */
		final var now = Instant.now();
		final var handler = new DummyHandler();
		final var scheduler = new Scheduler(new TimeLeapMinuteTimer(now));

		final var msgScheduler = scheduler.register(handler);

		final var msg1 = new DummyMessage("0", now, -1);
		final var msg2 = new DummyMessage("1", now, 1);
		final var msg3 = new DummyMessage("2", now, -2);

		msgScheduler.schedule(msg1);
		msgScheduler.schedule(msg2);
		msgScheduler.schedule(msg3);

		/* Test */
		assertTrue(scheduler.isScheduled(msg1));
		assertTrue(scheduler.isScheduled(msg2));
		assertTrue(scheduler.isScheduled(msg3));

		msgScheduler.remove(null);
		msgScheduler.remove("");
		msgScheduler.remove(msg2.getId());

		assertTrue(scheduler.isScheduled(msg1));
		assertFalse(scheduler.isScheduled(msg2));
		assertTrue(scheduler.isScheduled(msg3));

		msgScheduler.remove(msg1.getId());
		msgScheduler.remove(msg2.getId());
		msgScheduler.remove(msg3.getId());

		assertFalse(scheduler.isScheduled(msg1));
		assertFalse(scheduler.isScheduled(msg2));
		assertFalse(scheduler.isScheduled(msg3));
	}

	@Test
	public void testUnRegister() {
		/* Prepare */
		final var now = Instant.now();
		final var scheduler = new Scheduler(new TimeLeapMinuteTimer(now));

		final var handler = new DummyHandler();
		final var msgScheduler = scheduler.register(handler);

		final var msg10 = new DummyMessage("10", now, -1);
		final var msg11 = new DummyMessage("11", now, 1);

		final var handler2 = new DummyHandler();
		final var msgScheduler2 = scheduler.register(handler2);

		final var msg20 = new DummyMessage("20", now, -2);
		final var msg21 = new DummyMessage("21", now, -2);

		msgScheduler.schedule(msg10);
		msgScheduler2.schedule(msg20);

		/* TEST */
		scheduler.start();

		assertTrue(scheduler.isScheduled(msg10));
		assertFalse(scheduler.isScheduled(msg11));
		assertTrue(scheduler.isScheduled(msg20));
		assertFalse(scheduler.isScheduled(msg21));

		scheduler.unregister(handler2);

		assertTrue(scheduler.isScheduled(msg10));
		assertFalse(scheduler.isScheduled(msg11));
		assertFalse(scheduler.isScheduled(msg20));
		assertFalse(scheduler.isScheduled(msg21));

		scheduler.unregister(handler);

		assertFalse(scheduler.isScheduled(msg10));
		assertFalse(scheduler.isScheduled(msg11));
		assertFalse(scheduler.isScheduled(msg20));
		assertFalse(scheduler.isScheduled(msg21));

		scheduler.stop();
	}

	@Test
	public void testHandle() {
		/* Prepare */
		final var now = Instant.now();
		final var timer = new TimeLeapMinuteTimer(now);
		final var scheduler = new Scheduler(timer);

		final var handler = new DummyHandler() {
			@Override
			public void send(ZonedDateTime sentAt, java.util.List<DummyMessage> messages) {
				for (var msg : messages) {
					if (msg.getId().equals("0ERR")) {
						throw new RuntimeException("Test Exception");
					}
				}
			}
		};
		final var msgScheduler = scheduler.register(handler);

		final var msg1 = new DummyMessage("0ERR", now, -60 /*-1m*/);
		final var msg2 = new DummyMessage("1", now, -1 /*-1s*/);
		final var msg3 = new DummyMessage("2", now, 120 /* 2m */);
		final var msg4 = new DummyMessage("3", now, 480 /* 8m */);
		final var msg5 = new DummyMessage("4", now, 1200/* 20m */);

		msgScheduler.schedule(msg1);
		msgScheduler.schedule(msg2);
		msgScheduler.schedule(msg3);
		msgScheduler.schedule(msg4);
		msgScheduler.schedule(msg5);

		/* Test */
		scheduler.start();

		assertTrue(scheduler.isScheduled(msg1));
		assertTrue(scheduler.isScheduled(msg2));
		assertTrue(scheduler.isScheduled(msg3));
		assertTrue(scheduler.isScheduled(msg4));
		assertTrue(scheduler.isScheduled(msg5));

		timer.cycle();

		assertFalse(scheduler.isScheduled(msg1));
		assertFalse(scheduler.isScheduled(msg2));
		assertTrue(scheduler.isScheduled(msg3));
		assertTrue(scheduler.isScheduled(msg4));
		assertTrue(scheduler.isScheduled(msg5));

		timer.leap(3);

		assertFalse(scheduler.isScheduled(msg1));
		assertFalse(scheduler.isScheduled(msg2));
		assertFalse(scheduler.isScheduled(msg3));
		assertTrue(scheduler.isScheduled(msg4));
		assertTrue(scheduler.isScheduled(msg5));

		timer.leap(3);

		assertFalse(scheduler.isScheduled(msg1));
		assertFalse(scheduler.isScheduled(msg2));
		assertFalse(scheduler.isScheduled(msg3));
		assertTrue(scheduler.isScheduled(msg4));
		assertTrue(scheduler.isScheduled(msg5));

		timer.leap(3);

		assertFalse(scheduler.isScheduled(msg1));
		assertFalse(scheduler.isScheduled(msg2));
		assertFalse(scheduler.isScheduled(msg3));
		assertFalse(scheduler.isScheduled(msg4));
		assertTrue(scheduler.isScheduled(msg5));

		scheduler.stop();
	}

	/* *********************************************** */
	private static class DummyMessage extends Message {
		private ZonedDateTime timeStamp;

		public DummyMessage(String messageId, Instant now, int timeShift) {
			super(messageId);
			if (timeShift >= 0) {
				this.timeStamp = ZonedDateTime.ofInstant(now, ZoneOffset.UTC).plusSeconds(timeShift);
			} else {
				this.timeStamp = ZonedDateTime.ofInstant(now, ZoneOffset.UTC).minusSeconds(Math.abs(timeShift));
			}
		}

		@Override
		public ZonedDateTime getNotifyStamp() {
			return this.timeStamp;
		}

		@Override
		public JsonObject getParams() {
			throw new UnsupportedOperationException();
		}
	}

	private static class DummyHandler implements Handler<DummyMessage> {
		@Override
		public Consumer<EventReader> getEventHandler(String eventTopic) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void stop() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void send(ZonedDateTime sentAt, List<DummyMessage> messages) {
		}

		@Override
		public Class<DummyMessage> getGeneric() {
			throw new UnsupportedOperationException();
		}
	}
}