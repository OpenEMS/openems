package io.openems.edge.io.phoenixcontact.gds;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceImpl;
import io.openems.common.types.HttpStatus;
import io.openems.edge.io.phoenixcontact.auth.PlcNextTokenManager;
import io.openems.edge.io.phoenixcontact.gds.enums.PlcNextGdsDataVariableDefinition;

public class PlcNextGdsDataProviderTest {

	private PlcNextGdsDataProviderConfig dataProviderConfig;
	private String sessionId = "1234567890";

	private DummyBridgeHttp mockDummyBridgeHttp;
	private PlcNextTokenManager mockTokenManager;
	private PlcNextGdsDataToChannelMapper mockDataMapper;

	private PlcNextGdsDataProvider dataProvider;
	private String accessToken;

	@Before
	public void setupBefore() {
		dataProviderConfig = new PlcNextGdsDataProviderConfig("https://junit/_pxc_api/api/variables",
				"MeasurementDevice", List.of());
		accessToken = "dummy_access_token";

		mockDummyBridgeHttp = Mockito.mock(DummyBridgeHttp.class);
		when(mockDummyBridgeHttp.createService(any())).thenReturn(new HttpBridgeTimeServiceImpl(mockDummyBridgeHttp, //
				new DummyBridgeHttpExecutor(), new DummyEndpointFetcher()));

		mockTokenManager = Mockito.mock(PlcNextTokenManager.class);
		mockDataMapper = Mockito.mock(PlcNextGdsDataToChannelMapper.class);

		this.dataProvider = new PlcNextGdsDataProvider(mockDummyBridgeHttp, mockTokenManager, mockDataMapper);
	}

	@Test
	public void testBuildGdsDataEndpoint_Successfully() {
		// prep
		String expectedReqUrl = dataProviderConfig.dataUrl().concat(PlcNextGdsDataProvider.PATH_VARIABLES);
		String expectedReqBody = new StringBuilder("pathPrefix=")//
				.append(PlcNextGdsDataProviderConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME)//
				.append("/&paths=")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.phaseVoltages,")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.neutralCurrent,")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.energyImport")//
				.append("&sessionID=").append(sessionId)
				.toString();
		Map<String, String> expectedReqHeaders = Map.of(//
				"Authorization", "Bearer " + accessToken, //
				"Content-Type", "application/json", //
				"Accept", "application/json");
		PlcNextGdsDataVariableDefinition[] variableDefinitions = new PlcNextGdsDataVariableDefinition[] {
				PlcNextGdsDataVariableDefinition.PHASE_VOLTAGES, PlcNextGdsDataVariableDefinition.NEUTRAL_CURRENT,
				PlcNextGdsDataVariableDefinition.ENERGY_IMPORT };

		when(mockTokenManager.getToken()).thenReturn("dummy_access_token");
		when(mockTokenManager.hasValidToken()).thenReturn(true);

		// test
		Endpoint result = dataProvider.buildDataEndpointRepresentation(accessToken, sessionId, variableDefinitions,
				dataProviderConfig);

		// check
		Assert.assertNotNull(result);

