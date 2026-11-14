package io.openems.common.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class SingletonPoolTest {

	@Test
	void testLifecycle() {
		var pool = spy(new SingletonPoolImpl());

		var instance1 = pool.get("Hello");
		assertTrue(instance1.activated);
		verify(pool, times(1)).onPostLoadInstance(instance1);

		var instance2 = pool.get("Hello");
		assertEquals(instance1, instance2);
		assertTrue(instance1.activated);

		var instance3 = pool.get("World");
		assertNotEquals(instance1, instance3);
		assertArrayEquals(new SingletonPoolInstanceTest[] { instance1, instance3 },
				pool.getAllInstances().toArray(new SingletonPoolInstanceTest[0]));

		pool.unget(instance3);
		assertFalse(instance3.activated);
		verify(pool, times(1)).onPostUnloadInstance(instance3);

		pool.unget(instance2);
		assertTrue(instance2.activated);

		pool.unget(instance1);
		assertFalse(instance1.activated);
		verify(pool, times(1)).onPostUnloadInstance(instance1);
		verify(pool, times(2)).onPostUnloadInstance(any());
		verify(pool, times(1)).onPostLoadInstance(instance1);
		verify(pool, times(2)).onPostLoadInstance(any());

		var instance4 = pool.get("Hello");
		assertNotEquals(instance1, instance4);
	}

	@Test
	void testDispose() {
		var pool = spy(new SingletonPoolImpl());

		var instance1 = pool.get("Hello");
		assertTrue(instance1.activated);

		pool.dispose();

		assertFalse(instance1.activated);
		assertEquals(0, pool.getAllInstances().size());
	}

	static class SingletonPoolImpl extends SingletonPool<String, SingletonPoolInstanceTest> {

		@Override
		protected SingletonPoolInstanceTest createNewInstance(String config) {
			return new SingletonPoolInstanceTest();
		}

		@Override
		protected void onPostUnloadInstance(SingletonPoolInstanceTest instance) {
			System.out.println("DSSADSAD");
		}
	}

	static class SingletonPoolInstanceTest implements SingletonPool.SingletonPoolImplementation {

		private boolean activated = false;

		@Override
		public void activate() {
			this.activated = true;
		}

		@Override
		public void deactivate() {
			this.activated = false;
		}
	}

}
