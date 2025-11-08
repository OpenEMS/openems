package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.gson.JsonPrimitive;

import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.ComponentUtilImpl;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties.Property;

public class ComponentDefTest {

	@Test
	public void testFromEdgeConfig() {
		var ecc = new EdgeConfig.Component("id", "alias", "factory", JsonUtils.buildJsonObject() //
				.addProperty("test", "testValue")//
				.build());
		var cd = ComponentDef.from(ecc);

		assertTrue(ComponentUtilImpl.isSameConfiguration(null, cd, ecc));
	}

	@Test
	public void testToEdgeConfig() {
		var cd = new ComponentDef("id", "alias", "factory", new ComponentProperties(//
				List.of(new Property("test", new JsonPrimitive("testValue")))), null);
		var ecc = cd.toEdgeConfigComponent();

		assertTrue(ComponentUtilImpl.isSameConfiguration(null, cd, ecc));
	}

}
