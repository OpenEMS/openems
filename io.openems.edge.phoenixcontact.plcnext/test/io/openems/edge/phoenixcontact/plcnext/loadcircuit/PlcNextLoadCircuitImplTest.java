package io.openems.edge.phoenixcontact.plcnext.loadcircuit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceImpl;
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManagerImpl;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataAccessConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProviderImpl;
import io.openems.edge.phoenixcontact.plcnext.loadcircuit.data.PlcNextGdsLoadCircuitDataToChannelMapper;

public class PlcNextLoadCircuitImplTest {

	private static final String COMPONENT_ID = "loadCircuit0";

	private TestConfig myConfig;

	private BridgeHttp dummyAuthBridgeHttp;
	private BridgeHttp mockDummyDataBridgeHttp;

	private PlcNextTokenManager tokenManager;
	private PlcNextGdsDataProviderImpl dataProvider;
	private PlcNextGdsLoadCircuitDataToChannelMapper loadCircuitDataToChannelMapper;

	private PlcNextLoadCircuitImpl componentUnderTest;

	private String accessToken;

	@Before
	public void setupBefore() {
		this.myConfig = TestConfig.create() //
				.setId(COMPONENT_ID) //
				.build();
		this.componentUnderTest = new PlcNextLoadCircuitImpl();

		this.accessToken = "dummy_access";

		this.dummyAuthBridgeHttp = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
							"{'code': 'dummy_auth', 'expires_in': 600 }"));
				} else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
							"{'access_token': '" + accessToken + "'}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};

		this.mockDummyDataBridgeHttp = Mockito.mock(DummyBridgeHttp.class);
		when(mockDummyDataBridgeHttp.createService(any()))
				.thenReturn(new HttpBridgeTimeServiceImpl(mockDummyDataBridgeHttp, //
						new DummyBridgeHttpExecutor(), new DummyEndpointFetcher()));

		this.tokenManager = new PlcNextTokenManagerImpl(dummyAuthBridgeHttp);

		this.loadCircuitDataToChannelMapper = new PlcNextGdsLoadCircuitDataToChannelMapper();
		this.dataProvider = new PlcNextGdsDataProviderImpl(mockDummyDataBridgeHttp, this.tokenManager);

		JsonObject responseBody = new JsonObject();
		JsonArray variables = new JsonArray();

		JsonObject varMaxPowerExport = new JsonObject();
		varMaxPowerExport.addProperty("path", "OpenEMS_V1Component1/"+myConfig.dataInstanceName()+".udtIn.maxPower.MaxPowerExport");
		varMaxPowerExport.addProperty("value", 110001);
		variables.add(varMaxPowerExport);

		JsonObject varMaxPowerImport = new JsonObject();
		varMaxPowerImport.addProperty("path", "OpenEMS_V1Component1/"+myConfig.dataInstanceName()+".udtIn.maxPower.varMaxPowerImport");
		varMaxPowerImport.addProperty("value", 210001);
		variables.add(varMaxPowerImport);
		
		JsonObject varSetReactivePower = new JsonObject();
		varSetReactivePower.addProperty("path", "OpenEMS_V1Component1/"+myConfig.dataInstanceName()+".udtIn.setPower.ReactivePower");
		varSetReactivePower.addProperty("value", 320001);
		variables.add(varSetReactivePower);

		responseBody.add("variables", variables);

		String sessionId = "1234567890";
		PlcNextGdsDataAccessConfig dataProviderConfig = new PlcNextGdsDataAccessConfig(myConfig.baseUrl(),
				myConfig.dataInstanceName(), COMPONENT_ID);
		when(mockDummyDataBridgeHttp.requestJson(any()))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(responseBody)));

		Endpoint createSessionEndpoint = dataProvider.buildCreateSessionEndpoint(accessToken, dataProviderConfig);
		JsonObject createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", sessionId);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);
		when(mockDummyDataBridgeHttp.requestJson(eq(createSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<JsonElement>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		Endpoint maintainSessionEndpoint = dataProvider.buildMaintainSessionEndpoint(accessToken, sessionId,
				dataProviderConfig);
		JsonObject maintainSessionResponseBody = new JsonObject();
		maintainSessionResponseBody.addProperty("sessionID", sessionId);
		when(mockDummyDataBridgeHttp.requestJson(eq(maintainSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));
	}

	@Test
	public void testRunModule() throws Exception {
		ComponentTest test = new ComponentTest(componentUnderTest) //
				.addReference("gdsDataProvider", this.dataProvider) //
				.addReference("gdsLoadCircuitDataToChannelMapper", this.loadCircuitDataToChannelMapper); //

		test.next(new TestCase()); //

		test.deactivate();
	}
}
