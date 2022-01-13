package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonPrimitive;

public class AbstractOpenemsAppTest {

	@Test
	public void test() {
		assertTrue(AbstractOpenemsApp.equals(new JsonPrimitive(true), new JsonPrimitive(true)));
		assertTrue(AbstractOpenemsApp.equals(new JsonPrimitive(true), new JsonPrimitive("true")));
	}

}
