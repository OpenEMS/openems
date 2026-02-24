package io.openems.edge.phoenixcontact.plcnext.meter;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappedValue;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapperImpl;

public class PlcNextGdsMeterDataToChannelMapperTest {

	private PlcNextGdsDataToChannelMapper dataMapper;
	private String instanceName;

	@Before
	public void setupBefore() {
		dataMapper = new PlcNextGdsDataToChannelMapperImpl();
		instanceName = "MeasurementDevice";
	}

	@Test
	public void testSinglePrimitiveVariableMapping_Successfully() {
		// prep
		Integer expectedValue = 12345;

		JsonObject primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path",
				"OpenEMS_V1Component1/MeasurementDevice.udtIn.powerMeasurement.activePower.L123");
		primitiveVariable.addProperty("value", expectedValue);

		// test
		PlcNextGdsDataMappedValue mappedValue = dataMapper.mapSingleValueToChannel(primitiveVariable,
				instanceName, "jUnit", PlcNextMeterGdsDataReadMappingDefinition.values());

		// check
		Assert.assertNotNull(mappedValue);

		Assert.assertEquals(ElectricityMeter.ChannelId.ACTIVE_POWER, mappedValue.getChannelId());
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
		primitiveVariable.addProperty("path",
				"OpenEMS_V1Component1/MeasurementDevice.udtIn.powerMeasurement.activePower.L123");
		primitiveVariable.addProperty("value", expectedValue);
		variables.add(primitiveVariable);
		apiResponse.add("variables", variables);

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapAllValuesToChannels(variables, 
				instanceName, "junit", PlcNextMeterGdsDataReadMappingDefinition.values());

		// check
		Assert.assertNotNull(mappedValues);
		Assert.assertEquals(1, mappedValues.size());

		PlcNextGdsDataMappedValue mappedValue = mappedValues.get(0);
		Assert.assertEquals(ElectricityMeter.ChannelId.ACTIVE_POWER, mappedValue.getChannelId());
		Assert.assertEquals(expectedValue, mappedValue.getValue());
	}

	@Test
	public void testAllPlcNextVariablesAreMapped_Successfully() {
		// prep
		Integer expectedValueVoltagesL1N = 11000;
		Integer expectedValueNeutralCurrent = 55000;
		Long expectedValueEnergyImport = 44000L;

		JsonArray variables = new JsonArray();

		JsonObject varPhaseVoltages = new JsonObject();
		varPhaseVoltages.addProperty("path",
				"OpenEMS_V1Component1/MeasurementDevice.udtIn.voltageMeasurement.phasesToNeutral.L1N");
		varPhaseVoltages.addProperty("value", expectedValueVoltagesL1N);
		variables.add(varPhaseVoltages);

		JsonObject varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path",
				"OpenEMS_V1Component1/MeasurementDevice.udtIn.currentMeasurement.phases.Neutral");
		varNeutralCurrent.addProperty("value", expectedValueNeutralCurrent);
		variables.add(varNeutralCurrent);

		JsonObject varEnergyImport = new JsonObject();
		varEnergyImport.addProperty("path",
				"OpenEMS_V1Component1/MeasurementDevice.udtIn.energyMeasurement.EnergyImport");
		varEnergyImport.addProperty("value", expectedValueEnergyImport);
		variables.add(varEnergyImport);

		int mappedVariableCount = 3;

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = dataMapper.mapAllValuesToChannels(variables, 
				instanceName, "jUnit", PlcNextMeterGdsDataReadMappingDefinition.values());

		// check
		Assert.assertNotNull(mappedValues);
		Assert.assertEquals(mappedVariableCount, mappedValues.size());

		PlcNextGdsDataMappedValue phaseVolatageL1 = mappedValues.stream()//
				.filter(item -> ElectricityMeter.ChannelId.VOLTAGE_L1 == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(phaseVolatageL1);
		Assert.assertEquals((Object) expectedValueVoltagesL1N, phaseVolatageL1.getValue());

		PlcNextGdsDataMappedValue neutralCurrent = mappedValues.stream()//
				.filter(item -> PlcNextMeter.ChannelId.CURRENT_NEUTRAL == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(neutralCurrent);
		Assert.assertEquals((Object) expectedValueNeutralCurrent, neutralCurrent.getValue());

		PlcNextGdsDataMappedValue energyImport = mappedValues.stream()//
				.filter(item -> ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY == item.getChannelId())//
				.findFirst().orElse(null);
		Assert.assertNotNull(energyImport);
		Assert.assertEquals((Object) expectedValueEnergyImport, energyImport.getValue());
	}
}
