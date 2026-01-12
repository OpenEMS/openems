package io.openems.edge.phoenixcontact.plcnext.common.data;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.common.bridge.http.time.HttpBridgeTimeService.TimeEndpoint;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceImpl;
import io.openems.common.types.HttpStatus;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManagerImpl;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProviderImpl.PlcNextCreateSessionResponse;

public class PlcNextGdsDataProviderTest {

	private PlcNextGdsDataAccessConfig dataProviderConfig;
	private String sessionId = "1234567890";

	private DummyBridgeHttp mockDummyBridgeHttp;
	private PlcNextTokenManager mockTokenManager;

	private PlcNextGdsDataProviderImpl dataProvider;
	private String accessToken;

	@Before
	public void setupBefore() {
		dataProviderConfig = new PlcNextGdsDataAccessConfig("https://junit/_pxc_api/api/variables",
				"MeasurementDevice", "meter0");
		accessToken = "dummy_access_token";

		mockDummyBridgeHttp = Mockito.mock(DummyBridgeHttp.class);
		when(mockDummyBridgeHttp.createService(any())).thenReturn(new HttpBridgeTimeServiceImpl(mockDummyBridgeHttp, //
				new DummyBridgeHttpExecutor(), new DummyEndpointFetcher()));

		mockTokenManager = Mockito.mock(PlcNextTokenManagerImpl.class);

		this.dataProvider = new PlcNextGdsDataProviderImpl(mockDummyBridgeHttp, mockTokenManager);
	}

	@Test
	public void testBuildGdsDataEndpoint_Successfully() {
		// prep
		String expectedReqUrl = dataProviderConfig.dataUrl().concat(PlcNextGdsDataProvider.PATH_VARIABLES);
		String expectedReqBody = new StringBuilder("pathPrefix=")//
				.append(PlcNextGdsDataAccessConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME)//
				.append("/&paths=")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.phaseVoltages,")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.neutralCurrent,")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.energyImport")//
				.append("&sessionID=").append(sessionId).toString();
		Map<String, String> expectedReqHeaders = Map.of(//
				"Authorization", "Bearer " + accessToken, //
				"Content-Type", "application/json", //
				"Accept", "application/json");
		List<String> variableIdentifiers = List.of("phaseVoltages", "neutralCurrent", "energyImport");

		when(mockTokenManager.getToken()).thenReturn("dummy_access_token");
		when(mockTokenManager.hasValidToken()).thenReturn(true);

		// test
		Endpoint result = dataProvider.buildDataEndpointRepresentation(accessToken, sessionId, variableIdentifiers,
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
				.append(PlcNextGdsDataAccessConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME)//
				.append("/&paths=")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.phaseVoltages,")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.neutralCurrent,")//
				.append(dataProviderConfig.dataInstanceName()).append(".udtIn.energyImport")//
				.append("&sessionID=").append(sessionId)//
				.toString();
		Map<String, String> expectedReqHeaders = Map.of(//
				"Content-Type", "application/json", //
				"Accept", "application/json");
		List<String> variableIdentifiers = List.of("phaseVoltages", "neutralCurrent", "energyImport");

		when(mockTokenManager.getToken()).thenReturn(null);
		when(mockTokenManager.hasValidToken()).thenReturn(false);

