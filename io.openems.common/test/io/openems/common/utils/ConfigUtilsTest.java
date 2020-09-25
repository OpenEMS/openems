package io.openems.common.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConfigUtilsTest {

	private static final String PID = "Abc.Def.12345";

	private static final String CTRL0_ID = "ctrl0";
	private static final String CTRL1_ID = "ctrl1";

	@Test
	public void testGenerateReferenceTargetFilter() {
		System.out.println("(&(enabled=true)(!(service.pid=" + PID + "))(|(id=" + CTRL0_ID + ")))");
		System.out.println(ConfigUtils.generateReferenceTargetFilter(PID, CTRL0_ID));
		assertEquals(//
				"(&(enabled=true)(!(service.pid=" + PID + "))(|(id=" + CTRL0_ID + ")))", //
				ConfigUtils.generateReferenceTargetFilter(PID, CTRL0_ID));

		assertEquals(//
				"(&(enabled=true)(!(service.pid=" + PID + "))(|(id=" + CTRL0_ID + ")(id=" + CTRL1_ID + ")))", //
				ConfigUtils.generateReferenceTargetFilter(PID, CTRL0_ID, CTRL1_ID));

		assertEquals(//
				"(&(enabled=true))", //
				ConfigUtils.generateReferenceTargetFilter(null));

		assertEquals(//
				"(&(enabled=true)(!(service.pid=" + PID + ")))", //
				ConfigUtils.generateReferenceTargetFilter(PID));

		assertEquals(//
				"(&(enabled=true)(|(id=" + CTRL0_ID + ")))", //
				ConfigUtils.generateReferenceTargetFilter(null, CTRL0_ID));
	}

}
