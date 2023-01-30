package io.openems.edge.heater.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HeaterStateTest {

    @Test
    public void testContainsById() throws Exception {
	assertTrue(HeaterState.contains(HeaterState.RUNNING.getValue()));
	assertFalse(HeaterState.contains(HeaterState.RUNNING.getValue() + 1));
    }

    @Test
    public void testContainsByString() throws Exception {
	assertTrue(HeaterState.contains(HeaterState.RUNNING.getName()));
	assertFalse(HeaterState.contains("Startup"));
    }

}