		// test
		Endpoint result = dataProvider.buildDataEndpointRepresentation(null, sessionId, variableIdentifiers,
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
	public void testFetchVariablesFromGds_Successfully() {
		// prep
		List<String> variableIdentifiers = List.of("phase_voltages", "neutral_current", "energy_import");

		when(mockTokenManager.getToken()).thenReturn(accessToken);
		when(mockTokenManager.hasValidToken()).thenReturn(true);

		Endpoint dataEndpoint = dataProvider.buildDataEndpointRepresentation(accessToken, sessionId,
				variableIdentifiers, dataProviderConfig);

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
						() -> new HttpResponse<JsonElement>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		Endpoint maintainSessionEndpoint = dataProvider.buildMaintainSessionEndpoint(accessToken, sessionId,
				dataProviderConfig);
		JsonObject maintainSessionResponseBody = new JsonObject();
		maintainSessionResponseBody.addProperty("sessionID", sessionId);
		when(mockDummyBridgeHttp.requestJson(eq(maintainSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));

		// test
		Optional<JsonObject> result = dataProvider.readDataFromRestApi(variableIdentifiers, dataProviderConfig, null);

		// check
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isPresent());
	}

	@Test
	public void testFetchVariablesFromGds_FailureDueToException() {
		// prep
		List<String> variableIdentifiers = List.of("phaseVoltages", "neutralCurrent", "energyImport");

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
				variableIdentifiers, dataProviderConfig);

		when(mockDummyBridgeHttp.requestJson(eq(dataEndpoint)))//
				.thenThrow(CompletionException.class);

		// test
		Optional<JsonObject> result = dataProvider.readDataFromRestApi(variableIdentifiers, dataProviderConfig, null);

		// check
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testFetchVariablesFromGds_FailureDueToMissingAccessToken() {
		// prep
		List<String> variableIdentifiers = List.of("phaseVoltages", "neutralCurrent", "energyImport");

		when(mockTokenManager.getToken()).thenReturn(null);
		when(mockDummyBridgeHttp.requestJson(any(Endpoint.class)))//
				.thenThrow(CompletionException.class);

		// test
		Optional<JsonObject> result = dataProvider.readDataFromRestApi(variableIdentifiers, dataProviderConfig, null);

		// check
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testCreateSession_Successfully() {
		// prep
		when(mockTokenManager.getToken()).thenReturn(accessToken);
		when(mockTokenManager.hasValidToken()).thenReturn(true);

		JsonObject createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", sessionId);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);

		when(mockDummyBridgeHttp.requestJson(any()))//
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<JsonElement>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		// test
		Optional<PlcNextCreateSessionResponse> createSessionResponse = dataProvider
				.createSessionIfNecessary(dataProviderConfig);

		// check
		Assert.assertNotNull(createSessionResponse);
		Assert.assertTrue(createSessionResponse.isPresent());
		Assert.assertEquals(sessionId, createSessionResponse.get().sessionId());
		Assert.assertNotNull(createSessionResponse.get().sessionTimeout());
	}

	@Test
	public void testCreateSession_FailDueToUnexpectedResponse() {
		// prep
		when(mockTokenManager.getToken()).thenReturn(accessToken);
		when(mockTokenManager.hasValidToken()).thenReturn(true);

		JsonObject createSessionResponseBody = new JsonObject();

		when(mockDummyBridgeHttp.requestJson(any()))//
				.thenReturn(CompletableFuture.supplyAsync(() -> new HttpResponse<JsonElement>(HttpStatus.UNAUTHORIZED,
						Map.of(), createSessionResponseBody)));

		// test
		Optional<PlcNextCreateSessionResponse> createSessionResponse = dataProvider
				.createSessionIfNecessary(dataProviderConfig);

		// check
		Assert.assertNotNull(createSessionResponse);
		Assert.assertTrue(createSessionResponse.isEmpty());
	}

	@Test
	public void testMaintainSession_Successfully() {
		// prep
		when(mockTokenManager.getToken()).thenReturn(accessToken);
		when(mockTokenManager.hasValidToken()).thenReturn(true);
		
		dataProvider.sessionId = "1234567890";

		// test register
		Optional<TimeEndpoint> ote = dataProvider.triggerSessionMaintenanceIfNecessary(Delay.immediate(),
				dataProviderConfig);

		// check register
		Assert.assertNotNull(ote);
		Assert.assertTrue(ote.isPresent());

		// test trigger
		ote.get().onResult().accept(HttpResponse.ok("{ 'sessionID': '" + sessionId + "'}"));

		// check trigger
		Assert.assertEquals(sessionId, dataProvider.sessionId);
	}

	@Test
	public void testMaintainSession_FailDueToExpiredToken() {
		// prep
		when(mockTokenManager.hasValidToken()).thenReturn(false);
		
		dataProvider.sessionId = "1234567890";

		// test register
		Optional<TimeEndpoint> ote = dataProvider.triggerSessionMaintenanceIfNecessary(Delay.immediate(),
				dataProviderConfig);

		// check register
		Assert.assertNotNull(ote);
		Assert.assertTrue(ote.isPresent());

		// test trigger
		ote.get().onResult().accept(HttpResponse.ok("{}"));

		// check trigger
		Assert.assertNull(dataProvider.sessionId);
	}

	@Test
	public void testMaintainSession_FailDueToCommunicationError() {
		// prep
		when(mockTokenManager.hasValidToken()).thenReturn(true);
		
		dataProvider.sessionId = "1234567890";

		// test register
		Optional<TimeEndpoint> ote = dataProvider.triggerSessionMaintenanceIfNecessary(Delay.immediate(),
				dataProviderConfig);

		// check register
		Assert.assertNotNull(ote);
		Assert.assertTrue(ote.isPresent());

		// test trigger
		ote.get().onError().accept(new HttpError.ResponseError(HttpStatus.UNAUTHORIZED, "{}"));

		// check trigger
		Assert.assertNull(dataProvider.sessionId);
	}

	@Test
	public void testMaintainSession_FailDueToHttpStatusNeqOK() {
		// prep
		when(mockTokenManager.hasValidToken()).thenReturn(true);

		dataProvider.sessionId = "1234567890";

		// test register
		Optional<TimeEndpoint> ote = dataProvider.triggerSessionMaintenanceIfNecessary(Delay.immediate(),
				dataProviderConfig);

		// check register
		Assert.assertNotNull(ote);
		Assert.assertTrue(ote.isPresent());

		// test trigger
		ote.get().onResult().accept(new HttpResponse<String>(HttpStatus.CONFLICT, Map.of(), "{}"));

		// check trigger
		Assert.assertNull(dataProvider.sessionId);
	}
}
