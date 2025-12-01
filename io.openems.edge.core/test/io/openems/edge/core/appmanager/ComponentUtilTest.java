package io.openems.edge.core.appmanager;

import static io.openems.common.utils.JsonUtils.toJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.ConfigurationProperty;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef.Configuration;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.core.host.Inet4AddressWithSubnetmask;
import io.openems.edge.core.host.NetworkInterface;

public class ComponentUtilTest {

	@Test
	public void testEqualsRecursion() {
		assertFalse(ComponentUtilImpl.equals(new JsonPrimitive("abc"), new JsonObject()));
		assertFalse(ComponentUtilImpl.equals(new JsonObject(), new JsonPrimitive("abc")));
	}

	@Test
	public void testEqualsJsonElementJsonElement() {
		// Equal complex JSON objects
		var expected = JsonUtils.buildJsonObject() //
				.addProperty("text", "text") //
				.addProperty("boolean", false) //
				.addProperty("number", 4.124) //
				.add("array", JsonUtils.buildJsonArray() //
						.add("a") //
						.add("b") //
						.build())
				.add("object", JsonUtils.buildJsonObject() //
						.addProperty("c", false) //
						.build())
				.build();
		var actual = JsonUtils.buildJsonObject() //
				.addProperty("text", "text") //
				.addProperty("boolean", false) //
				.addProperty("number", 4.124) //
				.add("array", JsonUtils.buildJsonArray() //
						.add("a") //
						.add("b") //
						.build())
				.add("object", JsonUtils.buildJsonObject() //
						.addProperty("c", false) //
						.build())
				.build();
		assertTrue(ComponentUtilImpl.equals(expected, actual));

		// null
		assertFalse(ComponentUtilImpl.equals(null, toJson("test")));

		// Equal primitive values
		assertTrue(ComponentUtilImpl.equals(toJson("test"), toJson("test")));
		assertTrue(ComponentUtilImpl.equals(toJson(123), toJson(123)));
		assertTrue(ComponentUtilImpl.equals(toJson(true), toJson(true)));

		// Different primitive values
		assertFalse(ComponentUtilImpl.equals(toJson("test"), toJson("test2")));
		assertFalse(ComponentUtilImpl.equals(toJson(123), toJson(124)));
		assertFalse(ComponentUtilImpl.equals(toJson(true), toJson(false)));

		var jsonString = "{\"dso\":\"OTHER\"}";
		var jsonObject = JsonUtils.buildJsonObject() //
				.addProperty("dso", "OTHER") //
				.build();

		var expectedAsString = toJson(jsonString);
		assertTrue(ComponentUtilImpl.equals(expectedAsString, jsonObject));

		// Actual is JSON string that can be parsed as JSON, expected is JSON object
		var actualAsString = toJson(jsonString);
		assertTrue(ComponentUtilImpl.equals(jsonObject, actualAsString));

		// JSON string that cannot be parsed as JSON
		var invalidJsonString = toJson("{invalid json}");
		assertFalse(ComponentUtilImpl.equals(invalidJsonString, jsonObject));
		assertFalse(ComponentUtilImpl.equals(jsonObject, invalidJsonString));

		// Both are JSON strings containing JSON (should compare as strings, not parse)
		var jsonString1 = toJson("{\"a\": 1}");
		var jsonString2 = toJson("{\"a\": 1}");
		var jsonString3 = toJson("{\"a\": 2}");
		assertTrue(ComponentUtilImpl.equals(jsonString1, jsonString2));
		assertFalse(ComponentUtilImpl.equals(jsonString1, jsonString3));

		// Different JSON structures
		var obj1 = JsonUtils.buildJsonObject() //
				.addProperty("a", 1) //
				.build();
		var obj2 = JsonUtils.buildJsonArray() //
				.add(1) //
				.build();
		assertFalse(ComponentUtilImpl.equals(obj1, obj2));

		// Different objects with same content but different structure
		var simpleObj = JsonUtils.buildJsonObject() //
				.addProperty("a", 1) //
				.build();
		var nestedObj = JsonUtils.buildJsonObject() //
				.add("a", toJson(1)) //
				.build();
		assertTrue(ComponentUtilImpl.equals(simpleObj, nestedObj));

		// Boolean as string vs actual boolean
		var boolTrue = toJson(true);
		var stringTrue = toJson("true");
		assertTrue(ComponentUtilImpl.equals(boolTrue, stringTrue));
	}

