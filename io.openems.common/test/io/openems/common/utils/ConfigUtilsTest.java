package io.openems.common.utils;

import org.junit.Assert;
import org.junit.Test;

public class ConfigUtilsTest {

	private static final String PID = "Abc.Def.12345";

	private static final String CTRL0_ID = "ctrl0";
	private static final String CTRL1_ID = "ctrl1";

	@Test
	public void testGenerateReferenceTargetFilter() {
		Assert.assertEquals(//
				"(&(enabled=true)(!(service.pid=" + ConfigUtilsTest.PID + "))(|(id=" + ConfigUtilsTest.CTRL0_ID + ")))", //
				ConfigUtils.generateReferenceTargetFilter(ConfigUtilsTest.PID, ConfigUtilsTest.CTRL0_ID));

		Assert.assertEquals(//
				"(&(enabled=true)(!(service.pid=" + ConfigUtilsTest.PID + "))(|(id=" + ConfigUtilsTest.CTRL0_ID
						+ ")(id=" + ConfigUtilsTest.CTRL1_ID + ")))", //
				ConfigUtils.generateReferenceTargetFilter(ConfigUtilsTest.PID, ConfigUtilsTest.CTRL0_ID,
						ConfigUtilsTest.CTRL1_ID));

		Assert.assertEquals(//
				"(&(enabled=true))", //
				ConfigUtils.generateReferenceTargetFilter(null));

		Assert.assertEquals(//
				"(&(enabled=true)(!(service.pid=" + ConfigUtilsTest.PID + ")))", //
				ConfigUtils.generateReferenceTargetFilter(ConfigUtilsTest.PID));

		Assert.assertEquals(//
				"(&(enabled=true)(|(id=" + ConfigUtilsTest.CTRL0_ID + ")))", //
				ConfigUtils.generateReferenceTargetFilter(null, ConfigUtilsTest.CTRL0_ID));
	}

}
