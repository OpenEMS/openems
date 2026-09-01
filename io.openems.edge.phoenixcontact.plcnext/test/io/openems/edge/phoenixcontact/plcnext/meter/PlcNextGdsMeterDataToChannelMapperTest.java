package io.openems.edge.phoenixcontact.plcnext.meter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapperImpl;

public class PlcNextGdsMeterDataToChannelMapperTest {

	private PlcNextGdsDataToChannelMapper dataMapper;
	private String instanceName;

	@Before
	public void setupBefore() {
		this.dataMapper = new PlcNextGdsDataToChannelMapperImpl();
		this.instanceName = "MeasurementDevice.";
	}

	@Test
	public void testSinglePrimitiveVariableMapping_Successfully() {
		// prep
		var expectedValue = 12345;

		var primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", "MeasurementDevice.ActivePowerL123");
		primitiveVariable.addProperty("value", expectedValue);

		// test
		var mappedValue = this.dataMapper.mapSingleValueToChannel(primitiveVariable, this.instanceName, "jUnit",
				PlcNextMeterGdsDataReadMappingDefinition.values());

		// check
		assertNotNull(mappedValue);
		assertEquals(ElectricityMeter.ChannelId.ACTIVE_POWER, mappedValue.getChannelId());
		assertEquals(expectedValue, mappedValue.getValue());
	}

	@Test
	public void testAllPrimitiveVariableMapping_Successfully() {
		// prep
		var apiResponse = new JsonObject();
		apiResponse.addProperty("apVersion", "1.13.0.0");
		apiResponse.addProperty("projectCRC", 1410814331);
		apiResponse.addProperty("userAuthenticationRequired", true);

		var expectedValue = 12345;
		var primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", this.instanceName + "activePowerL123");
		primitiveVariable.addProperty("value", expectedValue);

		var variables = new JsonArray();
		variables.add(primitiveVariable);
		apiResponse.add("variables", variables);

		// test
		var mappedValues = this.dataMapper.mapAllValuesToChannels(variables, this.instanceName, "junit",
				PlcNextMeterGdsDataReadMappingDefinition.values());

		// check
		assertNotNull(mappedValues);
		assertEquals(1, mappedValues.size());

		var mappedValue = mappedValues.getFirst();
		assertEquals(ElectricityMeter.ChannelId.ACTIVE_POWER, mappedValue.getChannelId());
		assertEquals(expectedValue, mappedValue.getValue());
	}

	@Test
	public void testAllPlcNextVariablesAreMapped_Successfully() {
		// prep
		var variables = new JsonArray();

		var expectedValueVoltagesL1N = 11000;
		var varPhaseVoltages = new JsonObject();
		varPhaseVoltages.addProperty("path", this.instanceName + "VoltageL1N");
		varPhaseVoltages.addProperty("value", expectedValueVoltagesL1N);
		variables.add(varPhaseVoltages);

		var expectedValueNeutralCurrent = 55000;
		var varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path", this.instanceName + "CurrentNeutral");
		varNeutralCurrent.addProperty("value", expectedValueNeutralCurrent);
		variables.add(varNeutralCurrent);

		var expectedValueEnergyImport = 44000L;
		var varEnergyImport = new JsonObject();
		varEnergyImport.addProperty("path", this.instanceName + "EnergyImport");
		varEnergyImport.addProperty("value", expectedValueEnergyImport);
		variables.add(varEnergyImport);

		var mappedVariableCount = 3;

		// test
		var mappedValues = this.dataMapper.mapAllValuesToChannels(variables, this.instanceName, "jUnit",
				PlcNextMeterGdsDataReadMappingDefinition.values());

		// check
		assertNotNull(mappedValues);
		assertEquals(mappedVariableCount, mappedValues.size());

		var phaseVoltageL1 = mappedValues.stream()//
				.filter(item -> ElectricityMeter.ChannelId.VOLTAGE_L1 == item.getChannelId())//
				.findFirst().orElse(null);
		assertNotNull(phaseVoltageL1);
		assertEquals(expectedValueVoltagesL1N, phaseVoltageL1.getValue());

		var neutralCurrent = mappedValues.stream()//
				.filter(item -> PlcNextMeter.ChannelId.CURRENT_NEUTRAL == item.getChannelId())//
				.findFirst().orElse(null);
		assertNotNull(neutralCurrent);
		assertEquals(expectedValueNeutralCurrent, neutralCurrent.getValue());

		var energyImport = mappedValues.stream()//
				.filter(item -> ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY == item.getChannelId())//
				.findFirst().orElse(null);
		assertNotNull(energyImport);
		assertEquals(expectedValueEnergyImport, energyImport.getValue());
	}
}
