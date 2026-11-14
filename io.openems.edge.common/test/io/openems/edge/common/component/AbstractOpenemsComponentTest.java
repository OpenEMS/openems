package io.openems.edge.common.component;

import static io.openems.edge.common.component.AbstractOpenemsComponent.propertyIdToMethodName;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.channel.PropertyChannel;
import io.openems.common.types.EdgeConfig;

public class AbstractOpenemsComponentTest {

	/**
	 * Test configuration interface with select property.
	 */
	@ObjectClassDefinition(name = "Test Component with Select Property", description = "A test component configuration with a select property")
	@interface TestConfig {
		@AttributeDefinition(name = "Priority", description = "Priority level")
		@PropertyChannel
		String priority();
	}

	@Test
	public void testPropertyIdToMethodName() {
		assertEquals("modbus_id", propertyIdToMethodName("modbus.id"));
		assertEquals("modbus_component_id", propertyIdToMethodName("modbus.component.id"));
		assertEquals("alias", propertyIdToMethodName("alias"));
	}

	private static class DummyComponent extends AbstractOpenemsComponent implements OpenemsComponent {

		public DummyComponent(String id) {
			super(//
					OpenemsComponent.ChannelId.values() //
			);
			super.activate(null, id, "", true);
		}

	}

	@Test
	public void test() {
		assertThrows(IllegalArgumentException.class, () -> new DummyComponent(null));
	}

	@Test
	public void testAddChannelsForProperties() throws Exception {
		// This test verifies that getOrCreateChannel correctly parses select schema
		// (templateOptions.options) and adds channels with string options

		// Create a property with select schema
		JsonObject schemaJson = new JsonObject();
		schemaJson.addProperty("type", "select");

		JsonObject templateOptions = new JsonObject();
		JsonArray options = new JsonArray();

		// Add the select options
		List<String> optionValues = Arrays.asList("VERY_LOW", "LOW", "MEDIUM", "HIGH", "VERY_HIGH");
		List<String> optionLabels = Arrays.asList("Very Low", "Low", "Medium", "High", "Very High");

		for (int i = 0; i < optionValues.size(); i++) {
			JsonObject option = new JsonObject();
			option.addProperty("value", optionValues.get(i));
			option.addProperty("label", optionLabels.get(i));
			options.add(option);
		}

		templateOptions.add("options", options);
		schemaJson.add("templateOptions", templateOptions);

		// Create a real Property with select schema
		EdgeConfig.Factory.Property property = new EdgeConfig.Factory.Property(//
				"priority", "Priority", "Priority level", io.openems.common.types.OpenemsType.STRING, //
				false, false, null, schemaJson);

		// Create component
		DummyComponent comp = new DummyComponent("selectTest");

		// Get the getOrCreateChannel method and invoke it
		Method method = AbstractOpenemsComponent.class.getDeclaredMethod("getOrCreateChannel",
				EdgeConfig.Factory.Property.class, io.openems.common.types.OpenemsType.class, Class.class);
		method.setAccessible(true);

		// Invoke getOrCreateChannel with our test property
		Object result = method.invoke(comp, property, io.openems.common.types.OpenemsType.STRING, TestConfig.class);

		// Verify that the channel was created successfully
		assertNotNull("Channel should be created", result);

		// Verify that it's actually a Channel
		assertTrue("Result should be a Channel instance", result instanceof io.openems.edge.common.channel.Channel);

		io.openems.edge.common.channel.Channel<?> channel = (io.openems.edge.common.channel.Channel<?>) result;

		// Verify the channel has the correct ID
		assertNotNull("Channel ID should not be null", channel.channelId());

		// Verify that the channel was added to the component
		io.openems.edge.common.channel.Doc doc = channel.channelId().doc();
		assertNotNull("Channel Doc should not be null", doc);

		// Select-schema property becomes a real EnumDoc carrying the schema options
		assertTrue("Doc should be a StringEnumDoc", doc instanceof io.openems.edge.common.channel.StringEnumDoc);
		var enumDoc = (io.openems.edge.common.channel.StringEnumDoc) doc;
		List<String> docOptionValues = Arrays.stream(enumDoc.getOptions()).toList();
		assertTrue("Enum options should match the schema values", docOptionValues.containsAll(optionValues));
	}

}