		Assert.assertEquals(HttpMethod.POST, result.method());
		Assert.assertEquals(expectedReqUrl, result.url());

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
		String expectedReqUrl = dataProviderConfig.dataUrl().concat(PlcNextGdsDataProvider.PATH_VARIABLES);
		String expectedReqBody = new StringBuilder("pathPrefix=")//
				.append(PlcNextGdsDataProviderConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME)//
				.append("/&paths=")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.phaseVoltages,")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.neutralCurrent,")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.energyImport")//
				.append("&sessionID=").append(sessionId)//
				.toString();
		Map<String, String> expectedReqHeaders = Map.of(//
				"Content-Type", "application/json", //
				"Accept", "application/json");
		PlcNextGdsDataVariableDefinition[] variableDefinitions = new PlcNextGdsDataVariableDefinition[] {
				PlcNextGdsDataVariableDefinition.PHASE_VOLTAGES, PlcNextGdsDataVariableDefinition.NEUTRAL_CURRENT,
				PlcNextGdsDataVariableDefinition.ENERGY_IMPORT };

		when(mockTokenManager.getToken()).thenReturn(null);
		when(mockTokenManager.hasValidToken()).thenReturn(false);

		// test
		Endpoint result = dataProvider.buildDataEndpointRepresentation(null, sessionId, variableDefinitions,
				dataProviderConfig);

		// check
		Assert.assertNotNull(result);

		Assert.assertEquals(HttpMethod.POST, result.method());
		Assert.assertEquals(expectedReqUrl, result.url());

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
		String expectedReqUrl = dataProviderConfig.dataUrl() + PlcNextGdsDataProvider.PATH_VARIABLES;
		String expectedReqBody = "";
		Map<String, String> expectedReqHeaders = Map.of(//
				"Authorization", "Bearer " + accessToken, //
				"Content-Type", "application/json", //
				"Accept", "application/json");

		when(mockTokenManager.getToken()).thenReturn("dummy_access_token");
		when(mockTokenManager.hasValidToken()).thenReturn(true);

		// test
		Endpoint result = dataProvider.buildDataEndpointRepresentation(accessToken, sessionId, null,
				dataProviderConfig);

		// check
		Assert.assertNotNull(result);

		Assert.assertEquals(HttpMethod.POST, result.method());
		Assert.assertEquals(expectedReqUrl, result.url());

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
		when(mockTokenManager.getToken()).thenReturn(accessToken);
		when(mockTokenManager.hasValidToken()).thenReturn(true);

		Endpoint dataEndpoint = dataProvider.buildDataEndpointRepresentation(
				accessToken, sessionId, PlcNextGdsDataVariableDefinition.values(), dataProviderConfig);

		JsonObject dataResponseBody = new JsonObject();
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

		dataResponseBody.add("variables", variables);
		when(mockDummyBridgeHttp.requestJson(eq(dataEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(dataResponseBody)));

		Endpoint createSessionEndpoint = dataProvider.buildCreateSessionEndpoint(accessToken, dataProviderConfig);
		JsonObject createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", sessionId);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);
		when(mockDummyBridgeHttp.requestJson(eq(createSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<JsonElement>(HttpStatus.CREATED, Map.of(),
								createSessionResponseBody)));

		Endpoint maintainSessionEndpoint = dataProvider.buildMaintainSessionEndpoint(accessToken, sessionId,
				dataProviderConfig);
		JsonObject maintainSessionResponseBody = new JsonObject();
		maintainSessionResponseBody.addProperty("sessionID", sessionId);
		when(mockDummyBridgeHttp.requestJson(eq(maintainSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));

		// test
		dataProvider.readFromApiToChannels(dataProviderConfig);

		// check
		Mockito.verify(mockDataMapper, times(1)).mapAllValuesToChannels(any(), any());
	}

	@Test
	public void testFetchAllVariablesFromGds_FailureDueToException() {
		// prep
		when(mockTokenManager.getToken()).thenReturn(accessToken);
		when(mockTokenManager.hasValidToken()).thenReturn(true);

		Endpoint createSessionEndpoint = dataProvider.buildCreateSessionEndpoint(accessToken, dataProviderConfig);

		JsonObject createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", sessionId);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);

		when(mockDummyBridgeHttp.requestJson(eq(createSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<JsonElement>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		Endpoint maintainSessionEndpoint = dataProvider.buildMaintainSessionEndpoint(accessToken, sessionId,
				dataProviderConfig);

		JsonObject maintainSessionResponseBody = new JsonObject();
		maintainSessionResponseBody.addProperty("sessionID", sessionId);

		when(mockDummyBridgeHttp.requestJson(eq(maintainSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));

		Endpoint dataEndpoint = dataProvider.buildDataEndpointRepresentation(accessToken, sessionId,
				PlcNextGdsDataVariableDefinition.values(), dataProviderConfig);

		when(mockDummyBridgeHttp.requestJson(eq(dataEndpoint)))//
				.thenThrow(CompletionException.class);

		// test
		dataProvider.readFromApiToChannels(dataProviderConfig);

		// check
		Mockito.verify(mockDataMapper, times(0)).mapAllValuesToChannels(any(), any());
	}

	@Test
	public void testFetchAllVariablesFromGds_FailureDueToMissingAccessToken() {
		// prep
		when(mockTokenManager.getToken()).thenReturn(null);
		when(mockDummyBridgeHttp.requestJson(any(Endpoint.class)))//
				.thenThrow(CompletionException.class);

		// test
		dataProvider.readFromApiToChannels(dataProviderConfig);

		// check
		Mockito.verify(mockDataMapper, times(0)).mapAllValuesToChannels(any(), any());
	}
}
