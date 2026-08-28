package io.openems.edge.phoenixcontact.plcnext.common.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceImpl;
import io.openems.common.types.HttpStatus;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManagerImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlcNextGdsDataProviderReadTest {

	private PlcNextGdsDataAccessConfig dataProviderConfig;
	private final String sessionId = "1234567890";

	private DummyBridgeHttp mockDummyBridgeHttp;
	private PlcNextTokenManager mockTokenManager;

	private PlcNextGdsDataProviderImpl dataProvider;
	private String accessToken;

	@Before
	public void setupBefore() {
		this.dataProviderConfig = new PlcNextGdsDataAccessConfig("https://junit/_pxc_api/api/variables", "MeasurementDevice",
				"meter0");
		this.accessToken = "dummy_access_token";

		this.mockDummyBridgeHttp = mock(DummyBridgeHttp.class);
		when(this.mockDummyBridgeHttp.createService(any())).thenReturn(
                new HttpBridgeTimeServiceImpl(this. mockDummyBridgeHttp, new DummyBridgeHttpExecutor()));

		this.mockTokenManager = mock(PlcNextTokenManagerImpl.class);

		this.dataProvider = new PlcNextGdsDataProviderImpl(this.mockDummyBridgeHttp, this.mockTokenManager);
	}

	@Test
	public void testBuildGdsDataEndpointToReadVariables_Successfully() {
		// prep
		var variableIdentifiers = List.of("phaseVoltages", "neutralCurrent", "energyImport");

		when(this.mockTokenManager.getToken()).thenReturn("dummy_access_token");
		when(this.mockTokenManager.hasValidToken()).thenReturn(true);

		// test
		var requestBody = this.dataProvider.buildPostBodyForRead(this.sessionId, variableIdentifiers, this.dataProviderConfig);
		var result = this.dataProvider.buildDataEndpointRepresentation(this.accessToken, HttpMethod.POST,
                requestBody, this.dataProviderConfig);

        // check
        assertNotNull(result);
		assertEquals(HttpMethod.POST, result.method());

        var expectedReqUrl = this.dataProviderConfig.dataUrl().concat(PlcNextGdsDataProvider.PATH_VARIABLES);
		assertEquals(expectedReqUrl, result.url());

        var expectedReqBody = new StringBuilder("pathPrefix=")//
                .append("&paths=")//
                .append(this.dataProviderConfig.dataInstanceName()).append("phaseVoltages,")//
                .append(this.dataProviderConfig.dataInstanceName()).append("neutralCurrent,")//
                .append(this.dataProviderConfig.dataInstanceName()).append("energyImport")//
                .append("&sessionID=").append(this.sessionId).toString();
		assertEquals(expectedReqBody, result.body());

        var expectedReqHeaders = Map.of(//
                "Authorization", "Bearer " + this.accessToken, //
                "Content-Type", "application/json", //
                "Accept", "application/json");
		assertEquals(expectedReqHeaders, result.properties());
	}

	@Test
	public void testBuildGdsDataEndpointToReadVariables_SuccessfullyWhileTokenIsNotPresent() {
		// prep
		var variableIdentifiers = List.of("phaseVoltages", "neutralCurrent", "energyImport");

		when(this.mockTokenManager.getToken()).thenReturn(null);
		when(this.mockTokenManager.hasValidToken()).thenReturn(false);

		// test
		var requestBody = this.dataProvider.buildPostBodyForRead(this.sessionId, variableIdentifiers, this.dataProviderConfig);
		var result = this.dataProvider.buildDataEndpointRepresentation(null, HttpMethod.POST, requestBody, this.dataProviderConfig);

        // check
        assertNotNull(result);
        assertEquals(HttpMethod.POST, result.method());

        var expectedReqUrl = this.dataProviderConfig.dataUrl().concat(PlcNextGdsDataProvider.PATH_VARIABLES);
		assertEquals(expectedReqUrl, result.url());

        var expectedReqBody = new StringBuilder("pathPrefix=")//
                .append("&paths=")//
                .append(this.dataProviderConfig.dataInstanceName()).append("phaseVoltages,")//
                .append(this.dataProviderConfig.dataInstanceName()).append("neutralCurrent,")//
                .append(this.dataProviderConfig.dataInstanceName()).append("energyImport")//
                .append("&sessionID=").append(this.sessionId)//
                .toString();
		assertEquals(expectedReqBody, result.body());

        var expectedReqHeaders = Map.of(//
                "Content-Type", "application/json", //
                "Accept", "application/json");
		assertEquals(expectedReqHeaders, result.properties());
	}

	@Test
	public void testBuildGdsDataEndpointToReadVariables_SuccessfullyWhileVariableDefinitionsIsEmpty() {
		// prep
		when(this.mockTokenManager.getToken()).thenReturn("dummy_access_token");
		when(this.mockTokenManager.hasValidToken()).thenReturn(true);

		// test
		var requestBody = this.dataProvider.buildPostBodyForRead(this.sessionId, null, this.dataProviderConfig);
		var result = this.dataProvider.buildDataEndpointRepresentation(this.accessToken, HttpMethod.POST, requestBody,
				this.dataProviderConfig);

        // check
        assertNotNull(result);
		assertEquals(HttpMethod.POST, result.method());

        var expectedReqUrl = this.dataProviderConfig.dataUrl() + PlcNextGdsDataProvider.PATH_VARIABLES;
		assertEquals(expectedReqUrl, result.url());

        var expectedReqBody = "";
		assertEquals(expectedReqBody, result.body());

        var expectedReqHeaders = Map.of(//
                "Authorization", "Bearer " + this.accessToken, //
                "Content-Type", "application/json", //
                "Accept", "application/json");
		assertEquals(expectedReqHeaders, result.properties());
	}

	@Test
	public void testFetchVariablesFromGds_Successfully() {
		// prep
		when(this.mockTokenManager.getToken()).thenReturn(this.accessToken);
		when(this.mockTokenManager.hasValidToken()).thenReturn(true);

		var varPhaseVoltages = new JsonObject();
		varPhaseVoltages.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.phaseVoltages");

		var varPhaseVoltagesValues = new JsonArray(3);
		var varPhaseVoltagesL1 = new JsonObject();
		varPhaseVoltagesL1.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.phaseVoltages[0]");
		varPhaseVoltagesL1.addProperty("value", 1.2);
		varPhaseVoltagesValues.add(varPhaseVoltagesL1);

		var varPhaseVoltagesL2 = new JsonObject();
		varPhaseVoltagesL2.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.phaseVoltages[1]");
		varPhaseVoltagesL2.addProperty("value", 2.3);
		varPhaseVoltagesValues.add(varPhaseVoltagesL2);

		var varPhaseVoltagesL3 = new JsonObject();
		varPhaseVoltagesL3.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.phaseVoltages[2]");
		varPhaseVoltagesL3.addProperty("value", 3.4);
		varPhaseVoltagesValues.add(varPhaseVoltagesL3);

		varPhaseVoltages.add("value", varPhaseVoltagesValues);

        var variables = new JsonArray();
		variables.add(varPhaseVoltages);

		var varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.neutralCurrent");
		varNeutralCurrent.addProperty("value", 5.5);
		variables.add(varNeutralCurrent);

		var varEnergyImport = new JsonObject();
		varEnergyImport.addProperty("path", "OpenEMS_V1Component1/MeasurementDevice.udtIn.energyImport");
		varEnergyImport.addProperty("value", 4.4);
		variables.add(varEnergyImport);

        var dataResponseBody = new JsonObject();
		dataResponseBody.add("variables", variables);

        var variableIdentifiers = List.of("phase_voltages", "neutral_current", "energy_import");
        var requestBody = this.dataProvider.buildPostBodyForRead(this.sessionId, variableIdentifiers, this.dataProviderConfig);
        var dataEndpoint = this.dataProvider.buildDataEndpointRepresentation(this.accessToken, HttpMethod.POST, requestBody,
                this.dataProviderConfig);
        when(this.mockDummyBridgeHttp.requestJson(eq(dataEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(dataResponseBody)));

		var createSessionEndpoint = this.dataProvider.buildCreateSessionEndpoint(this.accessToken, this.dataProviderConfig);
		var createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", this.sessionId);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);
		when(this.mockDummyBridgeHttp.requestJson(createSessionEndpoint))//
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		var maintainSessionEndpoint = this.dataProvider.buildMaintainSessionEndpoint(this.accessToken, this.sessionId, this.dataProviderConfig);
		var maintainSessionResponseBody = new JsonObject();
		maintainSessionResponseBody.addProperty("sessionID", this.sessionId);
		when(this.mockDummyBridgeHttp.requestJson(maintainSessionEndpoint))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));

		// test
		var result = this.dataProvider.readDataFromRestApi(variableIdentifiers, this.dataProviderConfig, null) //
				.join();

		// check
		assertNotNull(result);
	}

	@Test
	public void testFetchVariablesFromGds_FailureDueToException() {
		// prep
		when(this.mockTokenManager.getToken()).thenReturn(this.accessToken);
		when(this.mockTokenManager.hasValidToken()).thenReturn(true);

		var createSessionEndpoint = this.dataProvider.buildCreateSessionEndpoint(this.accessToken, this.dataProviderConfig);

		var createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", this.sessionId);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);

		when(this.mockDummyBridgeHttp.requestJson(eq(createSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		var maintainSessionEndpoint = this.dataProvider.buildMaintainSessionEndpoint(this.accessToken, this.sessionId,
				this.dataProviderConfig);

		var maintainSessionResponseBody = new JsonObject();
		maintainSessionResponseBody.addProperty("sessionID", this.sessionId);

		when(this.mockDummyBridgeHttp.requestJson(eq(maintainSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));

        var variableIdentifiers = List.of("phaseVoltages", "neutralCurrent", "energyImport");
		var requestBody = this.dataProvider.buildPostBodyForRead(this.sessionId, variableIdentifiers, this.dataProviderConfig);
		var dataEndpoint = this.dataProvider.buildDataEndpointRepresentation(this.accessToken, HttpMethod.POST, requestBody,
				this.dataProviderConfig);

		when(this.mockDummyBridgeHttp.requestJson(eq(dataEndpoint)))//
				.thenThrow(CompletionException.class);

		// test + check
		assertThrows(CompletionException.class, () -> 
			this.dataProvider.readDataFromRestApi(variableIdentifiers, this.dataProviderConfig, null).join());
	}

	@Test
	public void testFetchVariablesFromGds_FailureDueToMissingAccessToken() {
		// prep
		var variableIdentifiers = List.of("phaseVoltages", "neutralCurrent", "energyImport");

		when(this.mockTokenManager.getToken()).thenReturn(null);
		when(this.mockTokenManager.hasValidToken()).thenReturn(false);
		when(this.mockTokenManager.fetchToken(any())).thenReturn(
				CompletableFuture.completedFuture(null));

		when(this.mockDummyBridgeHttp.requestJson(any(Endpoint.class)))//
				.thenThrow(CompletionException.class);

		// test
		var result = this.dataProvider.readDataFromRestApi(
				variableIdentifiers, this.dataProviderConfig, null);

		// check
		assertNotNull(result);
		assertTrue(result.isCompletedExceptionally());
	}

	@Test
	public void testCreateSession_Successfully() {
		// prep
		when(this.mockTokenManager.getToken()).thenReturn(this.accessToken);
		when(this.mockTokenManager.hasValidToken()).thenReturn(true);

		var createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", this.sessionId);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);

		when(this.mockDummyBridgeHttp.requestJson(any()))//
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		// test
		var createSessionResponse = this.dataProvider.createOrFetchSessionID(this.dataProviderConfig)
				.join();

		// check
		assertNotNull(createSessionResponse);
		assertEquals(this.sessionId, createSessionResponse.sessionId());
		assertNotNull(createSessionResponse.sessionTimeout());
	}

	@Test
	public void testCreateSession_FailDueToUnexpectedResponse() {
		// prep
		when(this.mockTokenManager.getToken()).thenReturn(this.accessToken);
		when(this.mockTokenManager.hasValidToken()).thenReturn(true);

		when(this.mockDummyBridgeHttp.requestJson(any()))//
				.thenThrow(CompletionException.class);

		// test
		var result = this.dataProvider
				.createOrFetchSessionID(this.dataProviderConfig);

		// check
		assertNotNull(result);
		assertTrue(result.isCompletedExceptionally());
	}

	@Test
	public void testMaintainSession_Successfully() {
		// prep
		when(this.mockTokenManager.getToken()).thenReturn(this.accessToken);
		when(this.mockTokenManager.hasValidToken()).thenReturn(true);

        this.initializeSession();

		// test register
		var te = this.dataProvider.enableSessionMaintenance(Delay.immediate(), this.dataProviderConfig);

		// check register
		assertNotNull(te);

		// test trigger
		te.onResult().apply(HttpResponse.ok("{ 'sessionID': '" + this.sessionId + "'}"));

		// check trigger
		assertEquals(this.sessionId, this.dataProvider.getSessionId());
	}

	@Test
	public void testMaintainSession_FailDueToExpiredToken() {
		// prep
		when(this.mockTokenManager.hasValidToken()).thenReturn(false);

        this.initializeSession();

		// test register
		var te = this.dataProvider.enableSessionMaintenance(Delay.immediate(), this.dataProviderConfig);

		// check register
		assertNotNull(te);

		// test trigger
		te.onResult().apply(HttpResponse.ok("{}"));

		// check trigger
		assertNull(this.dataProvider.getSessionId());
	}

	@Test
	public void testMaintainSession_FailDueToCommunicationError() {
		// prep
		when(this.mockTokenManager.hasValidToken()).thenReturn(true);

        this.initializeSession();

		// test register
		var te = this.dataProvider.enableSessionMaintenance(Delay.immediate(), this.dataProviderConfig);

		// check register
		assertNotNull(te);

		// test trigger
		te.onError().accept(new HttpError.ResponseError(HttpStatus.UNAUTHORIZED, "{}"));

		// check trigger
		assertNull(this.dataProvider.getSessionId());
	}

	@Test
	public void testMaintainSession_FailDueToHttpStatusNeqOK() {
		// prep
		when(this.mockTokenManager.hasValidToken()).thenReturn(true);

        this.initializeSession();

		// test register
		var te = this.dataProvider.enableSessionMaintenance(Delay.immediate(), this.dataProviderConfig);

		// check register
		assertNotNull(te);

		// test trigger
		te.onResult().apply(new HttpResponse<>(HttpStatus.CONFLICT, Map.of(), "{}"));

		// check trigger
		assertNull(this.dataProvider.getSessionId());
	}

    private void initializeSession() {
        var createSessionResponseBody = new JsonObject();
        createSessionResponseBody.addProperty("sessionID", this.sessionId);
        createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);

        when(this.mockDummyBridgeHttp.requestJson(any()))//
                .thenReturn(CompletableFuture.supplyAsync(
                        () -> new HttpResponse<>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

        this.dataProvider.createOrFetchSessionID(this.dataProviderConfig).join();
    }
}
