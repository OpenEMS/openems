package io.openems.edge.io.phoenixcontact.gds;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.edge.io.phoenixcontact.auth.PlcNextTokenManager;
import io.openems.edge.io.phoenixcontact.gds.enums.PlcNextGdsDataVariableDefinition;

public class PlcNextGdsDataProviderTest {
	private PlcNextGdsDataProviderConfig dataClientConfig;

	private DummyBridgeHttp mockDummyAuthBridgeHttp;
	private PlcNextTokenManager mockTokenManager;
	private PlcNextGdsDataToChannelMapper mockDataMapper;

	private PlcNextGdsDataProvider dataProvider;

	@Before
	public void setupBefore() {
		dataClientConfig = new PlcNextGdsDataProviderConfig("https://192.168.1.10/_pxc_api/api/variables",
				"MeasurementDevice", List.of());

		mockDummyAuthBridgeHttp = Mockito.mock(DummyBridgeHttp.class);
		mockTokenManager = Mockito.mock(PlcNextTokenManager.class);
		mockDataMapper = Mockito.mock(PlcNextGdsDataToChannelMapper.class);

		this.dataProvider = new PlcNextGdsDataProvider(mockDummyAuthBridgeHttp, mockTokenManager, mockDataMapper);
	}

	@Test
	public void testBuildGdsDataEndpoint_Successfully() {
		// prep
		String expectedReqBody = new StringBuilder("pathPrefix=")//
				.append(PlcNextGdsDataProviderConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME)//
				.append("/&paths=")//
				.append(dataClientConfig.dataInstanceName()).append(".udtIn.phaseVoltages,")//
				.append(dataClientConfig.dataInstanceName()).append(".udtIn.neutralCurrent,")//
				.append(dataClientConfig.dataInstanceName()).append(".udtIn.energyImport")//
				.toString();
		Map<String, String> expectedReqHeaders = Map.of(//
				"Authorization", "Bearer dummy_access_token", //
				"Content-Type", "application/json", //
				"Accept", "application/json");
		PlcNextGdsDataVariableDefinition[] variableDefinitions = new PlcNextGdsDataVariableDefinition[] {
				PlcNextGdsDataVariableDefinition.PHASE_VOLTAGES, PlcNextGdsDataVariableDefinition.NEUTRAL_CURRENT,
				PlcNextGdsDataVariableDefinition.ENERGY_IMPORT };

		when(mockTokenManager.getToken()).thenReturn("dummy_access_token");

		// test
		Endpoint result = dataProvider.buildDataEndpointRepresentation(variableDefinitions, dataClientConfig);

		// check
		Assert.assertNotNull(result);

		Assert.assertEquals(HttpMethod.POST, result.method());
		Assert.assertEquals(dataClientConfig.dataUrl(), result.url());

		System.out.println("ECHO: expected headers: " + expectedReqHeaders);
		System.out.println("ECHO: current headers: " + result.properties());
		Assert.assertEquals(expectedReqHeaders, result.properties());

		System.out.println("ECHO: expected body: " + expectedReqBody);
		System.out.println("ECHO: current body: " + result.body());
		Assert.assertEquals(expectedReqBody, result.body());
	}

	@Test
	public void testBuildGdsDataEndpoint_SuccessfullyWhileTokenIsNotPresent() {
		// prep
		String expectedReqBody = new StringBuilder("pathPrefix=")//
				.append(PlcNextGdsDataProviderConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME)//
				.append("/&paths=")//
				.append(dataClientConfig.dataInstanceName()).append(".udtIn.phaseVoltages,")//
				.append(dataClientConfig.dataInstanceName()).append(".udtIn.neutralCurrent,")//
				.append(dataClientConfig.dataInstanceName()).append(".udtIn.energyImport")//
				.toString();
		Map<String, String> expectedReqHeaders = Map.of(//
				"Content-Type", "application/json", //
				"Accept", "application/json");
		PlcNextGdsDataVariableDefinition[] variableDefinitions = new PlcNextGdsDataVariableDefinition[] {
				PlcNextGdsDataVariableDefinition.PHASE_VOLTAGES, PlcNextGdsDataVariableDefinition.NEUTRAL_CURRENT,
				PlcNextGdsDataVariableDefinition.ENERGY_IMPORT };

		when(mockTokenManager.getToken()).thenReturn(null);

		// test
		Endpoint result = dataProvider.buildDataEndpointRepresentation(variableDefinitions, dataClientConfig);

		// check
		Assert.assertNotNull(result);

		Assert.assertEquals(HttpMethod.POST, result.method());
		Assert.assertEquals(dataClientConfig.dataUrl(), result.url());

		System.out.println("ECHO: expected headers: " + expectedReqHeaders);
		System.out.println("ECHO: current headers: " + result.properties());
		Assert.assertEquals(expectedReqHeaders, result.properties());

		System.out.println("ECHO: expected body: " + expectedReqBody);
		System.out.println("ECHO: current body: " + result.body());
		Assert.assertEquals(expectedReqBody, result.body());
	}

