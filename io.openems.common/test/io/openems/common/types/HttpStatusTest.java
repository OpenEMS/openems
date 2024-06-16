package io.openems.common.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HttpStatusTest {

	@Test
	public void testIsStatusInformational() {
		assertFalse(HttpStatus.isStatusInformational(99));
		assertTrue(HttpStatus.isStatusInformational(100));
		assertFalse(HttpStatus.isStatusInformational(200));
		assertTrue(HttpStatus.CONTINUE.isInformational());
		assertFalse(HttpStatus.NOT_FOUND.isInformational());
	}

	@Test
	public void testIsStatusSuccessful() {
		assertFalse(HttpStatus.isStatusSuccessful(199));
		assertTrue(HttpStatus.isStatusSuccessful(200));
		assertFalse(HttpStatus.isStatusSuccessful(400));
		assertTrue(HttpStatus.OK.isSuccessful());
		assertFalse(HttpStatus.NOT_FOUND.isSuccessful());
	}

	@Test
	public void testIsStatusRedirection() {
		assertFalse(HttpStatus.isStatusRedirection(299));
		assertTrue(HttpStatus.isStatusRedirection(300));
		assertFalse(HttpStatus.isStatusRedirection(400));
		assertTrue(HttpStatus.MULTIPLE_CHOICES.isRedirection());
		assertFalse(HttpStatus.NOT_FOUND.isRedirection());
	}

	@Test
	public void testIsStatusClientError() {
		assertFalse(HttpStatus.isStatusClientError(399));
		assertTrue(HttpStatus.isStatusClientError(400));
		assertFalse(HttpStatus.isStatusClientError(500));
		assertTrue(HttpStatus.NOT_FOUND.isClientError());
		assertFalse(HttpStatus.MULTIPLE_CHOICES.isClientError());
	}

	@Test
	public void testIsStatusServerError() {
		assertFalse(HttpStatus.isStatusServerError(499));
		assertTrue(HttpStatus.isStatusServerError(500));
		assertFalse(HttpStatus.isStatusServerError(600));
		assertTrue(HttpStatus.INTERNAL_SERVER_ERROR.isServerError());
		assertFalse(HttpStatus.NOT_FOUND.isServerError());
	}

	@Test
	public void testIsStatusError() {
		assertTrue(HttpStatus.isStatusError(400));
		assertTrue(HttpStatus.isStatusError(500));
		assertFalse(HttpStatus.isStatusError(200));
		assertTrue(HttpStatus.NOT_FOUND.isError());
		assertTrue(HttpStatus.INTERNAL_SERVER_ERROR.isError());
		assertFalse(HttpStatus.OK.isError());
	}

	@Test
	public void testFromCodeOrNull() {
		assertEquals(HttpStatus.OK, HttpStatus.fromCodeOrNull(HttpStatus.OK.code()));
		assertNull(HttpStatus.fromCodeOrNull(666));
	}

	@Test
	public void testFromCodeOrCustom() {
		assertEquals(HttpStatus.OK, HttpStatus.fromCodeOrCustom(HttpStatus.OK.code(), HttpStatus.OK.description()));
		assertEquals(new HttpStatus(666, "custom description"), HttpStatus.fromCodeOrCustom(666, "custom description"));
	}

	@Test
	public void testToString() {
		assertEquals("200 OK", HttpStatus.OK.toString());
	}

}
