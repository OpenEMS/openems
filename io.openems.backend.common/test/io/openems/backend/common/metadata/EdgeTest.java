package io.openems.backend.common.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.openems.backend.common.test.DummyMetadata;
import io.openems.common.types.SemanticVersion;

public class EdgeTest {

	@Test
	public void testSetLastmessage() {
		AtomicBoolean event = new AtomicBoolean(false);
		var metadata = new DummyMetadata(e -> e.getTopic() == Edge.Events.ON_SET_LASTMESSAGE, e -> event.set(true));

		var time = ZonedDateTime.of(2023, 1, 2, 3, 4, 5, 6, ZoneId.of("UTC"));

		var sut = new Edge(metadata, null, null, null, null, time);

		// Initial, no event
		assertEquals(ZonedDateTime.of(2023, 1, 2, 3, 4, 0, 0, ZoneId.of("UTC")), sut.getLastmessage());

		// Lastmessage unchanged; no event
		time = ZonedDateTime.of(2023, 1, 2, 3, 4, 8, 9, ZoneId.of("UTC"));
		assertFalse(event.get());
		assertEquals(ZonedDateTime.of(2023, 1, 2, 3, 4, 0, 0, ZoneId.of("UTC")), sut.getLastmessage());

		// Lastmessage earlier; no event
		time = ZonedDateTime.of(2023, 1, 1, 1, 1, 1, 1, ZoneId.of("UTC"));
		assertFalse(event.get());
		assertEquals(ZonedDateTime.of(2023, 1, 2, 3, 4, 0, 0, ZoneId.of("UTC")), sut.getLastmessage());

		// Lastmessage changed + event
		sut.setLastmessage(ZonedDateTime.of(2023, 1, 2, 3, 5, 8, 9, ZoneId.of("UTC")));
		assertTrue(event.getAndSet(false));
		assertEquals(ZonedDateTime.of(2023, 1, 2, 3, 5, 0, 0, ZoneId.of("UTC")), sut.getLastmessage());
	}

	@Test
	public void testSetVersion() {
		AtomicBoolean event = new AtomicBoolean(false);
		var metadata = new DummyMetadata(e -> e.getTopic() == Edge.Events.ON_SET_VERSION, e -> event.set(true));

		var sut = new Edge(metadata, null, null, null, null, null);

		// Initial, no event
		assertFalse(event.get());
		assertEquals("0.0.0", sut.getVersion().toString());

		// Unchanged, no event
		sut.setVersion(null);
		assertFalse(event.get());

		// Version changed + event
		sut.setVersion(new SemanticVersion(1, 2, 3));
		assertTrue(event.getAndSet(false));
		assertEquals("1.2.3", sut.getVersion().toString());
	}

	@Test
	public void testSetProducttype() {
		AtomicBoolean event = new AtomicBoolean(false);
		var metadata = new DummyMetadata(e -> e.getTopic() == Edge.Events.ON_SET_PRODUCTTYPE, e -> event.set(true));

		var sut = new Edge(metadata, null, null, null, null, null);

		// Initial, no event
		assertFalse(event.get());
		assertEquals("", sut.getProducttype().toString());

		// Unchanged, no event
		sut.setProducttype(null);
		assertFalse(event.get());

		// Changed + event
		sut.setProducttype("HW01A");
		assertTrue(event.getAndSet(false));
		assertEquals("HW01A", sut.getProducttype().toString());
	}

}
