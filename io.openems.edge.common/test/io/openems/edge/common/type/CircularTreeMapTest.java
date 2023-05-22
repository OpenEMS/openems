package io.openems.edge.common.type;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CircularTreeMapTest {

	@Test
	public void test() {
		var m = new CircularTreeMap<String, String>(3);
		m.put("1", "one");
		m.put("2", "two");
		m.put("3", "three");
		m.put("4", "four");

		var ks = m.keySet();
		var i = ks.iterator();
		assertEquals("2", i.next());
		assertEquals("3", i.next());
		assertEquals("4", i.next());
	}

	@Test
	public void testAnotherOrder() {
		var m = new CircularTreeMap<String, String>(3);
		m.put("4", "four");
		m.put("3", "three");
		m.put("2", "two");
		m.put("1", "one");

		var ks = m.keySet();
		var i = ks.iterator();
		assertEquals("2", i.next());
		assertEquals("3", i.next());
		assertEquals("4", i.next());
	}
}
