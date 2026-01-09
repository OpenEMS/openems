package io.openems.edge.phoenixcontact.plcnext.meter;

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
import io.openems.edge.phoenixcontact.plcnext.meter.mapper.PlcNextGdsMeterDataToChannelMapper;

public class PlcNextMeterImplTest {

	private static final String COMPONENT_ID = "component0";

	private TestConfig myConfig;

	private BridgeHttp dummyAuthBridgeHttp;
	private BridgeHttp mockDummyDataBridgeHttp;

	private PlcNextTokenManager tokenManager;
	private PlcNextGdsDataProviderImpl dataProvider;
	private PlcNextGdsMeterDataToChannelMapper meterDataToChannelMapper;

	private PlcNextMeterImpl componentUnderTest;

	private String accessToken;

	@Before
	public void setupBefore() {
		this.myConfig = TestConfig.create() //
				.setId(COMPONENT_ID) //
				.build();
		this.componentUnderTest = new PlcNextMeterImpl();

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

		this.meterDataToChannelMapper = new PlcNextGdsMeterDataToChannelMapper();
		this.dataProvider = new PlcNextGdsDataProviderImpl(mockDummyDataBridgeHttp, this.tokenManager);

		JsonObject responseBody = new JsonObject();
		JsonArray variables = new JsonArray();

		JsonObject varPhaseVoltages = new JsonObject();
		varPhaseVoltages.addProperty("path", "OpenEMS_V1Component1/defaultOpenEmsInstance4JUnit.udtIn.phaseVoltages");

		JsonArray varPhaseVoltagesValues = new JsonArray(3);
		varPhaseVoltagesValues.add(1.1);
		varPhaseVoltagesValues.add(2.2);
		varPhaseVoltagesValues.add(3.4);

		varPhaseVoltages.add("value", varPhaseVoltagesValues);
		variables.add(varPhaseVoltages);

		JsonObject varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path", "OpenEMS_V1Component1/defaultOpenEmsInstance4JUnit.udtIn.neutralCurrent");
		varNeutralCurrent.addProperty("value", 5.5);
		variables.add(varNeutralCurrent);

		JsonObject varEnergyImport = new JsonObject();
		varEnergyImport.addProperty("path", "OpenEMS_V1Component1/defaultOpenEmsInstance4JUnit.udtIn.energyImport");
		varEnergyImport.addProperty("value", 4.4);
		variables.add(varEnergyImport);

		responseBody.add("variables", variables);
		System.out.println("ECHO: responseBody = " + responseBody);

		String sessionId = "1234567890";
		PlcNextGdsDataAccessConfig dataProviderConfig = new PlcNextGdsDataAccessConfig(myConfig.baseUrl(),
				myConfig.dataInstanceName());
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
				.addReference("gdsMeterDataToChannelMapper", this.meterDataToChannelMapper); //

		test.next(new TestCase()); //

		test.deactivate();
	}
}
