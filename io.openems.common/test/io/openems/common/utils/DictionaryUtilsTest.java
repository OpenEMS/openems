package io.openems.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import org.junit.Test;

public class DictionaryUtilsTest {

	@Test
	public void testGetAsInteger() {
		final var dict = new Hashtable<String, Object>();
		dict.put("1", 1);
		dict.put("2", 2.4);
		dict.put("3", Long.MAX_VALUE);
		dict.put("4", Long.MIN_VALUE);
		dict.put("5", "null");
		dict.put("6", "1030");
		dict.put("7", true);
		dict.put("8", false);
		dict.put("9", "");
		dict.put("10", new Hashtable<Object, Object>());
		
		assertEquals(1, (int) DictionaryUtils.getAsInteger(dict, "1"));
		assertEquals(2, (int) DictionaryUtils.getAsInteger(dict, "2"));
		assertEquals(Integer.MAX_VALUE, (int) DictionaryUtils.getAsInteger(dict, "3"));
		assertEquals(Integer.MIN_VALUE, (int) DictionaryUtils.getAsInteger(dict, "4"));
		assertNull(DictionaryUtils.getAsInteger(dict, "5"));
		assertEquals(1030, (int) DictionaryUtils.getAsInteger(dict, "6"));
		assertEquals(1, (int) DictionaryUtils.getAsInteger(dict, "7"));
		assertEquals(0, (int) DictionaryUtils.getAsInteger(dict, "8"));
		assertNull(DictionaryUtils.getAsInteger(dict, "9"));
		assertNull(DictionaryUtils.getAsInteger(dict, "10"));
		
		assertTrue(DictionaryUtils.getAsOptionalInteger(dict, "1").isPresent());
		assertTrue(DictionaryUtils.getAsOptionalInteger(dict, "10").isEmpty());
		assertTrue(DictionaryUtils.getAsOptionalInteger(dict, "100").isEmpty());
	}
	
	@Test
	public void testGetAsBoolean() {
		final var dict = new Hashtable<String, Object>();
		dict.put("1", 1);
		dict.put("2", 0);
		dict.put("3", true);
		dict.put("4", false);
		dict.put("5", "True");
		dict.put("6", "FALSE");
		dict.put("7", "error");
		dict.put("8", "");
		dict.put("9", new Hashtable<Object, Object>());
		
		assertEquals(true, DictionaryUtils.getAsBoolean(dict, "1"));
		assertEquals(false, DictionaryUtils.getAsBoolean(dict, "2"));
		assertEquals(true, DictionaryUtils.getAsBoolean(dict, "3"));
		assertEquals(false, DictionaryUtils.getAsBoolean(dict, "4"));
		assertEquals(true, DictionaryUtils.getAsBoolean(dict, "5"));
		assertEquals(false, DictionaryUtils.getAsBoolean(dict, "6"));
		assertNull(DictionaryUtils.getAsBoolean(dict, "7"));
		assertNull(DictionaryUtils.getAsBoolean(dict, "8"));
		assertNull(DictionaryUtils.getAsBoolean(dict, "9"));
		
		assertTrue(DictionaryUtils.getAsOptionalBoolean(dict, "1").isPresent());
		assertTrue(DictionaryUtils.getAsOptionalBoolean(dict, "9").isEmpty());
		assertTrue(DictionaryUtils.getAsOptionalBoolean(dict, "100").isEmpty());
	}
	
	@Test
	public void testGetAsString() {
		final var dict = new Hashtable<String, Object>();
		dict.put("1", 1);
		dict.put("2", 0.76);
		dict.put("3", true);
		dict.put("4", false);
		dict.put("5", "test");
		
		assertEquals("1", DictionaryUtils.getAsString(dict, "1"));
		assertEquals("0.76", DictionaryUtils.getAsString(dict, "2"));
		assertEquals("true", DictionaryUtils.getAsString(dict, "3"));
		assertEquals("false", DictionaryUtils.getAsString(dict, "4"));
		assertEquals("test", DictionaryUtils.getAsString(dict, "5"));
		
		assertTrue(DictionaryUtils.getAsOptionalString(dict, "1").isPresent());
		assertTrue(DictionaryUtils.getAsOptionalString(dict, "100").isEmpty());
	}
	
	@Test
	public void testContainsAnyKey() {
		final var empty = new Hashtable<String, Object>();
		final var dict = new Hashtable<String, Object>();
		dict.put("1", 1);
		dict.put("2", 0.76);
		
		assertTrue(DictionaryUtils.containsAnyKey(dict, "1", "2"));
		assertTrue(DictionaryUtils.containsAnyKey(dict, "1"));
		assertTrue(DictionaryUtils.containsAnyKey(dict, "1", "no"));
		assertTrue(DictionaryUtils.containsAnyKey(dict, "no", "2"));
		assertFalse(DictionaryUtils.containsAnyKey(dict, "no"));
		assertFalse(DictionaryUtils.containsAnyKey(dict, new String[] {}));
		assertFalse(DictionaryUtils.containsAnyKey(null, "1"));
		assertFalse(DictionaryUtils.containsAnyKey(empty, new String[] {}));
		assertFalse(DictionaryUtils.containsAnyKey(empty, "1"));
	}

}
