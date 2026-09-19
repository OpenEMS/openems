package io.openems.edge.phoenixcontact.plcnext.loadcircuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
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

	private MyConfig myConfig;

	private BridgeHttp dummyAuthBridgeHttp;
	private BridgeHttp mockDummyDataBridgeHttp;

	private PlcNextTokenManager tokenManager;

	private PlcNextGdsDataProviderImpl dataProvider;
	private PlcNextGdsDataAccessConfig dataProviderConfig;

	private PlcNextGdsDataToChannelMapper dataToChannelMapper;

	private PlcNextLoadCircuitImpl componentUnderTest;
	private ComponentTest test;

	private String accessToken;

	@BeforeEach
	public void setupBefore() throws Exception {
		this.myConfig = MyConfig.create() //
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
							"{'access_token': '" + PlcNextLoadCircuitImplTest.this.accessToken + "'}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};

		this.mockDummyDataBridgeHttp = mock(DummyBridgeHttp.class);
		when(this.mockDummyDataBridgeHttp.createService(any()))
				.thenReturn(new HttpBridgeTimeServiceImpl(this.mockDummyDataBridgeHttp, new DummyBridgeHttpExecutor()));
		this.tokenManager = new PlcNextTokenManagerImpl(this.dummyAuthBridgeHttp);

		this.dataProvider = new PlcNextGdsDataProviderImpl(this.mockDummyDataBridgeHttp, this.tokenManager);
		this.dataProviderConfig = new PlcNextGdsDataAccessConfig(this.myConfig.baseUrl(),
				this.myConfig.dataInstanceName(), COMPONENT_ID);

		this.dataToChannelMapper = new PlcNextGdsDataToChannelMapperImpl();

		var createSessionEndpointUrl = PlcNextUrlStringHelper.buildUrlString(this.dataProviderConfig.dataUrl(),
				PlcNextGdsDataProvider.PATH_SESSIONS);
		var createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", SESSION_ID);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);
		when(this.mockDummyDataBridgeHttp.requestJson(//
				argThat(arg -> Objects.nonNull(arg) //
						&& arg.method() == HttpMethod.POST //
						&& arg.url().startsWith(createSessionEndpointUrl)))) //
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		var maintainSessionEndpointUrl = new StringBuilder(PlcNextUrlStringHelper
				.buildUrlString(this.dataProviderConfig.dataUrl(), PlcNextGdsDataProvider.PATH_SESSIONS))//
				.append("/").append(SESSION_ID).toString();
		var maintainSessionResponseBody = new JsonObject();
		maintainSessionResponseBody.addProperty("sessionID", SESSION_ID);
		when(this.mockDummyDataBridgeHttp.requestJson(//
				argThat(arg -> Objects.nonNull(arg) //
						&& arg.method() == HttpMethod.POST //
						&& arg.url().startsWith(maintainSessionEndpointUrl)))) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));

		this.test = new ComponentTest(this.componentUnderTest) //
				.addReference("gdsDataProvider", this.dataProvider) //
				.addReference("gdsDataToChannelMapper", this.dataToChannelMapper)
				.addReference("configAdmin", new DummyConfigurationAdmin());
	}

	@Test
	public void testRunModuleSuccessfully() throws Exception {
		// prep
		var variables = new JsonArray();

		var expectedMaxPowerExportValue = 110001;
		var varMaxPowerExport = new JsonObject();
		varMaxPowerExport.addProperty("path", this.myConfig.dataInstanceName() + "MaxPowerExport");
		varMaxPowerExport.addProperty("value", expectedMaxPowerExportValue);
		variables.add(varMaxPowerExport);

		var expectedMaxPowerImportValue = 210001;
		var varMaxPowerImport = new JsonObject();
		varMaxPowerImport.addProperty("path", this.myConfig.dataInstanceName() + "MaxPowerImport");
		varMaxPowerImport.addProperty("value", expectedMaxPowerImportValue);
		variables.add(varMaxPowerImport);

		var expectedReactivePowerValue = 320001;
		var varSetReactivePower = new JsonObject();
		varSetReactivePower.addProperty("path", this.myConfig.dataInstanceName() + "MaxReactivePower");
		varSetReactivePower.addProperty("value", expectedReactivePowerValue);
		variables.add(varSetReactivePower);

		var responseBody = new JsonObject();
		responseBody.add("variables", variables);

		String dataEndpointUrl = PlcNextUrlStringHelper.buildUrlString(this.dataProviderConfig.dataUrl(),
				PlcNextGdsDataProvider.PATH_VARIABLES);
		when(this.mockDummyDataBridgeHttp.requestJson(//
				argThat(arg -> Objects.nonNull(arg) //
						&& arg.method() == HttpMethod.POST //
						&& arg.url().equals(dataEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(responseBody)));

		// test + check
		this.test //
				.activate(this.myConfig)

				.next(new TestCase("Trigger value consumption and do 10 wait cycles"), 10) //
				.next(new TestCase("Check requested data dropped in asynchronously")
						.onAfterProcessImage(assertChannelValue(//
								this.componentUnderTest, PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_EXPORT,
								expectedMaxPowerExportValue)) //
						.onAfterProcessImage(assertChannelValue(//
								this.componentUnderTest, PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_IMPORT,
								expectedMaxPowerImportValue)) //
						.onAfterProcessImage(assertChannelValue(//
								this.componentUnderTest, PlcNextLoadCircuit.ChannelId.MAX_REACTIVE_POWER,
								expectedReactivePowerValue)))

				.deactivate();
	}
}
