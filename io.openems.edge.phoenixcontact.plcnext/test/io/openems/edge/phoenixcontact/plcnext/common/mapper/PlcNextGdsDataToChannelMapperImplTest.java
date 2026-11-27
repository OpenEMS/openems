package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.edge.phoenixcontact.plcnext.meter.PlcNextMeterGdsDataReadMappingDefinition;

public class PlcNextGdsDataToChannelMapperImplTest {

	private PlcNextGdsDataToChannelMapperImpl dataMapper;
	private String instanceName;

	@BeforeEach
	public void setupBefore() {
		this.dataMapper = new PlcNextGdsDataToChannelMapperImpl();
		this.instanceName = "MeasurementDevice.";
	}

	@Test
	public void testVariableNameExtraction_Successfully() {
		String expectedVariableName = "activePower";

		JsonObject primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", this.instanceName + expectedVariableName);

		String variableName = this.dataMapper.getVariableName(primitiveVariable, this.instanceName) //
				.orElse(null);
		assertNotNull(variableName);
		assertEquals(expectedVariableName, variableName);
	}

	@Test
	public void testVariableNameExtraction_FailureDueToMissingPathElement() {
		JsonObject primitiveVariable = new JsonObject();

		String variableName = this.dataMapper.getVariableName(primitiveVariable, this.instanceName) //
				.orElse(null);
		assertNull(variableName);
	}

	@Test
	public void testMapping_FailureDueToMissingJsonPrimitiveNamedValue() {
		// prep
		JsonObject errorObject = new JsonObject();
		errorObject.addProperty("domain", "variables");
		errorObject.addProperty("reason", "NotExists");

		JsonObject responseBody = new JsonObject();
		responseBody.addProperty("path", this.instanceName + "energyMeasurement");
		responseBody.addProperty("value", (String) null);
		responseBody.add("error", errorObject);

		// test
		PlcNextGdsDataMappedValue result = this.dataMapper.mapSingleJsonPrimitiveVariable(responseBody,
				PlcNextMeterGdsDataReadMappingDefinition.ENERGY_EXPORT, "junit");

		// check
		assertNull(result);
	}
}
