package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.Test;

import com.google.common.collect.Lists;

import io.openems.common.types.ConfigurationProperty;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.host.Inet4AddressWithSubnetmask;
import io.openems.edge.core.host.NetworkInterface;

public class ComponentUtilTest {

	@Test
	public void testEqualsJsonElementJsonElement() {
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
	}

	@Test
	public void testEqualsListOfNetworkInterface() throws Exception {
		var expectedInet4Addresses = new HashSet<Inet4AddressWithSubnetmask>();
		expectedInet4Addresses.add(Inet4AddressWithSubnetmask.fromString("foo", "192.168.178.2/24"));
		List<NetworkInterface<?>> expected = Lists
				.newArrayList(new NetworkInterface<Void>("eth0", ConfigurationProperty.of(false),
						ConfigurationProperty.of(false), ConfigurationProperty.asNull(), ConfigurationProperty.asNull(),
						ConfigurationProperty.of(expectedInet4Addresses), ConfigurationProperty.of(145), null));

		var actualInet4Addresses = new HashSet<Inet4AddressWithSubnetmask>();
		actualInet4Addresses.add(Inet4AddressWithSubnetmask.fromString("foo", "192.168.178.2/24"));
		var networkInterface = new NetworkInterface<Void>("eth0", ConfigurationProperty.of(false),
				ConfigurationProperty.of(false), ConfigurationProperty.asNull(), ConfigurationProperty.asNull(),
				ConfigurationProperty.of(actualInet4Addresses), ConfigurationProperty.of(145), null);

		List<NetworkInterface<?>> actual = Lists.newArrayList(networkInterface);

		assertTrue(ComponentUtilImpl.equals(expected, actual));
	}

	@Test
	public void testIsSameConfiguration() {
		var expected = new EdgeConfig.Component("id", "alias", "factorieId", JsonUtils.buildJsonObject().build());
		var actual = new EdgeConfig.Component("id", "alias", "factorieId", JsonUtils.buildJsonObject().build());
		assertTrue(ComponentUtilImpl.isSameConfiguration(null, expected, actual));
	}

	@Test
	public void testIsSameConfigurationWithoutAlias() {
		var expected = new EdgeConfig.Component("id", "alias1", "factorieId", JsonUtils.buildJsonObject().build());
		var actual = new EdgeConfig.Component("id", "alias2", "factorieId", JsonUtils.buildJsonObject().build());
		assertTrue(ComponentUtilImpl.isSameConfigurationWithoutAlias(null, expected, actual));
	}

	@Test
	public void testIsSameConfigurationWithoutId() {
		var expected = new EdgeConfig.Component("id1", "alias", "factorieId", JsonUtils.buildJsonObject().build());
		var actual = new EdgeConfig.Component("id2", "alias", "factorieId", JsonUtils.buildJsonObject().build());
		assertTrue(ComponentUtilImpl.isSameConfigurationWithoutId(null, expected, actual));
	}

	@Test
	public void testIsSameConfigurationWithoutIdAndAlias() {
		var expected = new EdgeConfig.Component("id1", "alias1", "factorieId", JsonUtils.buildJsonObject().build());
		var actual = new EdgeConfig.Component("id2", "alias2", "factorieId", JsonUtils.buildJsonObject().build());
		assertTrue(ComponentUtilImpl.isSameConfigurationWithoutIdAndAlias(null, expected, actual));
	}

	@Test
	public void testOrder() {
		var components = Lists.newArrayList(//
				new EdgeConfig.Component("id0", "alias", "factorieId", //
						JsonUtils.buildJsonObject() //
								.addProperty("using.id", "id1") //
								.build()),
				new EdgeConfig.Component("id2", "alias", "factorieId", //
						JsonUtils.buildJsonObject() //
								.addProperty("using.id", "id3") //
								.build()),
				new EdgeConfig.Component("id1", "alias", "factorieId", //
						JsonUtils.buildJsonObject() //
								.addProperty("using.id", "id3") //
								.build()),
				new EdgeConfig.Component("id3", "alias", "factorieId", //
						JsonUtils.buildJsonObject() //
								.build()));

		final var orderedComponents = ComponentUtilImpl.order(components);

		// expected: id3 -> id1 -> id0 -> id2
		Queue<String> expectedOrder = new LinkedList<>();
		expectedOrder.add("id3");
		expectedOrder.add("id1");
		expectedOrder.add("id0");
		expectedOrder.add("id2");

		for (var component : orderedComponents) {
			var expectedId = expectedOrder.poll();
			assertEquals(component.getId(), expectedId);
		}
	}

}
