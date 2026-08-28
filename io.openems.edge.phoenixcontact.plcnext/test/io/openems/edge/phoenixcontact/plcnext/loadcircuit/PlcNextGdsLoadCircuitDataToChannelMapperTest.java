package io.openems.edge.phoenixcontact.plcnext.loadcircuit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappedValue;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapperImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PlcNextGdsLoadCircuitDataToChannelMapperTest {

	private PlcNextGdsDataToChannelMapperImpl dataMapper;
	private String instanceName;

	@Before
	public void setupBefore() {
		this.dataMapper = new PlcNextGdsDataToChannelMapperImpl();
		this.instanceName = "LoadCircuit.";
	}

	@Test
	public void testSinglePrimitiveVariableMapping_Successfully() {
		// prep
		var expectedValue = 12345;

		var primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", this.instanceName + "MaxPowerExport");
		primitiveVariable.addProperty("value", expectedValue);

		// test
		var mappedValue = this.dataMapper.mapSingleValueToChannel(primitiveVariable,
				this.instanceName,
				"jUnit", PlcNextLoadCircuitGdsDataReadMappingDefinition.values());

		// check
		assertNotNull(mappedValue);

		assertEquals(PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_EXPORT, mappedValue.getChannelId());
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
		primitiveVariable.addProperty("path", this.instanceName + "MaxReactivePower");
		primitiveVariable.addProperty("value", expectedValue);

		var variables = new JsonArray();
        variables.add(primitiveVariable);

        apiResponse.add("variables", variables);

		// test
		List<PlcNextGdsDataMappedValue> mappedValues = this.dataMapper.mapAllValuesToChannels(variables,
				this.instanceName,
				"jUnit", PlcNextLoadCircuitGdsDataReadMappingDefinition.values());

        // check
        assertNotNull(mappedValues);
		assertEquals(1, mappedValues.size());

		var mappedValue = mappedValues.getFirst();
		assertEquals(PlcNextLoadCircuit.ChannelId.MAX_REACTIVE_POWER, mappedValue.getChannelId());
		assertEquals(expectedValue, mappedValue.getValue());
	}

	@Test
	public void testAllPlcNextVariablesAreMapped_Successfully() {
		// prep
		var variables = new JsonArray();

		var expectedValueMaxPowerExport = 11001;
		var varPhaseVoltages = new JsonObject();
		varPhaseVoltages.addProperty("path", this.instanceName + "MaxPowerExport");
		varPhaseVoltages.addProperty("value", expectedValueMaxPowerExport);
		variables.add(varPhaseVoltages);

        var expectedValueMaxPowerImport = 22001;
		var varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path", this.instanceName + "MaxPowerImport");
		varNeutralCurrent.addProperty("value", expectedValueMaxPowerImport);
		variables.add(varNeutralCurrent);

		var mappedVariableCount = 2;

		// test
		var mappedValues = this.dataMapper.mapAllValuesToChannels(variables,
				this.instanceName,
				"jUnit", PlcNextLoadCircuitGdsDataReadMappingDefinition.values());

		// check
		assertNotNull(mappedValues);
		assertEquals(mappedVariableCount, mappedValues.size());

		var maxPowerExport = mappedValues.stream()//
				.filter(item -> PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_EXPORT == item.getChannelId())//
				.findFirst().orElse(null);
		assertNotNull(maxPowerExport);
		assertEquals(expectedValueMaxPowerExport, maxPowerExport.getValue());

		var maxPowerImport = mappedValues.stream()//
				.filter(item -> PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_IMPORT == item.getChannelId())//
				.findFirst().orElse(null);
		assertNotNull(maxPowerImport);
		assertEquals(expectedValueMaxPowerImport, maxPowerImport.getValue());
	}
}
