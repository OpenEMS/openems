package io.openems.backend.timedata.timescaledb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class TypeTest {

	@Test
	public void testDetect() throws OpenemsNamedException {
		assertEquals(Type.INTEGER, Type.detect(new JsonPrimitive("101180500005")));
	}

}
