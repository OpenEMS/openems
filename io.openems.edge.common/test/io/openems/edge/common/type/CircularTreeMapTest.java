package io.openems.edge.common.type;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

public class CircularTreeMapTest {

	@Test
	public void test() {
		CircularTreeMap<String, String> m = new CircularTreeMap<>(3);
		m.put("1", "one");
		m.put("2", "two");
		m.put("3", "three");
		m.put("4", "four");

		Set<String> ks = m.keySet();
		Iterator<String> i = ks.iterator();
		assertEquals("2", i.next());
		assertEquals("3", i.next());
		assertEquals("4", i.next());
	}

	@Test
	public void testAnotherOrder() {
		CircularTreeMap<String, String> m = new CircularTreeMap<>(3);
		m.put("4", "four");
		m.put("3", "three");
		m.put("2", "two");
		m.put("1", "one");
		
		Set<String> ks = m.keySet();
		Iterator<String> i = ks.iterator();
		assertEquals("2", i.next());
		assertEquals("3", i.next());
		assertEquals("4", i.next());
	}
}
