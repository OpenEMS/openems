package io.openems.backend.alerting.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.Message;
import io.openems.common.event.EventReader;

public class SchedulerTest {

	/**
	 * Validate dummy methods.
	 */
	@BeforeClass
	public static void dummyClass() {
		var dummyMsg = new DummyMessage("", 0);
		var dummyHan = new DummyHandler();
		Runnable[] methods = { () -> dummyHan.handleEvent(null), () -> dummyHan.stop(), () -> dummyHan.getGeneric(),
				() -> dummyMsg.getParams(), };
		assertNotNull(dummyMsg.getNotifyStamp());
		for (var method : methods) {
			assertThrows(UnsupportedOperationException.class, () -> method.run());
		}
	}

	private Scheduler scheduler;
	private final List<DummyHandler> handler = new ArrayList<>();
	private final List<DummyMessage> msgs = new ArrayList<>();
	private final List<MessageScheduler<DummyMessage>> msgScheduler = new ArrayList<>();

	@Test
	public void testFunctionaliy() {
		this.prepare();
		this.testSchedule();
		this.testRemove();
		this.testUnRegister();
		this.testHandle();
	}

	private void prepare() {
		this.scheduler = new Scheduler();
		// Handler
		this.handler.add(new DummyHandler());
		this.handler.add(new DummyHandler());
		// Messages
		this.msgs.add(new DummyMessage("1", -1));
		this.msgs.add(new DummyMessage("2", 1));
		this.msgs.add(new DummyMessage("3", -2));
		this.msgs.add(new DummyMessage("4", 2));
		// MsgScheduler
		this.msgScheduler.add(this.scheduler.register(this.handler.get(0)));
		this.msgScheduler.add(this.scheduler.register(this.handler.get(0)));
		this.msgScheduler.add(this.scheduler.register(this.handler.get(1)));
		// Check
		assertTrue(this.msgScheduler.get(0).isFor(this.handler.get(0)));
		assertTrue(this.msgScheduler.get(1).isFor(this.handler.get(0)));
		assertTrue(this.msgScheduler.get(2).isFor(this.handler.get(1)));
		assertFalse(this.msgScheduler.get(0).isFor(this.handler.get(1)));
	}

	private void testSchedule() {
		this.msgScheduler.get(0).schedule(this.msgs.get(0));

		assertTrue(this.scheduler.isScheduled(this.msgs.get(0)));
		assertFalse(this.scheduler.isScheduled(this.msgs.get(1)));
		assertFalse(this.scheduler.isScheduled(this.msgs.get(2)));

		this.msgScheduler.get(1).schedule(this.msgs.get(1));
		this.msgScheduler.get(2).schedule(this.msgs.get(2));

		assertTrue(this.scheduler.isScheduled(this.msgs.get(0)));
		assertTrue(this.scheduler.isScheduled(this.msgs.get(1)));
		assertTrue(this.scheduler.isScheduled(this.msgs.get(2)));
	}

	private void testRemove() {
		assertTrue(this.scheduler.isScheduled(this.msgs.get(0)));
		assertTrue(this.scheduler.isScheduled(this.msgs.get(1)));
		assertTrue(this.scheduler.isScheduled(this.msgs.get(2)));

		this.msgScheduler.get(1).remove(null);
		this.msgScheduler.get(1).remove(this.msgs.get(1).getId());

		assertTrue(this.scheduler.isScheduled(this.msgs.get(0)));
		assertFalse(this.scheduler.isScheduled(this.msgs.get(1)));
		assertTrue(this.scheduler.isScheduled(this.msgs.get(2)));
	}

	private void testUnRegister() {
		this.scheduler.unregister(this.handler.get(0));

		assertFalse(this.scheduler.isScheduled(this.msgs.get(0)));
		assertTrue(this.scheduler.isScheduled(this.msgs.get(2)));
	}

	private void testHandle() {
		this.msgScheduler.get(2).schedule(this.msgs.get(3));
		assertTrue(this.scheduler.isScheduled(this.msgs.get(2)));
		assertTrue(this.scheduler.isScheduled(this.msgs.get(3)));

		this.msgScheduler.get(2).handle();

		assertFalse(this.scheduler.isScheduled(this.msgs.get(2)));
		assertTrue(this.scheduler.isScheduled(this.msgs.get(3)));
	}

	/* *********************************************** */
	static class DummyMessage extends Message {
		ZonedDateTime timeStamp;

		public DummyMessage(String messageId, int timeShift) {
			super(messageId);
			if (timeShift >= 0) {
				this.timeStamp = ZonedDateTime.now().plusSeconds(timeShift);
			} else {
				this.timeStamp = ZonedDateTime.now().minusSeconds(Math.abs(timeShift));
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

	static class DummyHandler implements Handler<DummyMessage> {
		ZonedDateTime wasSentAt = null;

		@Override
		public void handleEvent(EventReader event) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void stop() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void send(ZonedDateTime sentAt, List<DummyMessage> messages) {
			this.wasSentAt = sentAt;
		}

		@Override
		public Class<DummyMessage> getGeneric() {
			throw new UnsupportedOperationException();
		}
	}
}