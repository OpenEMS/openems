package io.openems.edge.phoenixcontact.plcnext.loadcircuit.data;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappedValue;
import io.openems.edge.phoenixcontact.plcnext.loadcircuit.PlcNextLoadCircuit;
import io.openems.edge.phoenixcontact.plcnext.loadcircuit.PlcNextLoadCircuitImpl;
import io.openems.edge.phoenixcontact.plcnext.meter.PlcNextMeter;

public class PlcNextGdsLoadCircuitDataToChannelMapperTest {
	
	private PlcNextGdsLoadCircuitDataToChannelMapper dataMapper;
	private String instanceName;

	@Before
	public void setupBefore() {
		dataMapper = new PlcNextGdsLoadCircuitDataToChannelMapper();
		instanceName = "LoadCircuit";
	}


	@Test
	public void testSinglePrimitiveVariableMapping_Successfully() {
		// prep
		Integer expectedValue = 12345;
		
		JsonObject primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", "OpenEMS_V1Component1/LoadCircuit.udtIn.maxPower.MaxPowerExport");
		primitiveVariable.addProperty("value", expectedValue);

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapSingleValueToChannel(primitiveVariable,
				instanceName);

		// check
		Assert.assertNotNull(mappedValues);
		Assert.assertEquals(1, mappedValues.size());

		PlcNextGdsDataMappedValue mappedValue = mappedValues.get(0);
		Assert.assertEquals(PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_EXPORT, mappedValue.getChannelId());
		Assert.assertEquals(expectedValue, mappedValue.getValue());
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
		primitiveVariable.addProperty("path", "OpenEMS_V1Component1/LoadCircuit.udtIn.setPower.ReactivePower");
		primitiveVariable.addProperty("value", expectedValue);
		variables.add(primitiveVariable);
		apiResponse.add("variables", variables);

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapAllValuesToChannels(variables, instanceName);

		// check
		Assert.assertNotNull(mappedValues);
		Assert.assertEquals(1, mappedValues.size());

		PlcNextGdsDataMappedValue mappedValue = mappedValues.get(0);
		Assert.assertEquals(PlcNextLoadCircuit.ChannelId.MAX_REACTIVE_POWER, mappedValue.getChannelId());
		Assert.assertEquals(expectedValue, mappedValue.getValue());
	}

	@Test
	public void testAllPlcNextVariablesAreMapped_Successfully() {
		// prep
		Integer expectedValueMaxPowerExport = 11001;
		Integer expectedValueMaxPowerImport = 22001;
		
		JsonArray variables = new JsonArray();

		JsonObject varPhaseVoltages = new JsonObject();
		varPhaseVoltages.addProperty("path", "OpenEMS_V1Component1/LoadCircuit.udtIn.maxPower.MaxPowerExport");
		varPhaseVoltages.addProperty("value", expectedValueMaxPowerExport);
		variables.add(varPhaseVoltages);

		JsonObject varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path", "OpenEMS_V1Component1/LoadCircuit.udtIn.maxPower.MaxPowerImport");
		varNeutralCurrent.addProperty("value", expectedValueMaxPowerImport);
		variables.add(varNeutralCurrent);

		int mappedVariableCount = 2;

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapAllValuesToChannels(variables, instanceName);

		// check
		Assert.assertNotNull(mappedValues);
		Assert.assertEquals(mappedVariableCount, mappedValues.size());
		
		PlcNextGdsDataMappedValue maxPowerExport = mappedValues.stream()//
				.filter(item -> PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_EXPORT == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(maxPowerExport);		
		Assert.assertEquals((Object)expectedValueMaxPowerExport, maxPowerExport.getValue());
				
		PlcNextGdsDataMappedValue maxPowerImport = mappedValues.stream()//
				.filter(item -> PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_IMPORT == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(maxPowerImport);		
		Assert.assertEquals((Object)expectedValueMaxPowerImport, maxPowerImport.getValue());
	}
}
