package io.openems.edge.phoenixcontact.plcnext.loadcircuit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceImpl;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManagerImpl;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataAccessConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProviderImpl;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapperImpl;
import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextUrlStringHelper;

public class PlcNextLoadCircuitImplTest {

	private static final String COMPONENT_ID = "loadCircuit0";
	private static final String SESSION_ID = "1234567890";

	private static ThrowingRunnable<Exception> assertChannelValue(PlcNextLoadCircuitImpl sut, ChannelId channelId,
			Object expectedValue) {
		return () -> assertEquals(expectedValue, sut.channel(channelId).value().get());
	}

	private TestConfig myConfig;
	
	private BridgeHttp dummyAuthBridgeHttp;
	private BridgeHttp mockDummyDataBridgeHttp;

	private PlcNextTokenManager tokenManager;

	private PlcNextGdsDataProviderImpl dataProvider;
	private PlcNextGdsDataAccessConfig dataProviderConfig;

	private PlcNextGdsDataToChannelMapper dataToChannelMapper;

	private PlcNextLoadCircuitImpl componentUnderTest;
	private ComponentTest test;

	private String accessToken;

	@Before
	public void setupBefore() throws Exception {
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

		this.dataProvider = new PlcNextGdsDataProviderImpl(mockDummyDataBridgeHttp, this.tokenManager);
		this.dataProviderConfig = new PlcNextGdsDataAccessConfig(myConfig.baseUrl(), myConfig.dataInstanceName(),
				COMPONENT_ID);

		this.dataToChannelMapper = new PlcNextGdsDataToChannelMapperImpl();

		String createSessionEndpointUrl = PlcNextUrlStringHelper.buildUrlString(dataProviderConfig.dataUrl(), PlcNextGdsDataProvider.PATH_SESSIONS);
		JsonObject createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", SESSION_ID);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);
		when(mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg) && //
					arg.method() == HttpMethod.POST && //
					arg.url().startsWith(createSessionEndpointUrl)))) //
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<JsonElement>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		String maintainSessionEndpointUrl = new StringBuilder(
				PlcNextUrlStringHelper.buildUrlString(dataProviderConfig.dataUrl(), PlcNextGdsDataProvider.PATH_SESSIONS))//
				.append("/").append(SESSION_ID).toString();
		JsonObject maintainSessionResponseBody = new JsonObject();
		maintainSessionResponseBody.addProperty("sessionID", SESSION_ID);
		when(mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg) && //
					arg.method() == HttpMethod.POST && //
					arg.url().startsWith(maintainSessionEndpointUrl)))) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));

		this.test = new ComponentTest(componentUnderTest) //
				.addReference("gdsDataProvider", this.dataProvider) //
				.addReference("gdsDataToChannelMapper", this.dataToChannelMapper)
				.addReference("configAdmin", new DummyConfigurationAdmin());
	}

	@Test
	public void testRunModuleSuccessfully() throws Exception {
		// prep
		int expectedMaxPowerExportValue = 110001;
		int expectedMaxPowerImportValue = 210001;
		int expectedReactivePowerValue = 320001;

		JsonObject responseBody = new JsonObject();
		JsonArray variables = new JsonArray();

		JsonObject varMaxPowerExport = new JsonObject();
		varMaxPowerExport.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.maxPower.MaxPowerExport");
		varMaxPowerExport.addProperty("value", expectedMaxPowerExportValue);
		variables.add(varMaxPowerExport);

		JsonObject varMaxPowerImport = new JsonObject();
		varMaxPowerImport.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.maxPower.MaxPowerImport");
		varMaxPowerImport.addProperty("value", expectedMaxPowerImportValue);
		variables.add(varMaxPowerImport);

		JsonObject varSetReactivePower = new JsonObject();
		varSetReactivePower.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.setPower.ReactivePower");
		varSetReactivePower.addProperty("value", expectedReactivePowerValue);
		variables.add(varSetReactivePower);

		responseBody.add("variables", variables);

		String dataEndpointUrl = PlcNextUrlStringHelper.buildUrlString(
				dataProviderConfig.dataUrl(), PlcNextGdsDataProvider.PATH_VARIABLES);
		when(mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg) && //
					arg.method() == HttpMethod.POST && //
					arg.url().equals(dataEndpointUrl)))) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(responseBody)));

		// test + check
		this.test.activate(myConfig); //

		this.test.next(new TestCase("Trigger value consumption and do one wait cycle")) //
				.next(new TestCase("Check requested data dropped in asynchronously")
					.onAfterProcessImage(assertChannelValue(componentUnderTest,
						PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_EXPORT, expectedMaxPowerExportValue)) //
					.onAfterProcessImage(assertChannelValue(componentUnderTest,
						PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_IMPORT, expectedMaxPowerImportValue)) //
					.onAfterProcessImage(assertChannelValue(componentUnderTest,
						PlcNextLoadCircuit.ChannelId.MAX_REACTIVE_POWER, expectedReactivePowerValue))); //

		this.test.deactivate();
	}
}