	@Test
	public void testEqualsListOfNetworkInterface() throws Exception {
		var expectedInet4Addresses = new HashSet<Inet4AddressWithSubnetmask>();
		expectedInet4Addresses.add(Inet4AddressWithSubnetmask.fromString("foo", "192.168.178.2/24"));
		List<NetworkInterface<?>> expected = Lists
				.newArrayList(new NetworkInterface<Void>("eth0", ConfigurationProperty.of(false),
						ConfigurationProperty.of(false), ConfigurationProperty.asNull(), ConfigurationProperty.asNull(),
						ConfigurationProperty.of(expectedInet4Addresses), ConfigurationProperty.of(145),
						ConfigurationProperty.asNotSet(), ConfigurationProperty.asNotSet(), null));

		var actualInet4Addresses = new HashSet<Inet4AddressWithSubnetmask>();
		actualInet4Addresses.add(Inet4AddressWithSubnetmask.fromString("foo", "192.168.178.2/24"));
		var networkInterface = new NetworkInterface<Void>("eth0", ConfigurationProperty.of(false),
				ConfigurationProperty.of(false), ConfigurationProperty.asNull(), ConfigurationProperty.asNull(),
				ConfigurationProperty.of(actualInet4Addresses), ConfigurationProperty.of(145),
				ConfigurationProperty.asNotSet(), ConfigurationProperty.asNotSet(), null);

		List<NetworkInterface<?>> actual = Lists.newArrayList(networkInterface);

		assertTrue(ComponentUtilImpl.equals(expected, actual));
	}

	@Test
	public void testIsSameConfiguration() {
		var expected = new ComponentDef("id", "alias", "factorieId", ComponentProperties.emptyProperties(),
				Configuration.create().build());
		var actual = new EdgeConfig.Component("id", "alias", "factorieId", JsonUtils.buildJsonObject().build());
		assertTrue(ComponentUtilImpl.isSameConfiguration(null, expected, actual));
	}

	@Test
	public void testIsSameConfigurationWithoutAlias() {
		var expected = new ComponentDef("id", "alias1", "factorieId", ComponentProperties.emptyProperties(),
				Configuration.create().build());
		var actual = new EdgeConfig.Component("id", "alias2", "factorieId", JsonUtils.buildJsonObject().build());
		assertTrue(ComponentUtilImpl.isSameConfigurationWithoutAlias(null, expected, actual));
	}

	@Test
	public void testIsSameConfigurationWithoutId() {
		var expected = new ComponentDef("id1", "alias", "factorieId", ComponentProperties.emptyProperties(),
				Configuration.create().build());
		var actual = new EdgeConfig.Component("id2", "alias", "factorieId", JsonUtils.buildJsonObject().build());
		assertTrue(ComponentUtilImpl.isSameConfigurationWithoutId(null, expected, actual));
	}

	@Test
	public void testIsSameConfigurationWithoutIdAndAlias() {
		var expected = new ComponentDef("id1", "alias1", "factorieId", ComponentProperties.emptyProperties(),
				Configuration.create().build());
		var actual = new EdgeConfig.Component("id2", "alias2", "factorieId", JsonUtils.buildJsonObject().build());
		assertTrue(ComponentUtilImpl.isSameConfigurationWithoutIdAndAlias(null, expected, actual));
	}

	@Test
	public void testOrder() {
		var components = Lists.newArrayList(//
				new ComponentDef("id0", "alias", "factorieId", //
						ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
								.addProperty("using.id", "id1") //
								.build()),
						Configuration.create().build()),
				new ComponentDef("id2", "alias", "factorieId", //
						ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
								.addProperty("using.id", "id3") //
								.build()),
						Configuration.create().build()),
				new ComponentDef("id1", "alias", "factorieId", //
						ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
								.addProperty("using.id", "id3") //
								.build()),
						Configuration.create().build()),
				new ComponentDef("id3", "alias", "factorieId", //
						ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
								.build()),
						Configuration.create().build()));

		final var orderedComponents = ComponentUtilImpl.order(components);

		// expected: id3 -> id1 -> id0 -> id2
		Queue<String> expectedOrder = new LinkedList<>();
		expectedOrder.add("id3");
		expectedOrder.add("id1");
		expectedOrder.add("id0");
		expectedOrder.add("id2");

		for (var component : orderedComponents) {
			var expectedId = expectedOrder.poll();
			assertEquals(component.id(), expectedId);
		}
	}

}
