package io.openems.edge.phoenixcontact.plcnext.meter;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceImpl;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManagerImpl;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataAccessConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProviderImpl;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapperImpl;

public class PlcNextMeterImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String SESSION_ID = "1234567890";

	private static ThrowingRunnable<Exception> assertChannelValue(PlcNextMeterImpl sut, ChannelId channelId,
			Object expectedValue) {
		return () -> assertEquals(expectedValue, sut.channel(channelId).value().get());
	}

	private TestConfig myConfig;

	private BridgeHttp dummyAuthBridgeHttp;
	private BridgeHttp mockDummyDataBridgeHttp;

	private PlcNextTokenManager tokenManager;

	private PlcNextGdsDataProviderImpl dataProvider;
	private PlcNextGdsDataAccessConfig dataProviderConfig;

	private PlcNextGdsDataToChannelMapperImpl dataToChannelMapper;

	private PlcNextMeterImpl componentUnderTest;
	private ComponentTest test;

	private String accessToken;

	@Before
	public void setupBefore() throws Exception {
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

		this.dataToChannelMapper = new PlcNextGdsDataToChannelMapperImpl();
		this.dataProvider = new PlcNextGdsDataProviderImpl(mockDummyDataBridgeHttp, this.tokenManager);

		this.dataProviderConfig = new PlcNextGdsDataAccessConfig(myConfig.baseUrl(), myConfig.dataInstanceName(),
				COMPONENT_ID);

		Endpoint createSessionEndpoint = dataProvider.buildCreateSessionEndpoint(accessToken, dataProviderConfig);
		JsonObject createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", SESSION_ID);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);
		when(mockDummyDataBridgeHttp.requestJson(eq(createSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<JsonElement>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		Endpoint maintainSessionEndpoint = dataProvider.buildMaintainSessionEndpoint(accessToken, SESSION_ID,
				dataProviderConfig);
		JsonObject maintainSessionResponseBody = new JsonObject();
		maintainSessionResponseBody.addProperty("sessionID", SESSION_ID);
		when(mockDummyDataBridgeHttp.requestJson(eq(maintainSessionEndpoint)))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));

		this.test = new ComponentTest(componentUnderTest) //
				.addReference("gdsDataProvider", this.dataProvider) //
				.addReference("gdsDataToChannelMapper", this.dataToChannelMapper); //

	}

	@Test
	public void testRunModule() throws Exception {
		// prep
		int expectedPhases2Neutral1Value = 110000;
		int expectedPhases2Neutral2Value = 220000;
		int expectedPhases2Neutral3Value = 330000;
		int expectedPhasesNeutralValue = 550000;
		int expectedEnergyImportValue = 440000;

		JsonObject responseBody = new JsonObject();
		JsonArray variables = new JsonArray();

		JsonObject varPhaseVoltageL1N = new JsonObject();
		varPhaseVoltageL1N.addProperty("path", "OpenEMS_V1Component1/" + myConfig.dataInstanceName()
				+ ".udtIn.voltageMeasurement.phasesToNeutral.L1N");
		varPhaseVoltageL1N.addProperty("value", expectedPhases2Neutral1Value);
		variables.add(varPhaseVoltageL1N);

		JsonObject varPhaseVoltageL2N = new JsonObject();
		varPhaseVoltageL2N.addProperty("path", "OpenEMS_V1Component1/" + myConfig.dataInstanceName()
				+ ".udtIn.voltageMeasurement.phasesToNeutral.L2N");
		varPhaseVoltageL2N.addProperty("value", expectedPhases2Neutral2Value);
		variables.add(varPhaseVoltageL2N);

		JsonObject varPhaseVoltageL3N = new JsonObject();
		varPhaseVoltageL3N.addProperty("path", "OpenEMS_V1Component1/" + myConfig.dataInstanceName()
				+ ".udtIn.voltageMeasurement.phasesToNeutral.L3N");
		varPhaseVoltageL3N.addProperty("value", expectedPhases2Neutral3Value);
		variables.add(varPhaseVoltageL3N);

		JsonObject varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.currentMeasurement.phases.Neutral");
		varNeutralCurrent.addProperty("value", expectedPhasesNeutralValue);
		variables.add(varNeutralCurrent);

		JsonObject varEnergyImport = new JsonObject();
		varEnergyImport.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.energyMeasurement.EnergyImport");
		varEnergyImport.addProperty("value", expectedEnergyImportValue);
		variables.add(varEnergyImport);

		responseBody.add("variables", variables);

		List<String> readVariableIdentifiers = Stream.of(PlcNextMeterGdsDataReadMappingDefinition.values())//
				.map(PlcNextGdsDataMappingDefinition::getIdentifier).toList();
		String readDataRequestBody = this.dataProvider.buildPostBodyForRead(SESSION_ID, readVariableIdentifiers,
				dataProviderConfig);
		Endpoint readDataEndpoint = this.dataProvider.buildDataEndpointRepresentation(this.accessToken, HttpMethod.POST,
				readDataRequestBody, this.dataProviderConfig);
		when(mockDummyDataBridgeHttp.requestJson(readDataEndpoint)) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(responseBody)));

		// test + check
		this.test.activate(myConfig); //

		this.test.next(new TestCase() //
				.onAfterProcessImage(assertChannelValue(componentUnderTest, ElectricityMeter.ChannelId.VOLTAGE_L1,
						expectedPhases2Neutral1Value)) //
				.onAfterProcessImage(assertChannelValue(componentUnderTest, ElectricityMeter.ChannelId.VOLTAGE_L2,
						expectedPhases2Neutral2Value)) //
				.onAfterProcessImage(assertChannelValue(componentUnderTest, ElectricityMeter.ChannelId.VOLTAGE_L3,
						expectedPhases2Neutral3Value)) //
				.onAfterProcessImage(assertChannelValue(componentUnderTest, PlcNextMeter.ChannelId.CURRENT_NEUTRAL,
						expectedPhasesNeutralValue)) //
				.onAfterProcessImage(assertChannelValue(componentUnderTest, PlcNextMeter.ChannelId.CURRENT_NEUTRAL,
						expectedPhasesNeutralValue))); //

		test.deactivate();
	}
}
