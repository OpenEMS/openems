package io.openems.edge.phoenixcontact.plcnext.pvinverter;

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
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManagerImpl;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataAccessConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProviderImpl;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataWriteValueType;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextChannelToGdsDataMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextChannelToGdsDataMapperImpl;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapperImpl;
import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextUrlStringHelper;
import io.openems.edge.phoenixcontact.plcnext.meter.PlcNextMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public class PlcNextPvInverterImplTest {

	private static final String COMPONENT_ID = "pvinverter0";
	private static final String SESSION_ID = "1234567890";

	private static ThrowingRunnable<Exception> assertChannelValue(PlcNextPvInverterImpl sut, ChannelId channelId,
			Object expectedValue) {
		return () -> assertEquals(expectedValue, sut.channel(channelId).value().get());
	}

	private static ThrowingRunnable<Exception> assertIntegerWriteChannelValue(PlcNextPvInverterImpl sut,
			ChannelId channelId, Object expectedValue) {
		return () -> assertEquals(expectedValue,
				((IntegerWriteChannel) sut.channel(channelId)).getNextWriteValue().get());
	}

	private MyConfig myConfig;

	private BridgeHttp dummyAuthBridgeHttp;
	private BridgeHttp mockDummyDataBridgeHttp;

	private PlcNextTokenManager tokenManager;

	private PlcNextGdsDataAccessConfig dataProviderConfig;
	private PlcNextGdsDataProviderImpl dataProvider;

	private PlcNextGdsDataToChannelMapper dataToChannelMapper;
	private PlcNextChannelToGdsDataMapper channelToDataMapper;

	private PlcNextPvInverterImpl componentUnderTest;
	private ComponentTest test;

	private String accessToken;

	@BeforeEach
	public void setupBefore() throws Exception {
		this.myConfig = MyConfig.create() //
				.setId(COMPONENT_ID) //
				.build();
		this.componentUnderTest = new PlcNextPvInverterImpl();

		this.accessToken = "dummy_access";

		this.dummyAuthBridgeHttp = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
							"{'code': 'dummy_auth', 'expires_in': 600 }"));
				} else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
							"{'access_token': '" + PlcNextPvInverterImplTest.this.accessToken + "'}"));
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

		this.channelToDataMapper = new PlcNextChannelToGdsDataMapperImpl();
		this.dataToChannelMapper = new PlcNextGdsDataToChannelMapperImpl();

		this.dataProviderConfig = new PlcNextGdsDataAccessConfig(this.myConfig.baseUrl(),
				this.myConfig.dataInstanceName(), COMPONENT_ID);

		var createSessionEndpointUrl = PlcNextUrlStringHelper.buildUrlString(this.dataProviderConfig.dataUrl(),
				PlcNextGdsDataProvider.PATH_SESSIONS);
		var createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", SESSION_ID);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);
		when(this.mockDummyDataBridgeHttp.requestJson(//
				argThat(arg -> Objects.nonNull(arg) //
						&& arg.method() == HttpMethod.POST //
						&& arg.url().startsWith(createSessionEndpointUrl))))
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
						&& arg.url().startsWith(maintainSessionEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));

		this.test = new ComponentTest(this.componentUnderTest) //
				.addReference("gdsDataProvider", this.dataProvider) //
				.addReference("gdsDataToChannelMapper", this.dataToChannelMapper)
				.addReference("gdsChannelToGdsDataMapper", this.channelToDataMapper);
	}

	@Test
	public void testRunModuleSuccessfully() throws Exception {
		// prep

		// -- Read
		var variables = new JsonArray();

		var expectedPhases2Neutral1Value = 110000;
		var varPhaseVoltageL1N = new JsonObject();
		varPhaseVoltageL1N.addProperty("path", this.myConfig.dataInstanceName() + "voltageL1N");
		varPhaseVoltageL1N.addProperty("value", expectedPhases2Neutral1Value);
		variables.add(varPhaseVoltageL1N);

		var expectedPhases2Neutral2Value = 220000;
		var varPhaseVoltageL2N = new JsonObject();
		varPhaseVoltageL2N.addProperty("path", this.myConfig.dataInstanceName() + "voltageL2N");
		varPhaseVoltageL2N.addProperty("value", expectedPhases2Neutral2Value);
		variables.add(varPhaseVoltageL2N);

		var expectedPhases2Neutral3Value = 330000;
		var varPhaseVoltageL3N = new JsonObject();
		varPhaseVoltageL3N.addProperty("path", this.myConfig.dataInstanceName() + "voltageL3N");
		varPhaseVoltageL3N.addProperty("value", expectedPhases2Neutral3Value);
		variables.add(varPhaseVoltageL3N);

		var expectedPhasesNeutralValue = 550000;
		var varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path", this.myConfig.dataInstanceName() + "currentNeutral");
		varNeutralCurrent.addProperty("value", expectedPhasesNeutralValue);
		variables.add(varNeutralCurrent);

		var expectedEnergyImportValue = 440000;
		var varEnergyImport = new JsonObject();
		varEnergyImport.addProperty("path", this.myConfig.dataInstanceName() + "EnergyImport");
		varEnergyImport.addProperty("value", expectedEnergyImportValue);
		variables.add(varEnergyImport);

		var readDataResponseBody = new JsonObject();
		readDataResponseBody.add("variables", variables);

		var dataEndpointUrl = PlcNextUrlStringHelper.buildUrlString(this.dataProviderConfig.dataUrl(),
				PlcNextGdsDataProvider.PATH_VARIABLES);
		when(this.mockDummyDataBridgeHttp.requestJson(//
				argThat(arg -> Objects.nonNull(arg) //
						&& arg.method() == HttpMethod.POST //
						&& arg.url().equals(dataEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(readDataResponseBody)));

		// -- Write
		var setActivePowerEqualsValue = 140002;
		var requestBodyVarSetActivePowerEquals = new JsonObject();
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_PATH,
				"/" + this.myConfig.dataInstanceName()
						+ PlcNextPvInverterGdsDataWriteMappingDefinition.SET_ACTIVE_POWER.getIdentifier());
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE_TYPE,
				PlcNextGdsDataWriteValueType.VARIABLE.getIdentifier());
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE,
				setActivePowerEqualsValue);

		var writeDataResponseBody = new JsonObject();
		writeDataResponseBody.addProperty("apiVersion", "n/a");
		writeDataResponseBody.addProperty("projectCRC", "1234567890");
		writeDataResponseBody.addProperty("userAuthenticationRequired", "true");

		var writeVariables = new JsonArray();
		writeVariables.add(requestBodyVarSetActivePowerEquals);

		writeDataResponseBody.add(PlcNextGdsDataProvider.PLC_NEXT_VARIABLES, writeVariables);

		when(this.mockDummyDataBridgeHttp.requestJson(//
				argThat(arg -> Objects.nonNull(arg) //
						&& arg.method() == HttpMethod.PUT //
						&& arg.url().equals(dataEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(writeDataResponseBody)));

		// test + check
		this.test //
				.activate(this.myConfig)

				.next(new TestCase("Trigger value consumption and check write value") //
						.input(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, setActivePowerEqualsValue)
						.onBeforeWriteCallbacks(assertIntegerWriteChannelValue(this.componentUnderTest,
								ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, setActivePowerEqualsValue)))
				.next(new TestCase("Check requested data dropped in asynchronously")
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest,
								ElectricityMeter.ChannelId.VOLTAGE_L1, expectedPhases2Neutral1Value)) //
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest,
								ElectricityMeter.ChannelId.VOLTAGE_L2, expectedPhases2Neutral2Value)) //
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest,
								ElectricityMeter.ChannelId.VOLTAGE_L3, expectedPhases2Neutral3Value)) //
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest,
								PlcNextMeter.ChannelId.CURRENT_NEUTRAL, expectedPhasesNeutralValue)) //
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest,
								PlcNextMeter.ChannelId.CURRENT_NEUTRAL, expectedPhasesNeutralValue)))

				.deactivate();
	}
}
