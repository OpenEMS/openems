package io.openems.common.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.utils.JsonUtils;

public class EdgeConfigDiffTest {

	@Test
	public void testNotDifferent() {
		var config1 = EdgeConfig.empty();
		var config2 = EdgeConfig.empty();
		var diff = EdgeConfigDiff.diff(config1, config2);
		assertFalse(diff.isDifferent());
	}

	@Test
	public void testDifferent() {
		var config1 = new EdgeConfig.ActualEdgeConfig.Builder() //
				.addComponent("foo0", new EdgeConfig.Component("foo0", "", "Component.Foo", JsonUtils.buildJsonObject() //
						.add("ip", null) //
						.build())) //
				.buildEdgeConfig();
		var config2 = EdgeConfig.empty();
		var diff = EdgeConfigDiff.diff(config1, config2);
		assertTrue(diff.isDifferent());
	}

}
