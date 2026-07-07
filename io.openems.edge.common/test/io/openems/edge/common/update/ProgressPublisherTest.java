package io.openems.edge.common.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

public class ProgressPublisherTest {

	@Test
	public void testFori() {
		final var history = new ProgressHistory();
		final var progress = new ProgressPublisher();
		progress.addListener(history::addProgress);
		final var iterator = progress.fori(0, 50, 5, "Progress").iterator();

		assertTrue(iterator.hasNext());
		assertEquals((Integer) 0, iterator.next());
		assertEquals(10, history.last().percentage());
		assertTrue(iterator.hasNext());
		assertEquals((Integer) 5, iterator.next());
		assertEquals(20, history.last().percentage());
		assertTrue(iterator.hasNext());
		assertEquals((Integer) 10, iterator.next());
		assertEquals(30, history.last().percentage());
		assertTrue(iterator.hasNext());
		assertEquals((Integer) 15, iterator.next());
		assertEquals(40, history.last().percentage());
		assertTrue(iterator.hasNext());
		assertEquals((Integer) 20, iterator.next());
		assertEquals(50, history.last().percentage());
		assertTrue(iterator.hasNext());
		assertEquals((Integer) 25, iterator.next());
		assertEquals(60, history.last().percentage());
		assertTrue(iterator.hasNext());
		assertEquals((Integer) 30, iterator.next());
		assertEquals(70, history.last().percentage());
		assertTrue(iterator.hasNext());
		assertEquals((Integer) 35, iterator.next());
		assertEquals(80, history.last().percentage());
		assertTrue(iterator.hasNext());
		assertEquals((Integer) 40, iterator.next());
		assertEquals(90, history.last().percentage());
		assertTrue(iterator.hasNext());
		assertEquals((Integer) 45, iterator.next());
		assertEquals(100, history.last().percentage());
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testSubProgress() {
		final var history = new ProgressHistory();
		final var progress = new ProgressPublisher();
		progress.addListener(history::addProgress);

		final var subProgress = progress.subProgress(30, 50);
		subProgress.setPercentage(0);
		assertEquals(30, history.last().percentage());
		subProgress.setPercentage(50);
		assertEquals(40, history.last().percentage());
		subProgress.setPercentage(100);
		assertEquals(50, history.last().percentage());
	}

	@Test
	public void testOutOfRange() {
		final var progress = new ProgressPublisher();
		assertThrows(IllegalArgumentException.class, () -> {
			progress.setPercentage(-1);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			progress.setPercentage(101);
		});
	}

	@Test
	@Ignore
	public void testSleep() throws Exception {
		final var progress = new ProgressPublisher();
		progress.addListener(value -> {
			System.out.println("Progress: " + value);
		});

		final var start = System.currentTimeMillis();
		progress.sleep(5000, 0, 100, "Test");
		final var end = System.currentTimeMillis();

		System.out.println("Took: " + (end - start));
	}

}
