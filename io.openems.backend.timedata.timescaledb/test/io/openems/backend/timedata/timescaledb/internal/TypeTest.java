package io.openems.backend.timedata.timescaledb.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;

public class TypeTest {

	@Test
	public void testDetect() throws OpenemsNamedException {
		{
			var j = new JsonPrimitive("101180500");
			assertEquals(Type.INTEGER, Type.detect(j));
			assertEquals((Integer) 101180500, JsonUtils.getAsType(OpenemsType.INTEGER, j));
		}
		{
			var j = new JsonPrimitive("101180500005");
			assertEquals(Type.LONG, Type.detect(j));
			assertEquals((Long) 101180500005L, JsonUtils.getAsType(OpenemsType.LONG, j));
		}
		{
			var j = new JsonPrimitive("519100001009210611000019");
			assertEquals(Type.STRING, Type.detect(j));
			assertEquals(j.getAsString(), JsonUtils.getAsType(OpenemsType.STRING, j));
		}
	}

}
