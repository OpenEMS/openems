package io.openems.backend.alerting;

import java.time.ZonedDateTime;

import io.openems.backend.common.mail.MailContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MessageTest {

	@Test
	void testMessage() {
		final var now = ZonedDateTime.now();
		final var msg10 = new DummmyMessage("1", now);
		final var msg20 = new DummmyMessage("2", now.plusMinutes(1));
		final var msg11 = new DummmyMessage("1", now.minusMinutes(1));

		assertEquals("1", msg10.getId());
		assertEquals("2", msg20.getId());
		assertEquals("1", msg11.getId());

		assertEquals(msg10, msg11);
		assertNotEquals(msg10, msg20);
		assertNotNull(msg10);

		assertEquals(msg10.hashCode(), msg11.hashCode());
		assertNotEquals(msg10.hashCode(), msg20.hashCode());

		assertTrue(msg10.compareTo(msg11) > 0, "msg10 should be greater than msg11");
		assertTrue(msg10.compareTo(msg20) < 0, "msg10 should be lower than msg20");

		assertTrue(msg10.compareTo(null) > 0, "msg10 should be greater than null");
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
		public MailContext getContext() {
			throw new UnsupportedOperationException();
		}

	}
}
