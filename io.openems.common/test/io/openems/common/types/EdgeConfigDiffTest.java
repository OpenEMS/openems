package io.openems.common.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.utils.JsonUtils;

public class EdgeConfigDiffTest {

	@Test
	public void testNotDifferent() {
		var config1 = new EdgeConfig();
		var config2 = new EdgeConfig();
		var diff = EdgeConfigDiff.diff(config1, config2);
		assertFalse(diff.isDifferent());
	}

	@Test
	public void testDifferent() {
		var config1 = new EdgeConfig();
		{
			var component = new EdgeConfig.Component("foo0", "", "Component.Foo", JsonUtils.buildJsonObject() //
					.add("ip", null) //
					.build());
			config1.addComponent("foo0", component);
		}
		var config2 = new EdgeConfig();
		var diff = EdgeConfigDiff.diff(config1, config2);
		assertTrue(diff.isDifferent());
	}

}
