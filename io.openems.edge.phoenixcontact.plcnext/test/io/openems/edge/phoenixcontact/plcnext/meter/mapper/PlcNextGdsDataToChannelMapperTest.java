package io.openems.edge.phoenixcontact.plcnext.meter.mapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappedValue;
import io.openems.edge.phoenixcontact.plcnext.meter.PlcNextMeter;

public class PlcNextGdsDataToChannelMapperTest {

	private PlcNextGdsMeterDataToChannelMapper dataMapper;
	private String instanceName;

	@Before
	public void setupBefore() {
		dataMapper = new PlcNextGdsMeterDataToChannelMapper();
		instanceName = "MeasurementDevice";
	}


	@Test
	public void testSinglePrimitiveVariableMapping_Successfully() {
		// prep
		JsonObject primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.powerMeasurement.activePower");
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
		primitiveVariable.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.powerMeasurement.activePower");
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
		varPhaseVoltages.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.voltageMeasurement.VoltagesL1N");
		varPhaseVoltages.addProperty("value", 1.1);
		variables.add(varPhaseVoltages);

		JsonObject varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.currentMeasurement.neutralCurrent");
		varNeutralCurrent.addProperty("value", 5.5);
		variables.add(varNeutralCurrent);

		JsonObject varEnergyImport = new JsonObject();
		varEnergyImport.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.energyMeasurement.energyImport");
		varEnergyImport.addProperty("value", 4.4);
		variables.add(varEnergyImport);

		int mappedVariableCount = 3;

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapAllValuesToChannels(variables, instanceName);

		// check
		Assert.assertNotNull(mappedValues);
		Assert.assertEquals(mappedVariableCount, mappedValues.size());
		
		PlcNextGdsDataMappedValue phaseVolatageL1 = mappedValues.stream()//
				.filter(item -> ElectricityMeter.ChannelId.VOLTAGE_L1 == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(phaseVolatageL1);		
		Assert.assertEquals((Object)1, phaseVolatageL1.getValue());
				
		PlcNextGdsDataMappedValue neutralCurrent = mappedValues.stream()//
				.filter(item -> PlcNextMeter.ChannelId.CURRENT_NEUTRAL == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(neutralCurrent);		
		Assert.assertEquals((Object)5, neutralCurrent.getValue());
		
		PlcNextGdsDataMappedValue energyImport = mappedValues.stream()//
				.filter(item -> ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(energyImport);		
		Assert.assertEquals((Object)4l, energyImport.getValue());
	}
}
