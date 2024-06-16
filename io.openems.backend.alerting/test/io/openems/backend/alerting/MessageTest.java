package io.openems.backend.alerting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.gson.JsonObject;

public class MessageTest {

	@Test
	public void testMessage() {
		final var now = ZonedDateTime.now();
		final var msg10 = new DummmyMessage("1", now);
		final var msg20 = new DummmyMessage("2", now.plusMinutes(1));
		final var msg11 = new DummmyMessage("1", now.minusMinutes(1));

		assertEquals("1", msg10.getId());
		assertEquals("2", msg20.getId());
		assertEquals("1", msg11.getId());

		assertEquals(msg10, msg10);
		assertEquals(msg10, msg11);
		assertNotEquals(msg10, msg20);
		assertNotEquals(msg10, null);
		assertNotEquals(msg10, "1");

		assertEquals(msg10.hashCode(), msg11.hashCode());
		assertNotEquals(msg10.hashCode(), msg20.hashCode());
		assertNotEquals(msg10.hashCode(), null);

		assertTrue("msg10 should be greater than msg11", msg10.compareTo(msg11) > 0);
		assertTrue("msg10 should be lower than msg20", msg10.compareTo(msg20) < 0);
		
		assertTrue("msg10 should be greater than null", msg10.compareTo(null) > 0);
	}

	/* *********************************************** */
	static class DummmyMessage extends Message {
		private final ZonedDateTime notifyStamp;

		public DummmyMessage(String messageId, ZonedDateTime notifyStamp) {
			super(messageId);
			this.notifyStamp = notifyStamp;
		}

		@Override
		public ZonedDateTime getNotifyStamp() {
			return this.notifyStamp;
		}

		@Override
		public JsonObject getParams() {
			throw new UnsupportedOperationException();
		}

	}
}