	@Test
	public void testBuildGdsDataEndpoint_SuccessfullyWhileVariableDefinitionsIsEmpty() {
		// prep
		String expectedReqBody = "";
		Map<String, String> expectedReqHeaders = Map.of(//
				"Authorization", "Bearer dummy_access_token", //
				"Content-Type", "application/json", //
				"Accept", "application/json");

		when(mockTokenManager.getToken()).thenReturn("dummy_access_token");

		// test
		Endpoint result = dataProvider.buildDataEndpointRepresentation(null, dataClientConfig);

		// check
		Assert.assertNotNull(result);

		Assert.assertEquals(HttpMethod.POST, result.method());
		Assert.assertEquals(dataClientConfig.dataUrl(), result.url());

		System.out.println("ECHO: expected headers: " + expectedReqHeaders);
		System.out.println("ECHO: current headers: " + result.properties());
		Assert.assertEquals(expectedReqHeaders, result.properties());

		System.out.println("ECHO: expected body: " + expectedReqBody);
		System.out.println("ECHO: current body: " + result.body());
		Assert.assertEquals(expectedReqBody, result.body());
	}

	@Test
	public void testFetchAllVariablesFromGds_Successfully() {
		// prep
		Set<PlcNextGdsDataVariableDefinition> variableDefinitions = Set.of(
				PlcNextGdsDataVariableDefinition.PHASE_VOLTAGES, PlcNextGdsDataVariableDefinition.NEUTRAL_CURRENT,
				PlcNextGdsDataVariableDefinition.ENERGY_IMPORT);

		JsonObject responseBody = new JsonObject();
		JsonArray variables = new JsonArray();

		JsonObject varPhaseVoltages = new JsonObject();
		varPhaseVoltages.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.phaseVoltages");

		JsonArray varPhaseVoltagesValues = new JsonArray(3);
		JsonObject varPhaseVoltagesL1 = new JsonObject();
		varPhaseVoltagesL1.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.phaseVoltages[0]");
		varPhaseVoltagesL1.addProperty("value", 1.2);
		varPhaseVoltagesValues.add(varPhaseVoltagesL1);

		JsonObject varPhaseVoltagesL2 = new JsonObject();
		varPhaseVoltagesL2.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.phaseVoltages[1]");
		varPhaseVoltagesL2.addProperty("value", 2.3);
		varPhaseVoltagesValues.add(varPhaseVoltagesL2);

		JsonObject varPhaseVoltagesL3 = new JsonObject();
		varPhaseVoltagesL3.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.phaseVoltages[2]");
		varPhaseVoltagesL3.addProperty("value", 3.4);
		varPhaseVoltagesValues.add(varPhaseVoltagesL3);

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

		responseBody.add("variables", variables);

		when(mockTokenManager.getToken()).thenReturn("dummy-access-token");
		when(mockDummyAuthBridgeHttp.requestJson(any(Endpoint.class)))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(responseBody)));

		// test
		dataProvider.readFromApiToChannels(dataClientConfig);

		// check
		Mockito.verify(mockDataMapper, times(1)).mapAllAspectsToChannel(any(), any());
	}

	@Test
	public void testFetchAllVariablesFromGds_FailureDueToException() {
		// prep
		when(mockTokenManager.getToken()).thenReturn("dummy-access-token");
		when(mockDummyAuthBridgeHttp.requestJson(any(Endpoint.class)))//
				.thenThrow(CompletionException.class);

		// test
		dataProvider.readFromApiToChannels(dataClientConfig);

		// check
		Mockito.verify(mockDataMapper, times(0)).mapAllAspectsToChannel(any(), any());
	}

	@Test
	public void testFetchAllVariablesFromGds_FailureDueToMissingAccessToken() {
		// prep
		when(mockTokenManager.getToken()).thenReturn(null);
		when(mockDummyAuthBridgeHttp.requestJson(any(Endpoint.class)))//
				.thenThrow(CompletionException.class);

		// test
		dataProvider.readFromApiToChannels(dataClientConfig);

		// check
		Mockito.verify(mockDataMapper, times(0)).mapAllAspectsToChannel(any(), any());
	}
}
