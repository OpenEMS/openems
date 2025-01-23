package io.openems.backend.timedata.influx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class FieldTypeConflictHandlerTest {

	@Test
	public void testHandleExceptionMessage() {
		var influx = new TimedataInfluxDb();
		var sut = new FieldTypeConflictHandler(influx);
		assertTrue(sut.handleExceptionMessage("""
				HTTP status code: 400; Message: partial write: field type conflict: \
				input field "foo/bar" on measurement "data" is type integer, \
				already exists as type float dropped=2"""));
	}

	@Test
	public void testGetAsFieldTypeString() throws Exception {
		assertEquals("123", FieldTypeConflictHandler.getAsFieldTypeString(new JsonPrimitive(123)));
		assertEquals("123", FieldTypeConflictHandler.getAsFieldTypeString(new JsonPrimitive("123")));
		assertEquals("123.5", FieldTypeConflictHandler.getAsFieldTypeString(new JsonPrimitive(123.5)));
		assertEquals("123.5", FieldTypeConflictHandler.getAsFieldTypeString(new JsonPrimitive("123.5")));
		assertEquals("string", FieldTypeConflictHandler.getAsFieldTypeString(new JsonPrimitive("string")));
		assertEquals("{}", FieldTypeConflictHandler.getAsFieldTypeString(new JsonObject()));
		assertNull(FieldTypeConflictHandler.getAsFieldTypeString(JsonNull.INSTANCE));
		assertEquals("true", FieldTypeConflictHandler.getAsFieldTypeString(new JsonPrimitive(true)));
		assertEquals("true", FieldTypeConflictHandler.getAsFieldTypeString(new JsonPrimitive("true")));
		assertEquals("false", FieldTypeConflictHandler.getAsFieldTypeString(new JsonPrimitive(false)));
		assertEquals("false", FieldTypeConflictHandler.getAsFieldTypeString(new JsonPrimitive("false")));
	}

	@Test
	public void testGetAsFieldTypeLong() throws Exception {
		assertEquals((Long) 123L, FieldTypeConflictHandler.getAsFieldTypeLong(new JsonPrimitive(123)));
		assertEquals((Long) 123L, FieldTypeConflictHandler.getAsFieldTypeLong(new JsonPrimitive("123")));
		assertEquals((Long) 123L, FieldTypeConflictHandler.getAsFieldTypeLong(new JsonPrimitive(123.5)));
		assertEquals((Long) 123L, FieldTypeConflictHandler.getAsFieldTypeLong(new JsonPrimitive("123.5")));
		assertNull(FieldTypeConflictHandler.getAsFieldTypeLong(new JsonPrimitive("string")));
		assertNull(FieldTypeConflictHandler.getAsFieldTypeLong(new JsonObject()));
		assertNull(FieldTypeConflictHandler.getAsFieldTypeLong(JsonNull.INSTANCE));
		assertEquals((Long) 1L, FieldTypeConflictHandler.getAsFieldTypeLong(new JsonPrimitive(true)));
		assertEquals((Long) 1L, FieldTypeConflictHandler.getAsFieldTypeLong(new JsonPrimitive("true")));
		assertEquals((Long) 0L, FieldTypeConflictHandler.getAsFieldTypeLong(new JsonPrimitive(false)));
		assertEquals((Long) 0L, FieldTypeConflictHandler.getAsFieldTypeLong(new JsonPrimitive("false")));
	}

	@Test
	public void testGetAsFieldTypeDouble() throws Exception {
		assertEquals((Double) 123D, FieldTypeConflictHandler.getAsFieldTypeDouble(new JsonPrimitive(123)));
		assertEquals((Double) 123D, FieldTypeConflictHandler.getAsFieldTypeDouble(new JsonPrimitive("123")));
		assertEquals((Double) 123.5, FieldTypeConflictHandler.getAsFieldTypeDouble(new JsonPrimitive(123.5)));
		assertEquals((Double) 123.5, FieldTypeConflictHandler.getAsFieldTypeDouble(new JsonPrimitive("123.5")));
		assertNull(FieldTypeConflictHandler.getAsFieldTypeDouble(new JsonPrimitive("string")));
		assertNull(FieldTypeConflictHandler.getAsFieldTypeDouble(new JsonObject()));
		assertNull(FieldTypeConflictHandler.getAsFieldTypeDouble(JsonNull.INSTANCE));
		assertEquals((Double) 1D, FieldTypeConflictHandler.getAsFieldTypeDouble(new JsonPrimitive(true)));
		assertEquals((Double) 1D, FieldTypeConflictHandler.getAsFieldTypeDouble(new JsonPrimitive("true")));
		assertEquals((Double) 0D, FieldTypeConflictHandler.getAsFieldTypeDouble(new JsonPrimitive(false)));
		assertEquals((Double) 0D, FieldTypeConflictHandler.getAsFieldTypeDouble(new JsonPrimitive("false")));
	}

}
