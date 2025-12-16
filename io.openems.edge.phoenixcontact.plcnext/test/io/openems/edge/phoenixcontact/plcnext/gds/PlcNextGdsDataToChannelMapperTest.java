package io.openems.edge.phoenixcontact.plcnext.gds;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.PlcNextDevice;
import io.openems.edge.phoenixcontact.plcnext.gds.enums.PlcNextGdsDataType;

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
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapSingleValueToChannel(primitiveVariable,
				instanceName);

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
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapAllValuesToChannels(variables, instanceName);

		// check
		Assert.assertNotNull(mappedValues);
		Assert.assertEquals(1, mappedValues.size());

		PlcNextGdsDataMappedValue mappedValue = mappedValues.get(0);
		Assert.assertEquals(ElectricityMeter.ChannelId.ACTIVE_POWER, mappedValue.getChannelId());
		Assert.assertEquals(1, mappedValue.getValue());
	}

	@Test
	public void testAllPlcNextVariablesAreMapped_Successfully() {
		// prep
		JsonArray variables = new JsonArray();

		JsonObject varPhaseVoltages = new JsonObject();
		varPhaseVoltages.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.phaseVoltages");

		JsonArray varPhaseVoltagesValues = new JsonArray(3);
		varPhaseVoltagesValues.add(1.1);
		varPhaseVoltagesValues.add(2.2);
		varPhaseVoltagesValues.add(3.3);

		varPhaseVoltages.add("value", varPhaseVoltagesValues);
		variables.add(varPhaseVoltages);

		JsonObject varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.neutralCurrent");
		varNeutralCurrent.addProperty("value", 5.5);
		variables.add(varNeutralCurrent);

		JsonObject varEnergyImport = new JsonObject();
		varEnergyImport.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.energyImport");
		varEnergyImport.addProperty("value", 4.4);
		variables.add(varEnergyImport);

		int mappedVariableCount = 5;

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapAllValuesToChannels(variables, instanceName);

		// check
		Assert.assertNotNull(mappedValues);
		Assert.assertEquals(mappedVariableCount, mappedValues.size());
		
		
		PlcNextGdsDataMappedValue phaseVolatageL1 = mappedValues.stream()//
				.filter(item -> ElectricityMeter.ChannelId.VOLTAGE_L1 == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(phaseVolatageL1);		
		Assert.assertEquals((Object)1100, phaseVolatageL1.getValue());
		
		PlcNextGdsDataMappedValue phaseVolatageL2 = mappedValues.stream()//
				.filter(item -> ElectricityMeter.ChannelId.VOLTAGE_L2 == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(phaseVolatageL2);		
		Assert.assertEquals((Object)2200, phaseVolatageL2.getValue());
		
		PlcNextGdsDataMappedValue phaseVolatageL3 = mappedValues.stream()//
				.filter(item -> ElectricityMeter.ChannelId.VOLTAGE_L3 == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(phaseVolatageL3);		
		Assert.assertEquals((Object)3300, phaseVolatageL3.getValue());
		
		PlcNextGdsDataMappedValue neutralCurrent = mappedValues.stream()//
				.filter(item -> PlcNextDevice.ChannelId.NEUTRAL_CURRENT == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(neutralCurrent);		
		Assert.assertEquals((Object)5500.0, neutralCurrent.getValue());
		
		PlcNextGdsDataMappedValue energyImport = mappedValues.stream()//
				.filter(item -> ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(energyImport);		
		Assert.assertEquals((Object)4l, energyImport.getValue());
		
	}
}
