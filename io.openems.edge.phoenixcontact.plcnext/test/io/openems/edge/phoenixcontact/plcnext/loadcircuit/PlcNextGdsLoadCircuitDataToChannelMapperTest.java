package io.openems.edge.phoenixcontact.plcnext.loadcircuit;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappedValue;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapperImpl;

public class PlcNextGdsLoadCircuitDataToChannelMapperTest {

	private PlcNextGdsDataToChannelMapperImpl dataMapper;
	private String instanceName;

	@BeforeEach
	public void setupBefore() {
		dataMapper = new PlcNextGdsDataToChannelMapperImpl();
		instanceName = "LoadCircuit.";
	}

	@Test
	public void testSinglePrimitiveVariableMapping_Successfully() {
		// prep
		Integer expectedValue = 12345;

		JsonObject primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", instanceName + "MaxPowerExport");
		primitiveVariable.addProperty("value", expectedValue);

		// test
		PlcNextGdsDataMappedValue mappedValue = dataMapper.mapSingleValueToChannel(primitiveVariable, instanceName,
				"jUnit", PlcNextLoadCircuitGdsDataReadMappingDefinition.values());

		// check
		Assertions.assertNotNull(mappedValue);

		Assertions.assertEquals(PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_EXPORT, mappedValue.getChannelId());
		Assertions.assertEquals(expectedValue, mappedValue.getValue());
	}

	@Test
	public void testAllPrimitiveVariableMapping_Successfully() {
		// prep
		Integer expectedValue = 12345;

		JsonObject apiResponse = new JsonObject();
		apiResponse.addProperty("apVersion", "1.13.0.0");
		apiResponse.addProperty("projectCRC", 1410814331);
		apiResponse.addProperty("userAuthenticationRequired", true);

		JsonArray variables = new JsonArray();
		JsonObject primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", instanceName + "MaxReactivePower");
		primitiveVariable.addProperty("value", expectedValue);
		variables.add(primitiveVariable);
		apiResponse.add("variables", variables);

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapAllValuesToChannels(variables, instanceName,
				"jUnit", PlcNextLoadCircuitGdsDataReadMappingDefinition.values());

		// check
		Assertions.assertNotNull(mappedValues);
		Assertions.assertEquals(1, mappedValues.size());

		PlcNextGdsDataMappedValue mappedValue = mappedValues.get(0);
		Assertions.assertEquals(PlcNextLoadCircuit.ChannelId.MAX_REACTIVE_POWER, mappedValue.getChannelId());
		Assertions.assertEquals(expectedValue, mappedValue.getValue());
	}

	@Test
	public void testAllPlcNextVariablesAreMapped_Successfully() {
		// prep
		Integer expectedValueMaxPowerExport = 11001;
		Integer expectedValueMaxPowerImport = 22001;

		JsonArray variables = new JsonArray();

		JsonObject varPhaseVoltages = new JsonObject();
		varPhaseVoltages.addProperty("path", instanceName + "MaxPowerExport");
		varPhaseVoltages.addProperty("value", expectedValueMaxPowerExport);
		variables.add(varPhaseVoltages);

		JsonObject varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path", instanceName + "MaxPowerImport");
		varNeutralCurrent.addProperty("value", expectedValueMaxPowerImport);
		variables.add(varNeutralCurrent);

		int mappedVariableCount = 2;

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapAllValuesToChannels(variables, instanceName,
				"jUnit", PlcNextLoadCircuitGdsDataReadMappingDefinition.values());

		// check
		Assertions.assertNotNull(mappedValues);
		Assertions.assertEquals(mappedVariableCount, mappedValues.size());

		PlcNextGdsDataMappedValue maxPowerExport = mappedValues.stream()//
				.filter(item -> PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_EXPORT == item.getChannelId())//
				.findFirst().orElse(null);
		Assertions.assertNotNull(maxPowerExport);
		Assertions.assertEquals((Object) expectedValueMaxPowerExport, maxPowerExport.getValue());

		PlcNextGdsDataMappedValue maxPowerImport = mappedValues.stream()//
				.filter(item -> PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_IMPORT == item.getChannelId())//
				.findFirst().orElse(null);
		Assertions.assertNotNull(maxPowerImport);
		Assertions.assertEquals((Object) expectedValueMaxPowerImport, maxPowerImport.getValue());
	}
}
