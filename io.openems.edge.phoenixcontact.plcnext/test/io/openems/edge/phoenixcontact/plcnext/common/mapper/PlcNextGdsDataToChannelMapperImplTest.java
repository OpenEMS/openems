package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.edge.phoenixcontact.plcnext.meter.PlcNextMeterGdsDataReadMappingDefinition;

public class PlcNextGdsDataToChannelMapperImplTest {

	private PlcNextGdsDataToChannelMapperImpl dataMapper;
	private String instanceName;

	@Before
	public void setupBefore() {
		dataMapper = new PlcNextGdsDataToChannelMapperImpl();
		instanceName = "MeasurementDevice";
	}

	@Test
	public void testVariableNameExtraction_Successfully() {
		String expectedVariableName = "activePower";

		JsonObject primitiveVariable = new JsonObject();
		primitiveVariable.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn." + expectedVariableName);

		String variableName = dataMapper.getVariableName(primitiveVariable, instanceName).orElse(null);
		assertNotNull(variableName);
		assertEquals(expectedVariableName, variableName);
	}

	@Test
	public void testVariableNameExtraction_FailureDueToMissingPathElement() {
		JsonObject primitiveVariable = new JsonObject();

		String variableName = dataMapper.getVariableName(primitiveVariable, instanceName).orElse(null);
		assertNull(variableName);
	}
	
	@Test
	public void testMapping_FailureDueToMissingJsonPrimitiveNamedValue() {
		// prep
		JsonObject errorObject = new JsonObject();
		errorObject.addProperty("domain", "variables");
		errorObject.addProperty("reason", "NotExists");
		
		JsonObject responseBody = new JsonObject();
		responseBody.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.Arp.PlcEclr.energyMeasurement.EnergyExport");
		responseBody.addProperty("value", (String)null);
		responseBody.add("error", errorObject);
		
		// test
		 PlcNextGdsDataMappedValue result = dataMapper.mapSingleJsonPrimitiveVariable(responseBody, "value", 
				 PlcNextMeterGdsDataReadMappingDefinition.ENERGY_EXPORT, "junit");
		 
		// check
		assertNull(result);		
	}
}
