package io.openems.backend.timedata.influx;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FieldTypeConflictHandlerTest {

	@Test
	public void testHandleExceptionMessage() {
		var influx = new InfluxImpl();
		var sut = new FieldTypeConflictHandler(influx);
		assertTrue(sut.handleExceptionMessage("""
			HTTP status code: 400; Message: partial write: field type conflict: \
			input field "foo/bar" on measurement "data" is type integer, \
			already exists as type float dropped=2"""));
	}

}
