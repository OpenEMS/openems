package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

public class DummyPlcNextGdsDataToChannelMapperTest {

	private DummyPlcNextGdsDataToChannelMapper dataMapper;
	private String instanceName;

	@Before
	public void setupBefore() {
		dataMapper = new DummyPlcNextGdsDataToChannelMapper();
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
}
