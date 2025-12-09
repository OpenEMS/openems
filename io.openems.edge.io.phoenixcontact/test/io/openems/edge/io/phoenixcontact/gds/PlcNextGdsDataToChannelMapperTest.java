package io.openems.edge.io.phoenixcontact.gds;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.edge.io.phoenixcontact.gds.enums.PlcNextGdsDataType;
import io.openems.edge.meter.api.ElectricityMeter;

public class PlcNextGdsDataToChannelMapperTest {

	private PlcNextGdsDataToChannelMapper dataMapper;
	private String instanceName;

	@Before
	public void setupBefore() {
		dataMapper = new PlcNextGdsDataToChannelMapper();
		instanceName = "MeasurementDevice";
	}

	@Test
	public void testVariableNameExtraction_Successfully() {
		String expectedVariableName = "activePower";

		JsonObject primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn." + expectedVariableName);

		String variableName = dataMapper.getVariableName(primitiveVariable, instanceName).orElse(null);
		Assert.assertNotNull(variableName);
		Assert.assertEquals(expectedVariableName, variableName);
	}

	@Test
	public void testVariableNameExtraction_FailureDueToMissingPathElement() {
		JsonObject primitiveVariable = new JsonObject();

		String variableName = dataMapper.getVariableName(primitiveVariable, instanceName).orElse(null);
		Assert.assertNull(variableName);
	}

	@Test
	public void testReadValueFromJson_Successfully() {
		Float expectedValue = Double.valueOf(1.2345).floatValue();
		JsonElement testElement = new JsonPrimitive("1.2345");

		Object jsonValue = dataMapper.getJsonValue(testElement, PlcNextGdsDataType.FLOAT32);

		Assert.assertNotNull(jsonValue);
		Assert.assertTrue(jsonValue instanceof Float);
		Assert.assertEquals(expectedValue, jsonValue);
	}

	@Test
	public void testReadValueFromJson_FailureDueToUnsupportedType() {
		JsonElement testElement = new JsonPrimitive("1.2345");

		Assert.assertThrows(PlcNextGdsDataMappingException.class, () -> {
			dataMapper.getJsonValue(testElement, PlcNextGdsDataType.FLOAT32_ARRAY_3);
		});
	}

	@Test
	public void testSinglePrimitiveVariableMapping_Successfully() {
		// prep
		JsonObject primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.activePower");
		primitiveVariable.addProperty("value", 1.2345);

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapSingleAspectToChannel(primitiveVariable, instanceName);

		// check
		Assert.assertNotNull(mappedValues);
		Assert.assertEquals(1, mappedValues.size());

		PlcNextGdsDataMappedValue mappedValue = mappedValues.get(0);
		Assert.assertEquals(ElectricityMeter.ChannelId.ACTIVE_POWER, mappedValue.getChannelId());
		Assert.assertEquals(1, mappedValue.getValue());
	}

	@Test
	public void testAllPrimitiveVariableMapping_Successfully() {
		// prep
		JsonObject apiResponse = new JsonObject();
		apiResponse.addProperty("apVersion", "1.13.0.0");
		apiResponse.addProperty("projectCRC", 1410814331);
		apiResponse.addProperty("userAuthenticationRequired", true);

		JsonArray variables = new JsonArray();
		JsonObject primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.activePower");
		primitiveVariable.addProperty("value", 1.2345);
		variables.add(primitiveVariable);
		apiResponse.add("variables", variables);

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapAllAspectsToChannel(variables, instanceName);

		// check
		Assert.assertNotNull(mappedValues);
		Assert.assertEquals(1, mappedValues.size());

		PlcNextGdsDataMappedValue mappedValue = mappedValues.get(0);
		Assert.assertEquals(ElectricityMeter.ChannelId.ACTIVE_POWER, mappedValue.getChannelId());
		Assert.assertEquals(1, mappedValue.getValue());
	}

}
