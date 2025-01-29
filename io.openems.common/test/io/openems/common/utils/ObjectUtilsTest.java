package io.openems.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Optional;

import org.junit.Test;

public class ObjectUtilsTest {

	@Test
	public void testGetAsString() {
		assertNull(ObjectUtils.getAsString(null));
		assertNull(ObjectUtils.getAsString(new Object()));
		assertEquals("string", ObjectUtils.getAsString("string"));
	}

	@Test
	public void testGetAsOptionalString() {
		assertEquals(Optional.empty(), ObjectUtils.getAsOptionalString(null));
		assertEquals(Optional.empty(), ObjectUtils.getAsOptionalString(new Object()));
		assertEquals(Optional.of("string"), ObjectUtils.getAsOptionalString("string"));
	}

	@Test
	public void testGetAsInteger() {
		assertNull(ObjectUtils.getAsInteger(null));
		assertNull(ObjectUtils.getAsInteger(new Object()));
		assertEquals((Integer) 1337, ObjectUtils.getAsInteger(1337));
	}

	@Test
	public void testGetAsObjectArrray() {
		assertNull(ObjectUtils.getAsObjectArrray(null));
		assertNull(ObjectUtils.getAsObjectArrray(new Object()));
		final var array = new Object[] {};
		assertArrayEquals(array, ObjectUtils.getAsObjectArrray(array));
	}

}
