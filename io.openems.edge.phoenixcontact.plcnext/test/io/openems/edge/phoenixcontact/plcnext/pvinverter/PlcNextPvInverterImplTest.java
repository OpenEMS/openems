package io.openems.edge.phoenixcontact.plcnext.pvinverter;

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

	private static ThrowingRunnable<Exception> assertIntegerWriteChannelValue(PlcNextPvInverterImpl sut, ChannelId channelId,
			Object expectedValue) {
		return () -> assertEquals(expectedValue, ((IntegerWriteChannel)sut.channel(channelId)).getNextWriteValue().get());
	}

	private TestConfig myConfig;

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
	
	@Before
	public void setupBefore() throws Exception {
		this.myConfig = TestConfig.create() //
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

		this.channelToDataMapper = new PlcNextChannelToGdsDataMapperImpl();
		this.dataToChannelMapper = new PlcNextGdsDataToChannelMapperImpl();

		this.dataProviderConfig = new PlcNextGdsDataAccessConfig(myConfig.baseUrl(), myConfig.dataInstanceName(),
				COMPONENT_ID);

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
				.addReference("gdsChannelToGdsDataMapper", this.channelToDataMapper);
	}

	@Test
	public void testRunModuleSuccessfully() throws Exception {
		// prep
		int expectedPhases2Neutral1Value = 110000;
		int expectedPhases2Neutral2Value = 220000;
		int expectedPhases2Neutral3Value = 330000;
		int expectedPhasesNeutralValue = 550000;
		int expectedEnergyImportValue = 440000;
		int setActivePowerEqualsValue = 140002;

		//// Read
		JsonObject readDataResponseBody = new JsonObject();
		JsonArray variables = new JsonArray();

		JsonObject varPhaseVoltageL1N = new JsonObject();
		varPhaseVoltageL1N.addProperty("path", "OpenEMS_V1Component1/" + myConfig.dataInstanceName()
				+ ".udtIn.electricityMeter.voltageMeasurement.phasesToNeutral.L1N");
		varPhaseVoltageL1N.addProperty("value", expectedPhases2Neutral1Value);
		variables.add(varPhaseVoltageL1N);

		JsonObject varPhaseVoltageL2N = new JsonObject();
		varPhaseVoltageL2N.addProperty("path", "OpenEMS_V1Component1/" + myConfig.dataInstanceName()
				+ ".udtIn.electricityMeter.voltageMeasurement.phasesToNeutral.L2N");
		varPhaseVoltageL2N.addProperty("value", expectedPhases2Neutral2Value);
		variables.add(varPhaseVoltageL2N);

		JsonObject varPhaseVoltageL3N = new JsonObject();
		varPhaseVoltageL3N.addProperty("path", "OpenEMS_V1Component1/" + myConfig.dataInstanceName()
				+ ".udtIn.electricityMeter.voltageMeasurement.phasesToNeutral.L3N");
		varPhaseVoltageL3N.addProperty("value", expectedPhases2Neutral3Value);
		variables.add(varPhaseVoltageL3N);

		JsonObject varNeutralCurrent = new JsonObject();
		varNeutralCurrent.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.electricityMeter.currentMeasurement.phases.Neutral");
		varNeutralCurrent.addProperty("value", expectedPhasesNeutralValue);
		variables.add(varNeutralCurrent);

		JsonObject varEnergyImport = new JsonObject();
		varEnergyImport.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.electricityMeter.energyMeasurement.EnergyImport");
		varEnergyImport.addProperty("value", expectedEnergyImportValue);
		variables.add(varEnergyImport);

		readDataResponseBody.add("variables", variables);

		String dataEndpointUrl = PlcNextUrlStringHelper.buildUrlString(
				dataProviderConfig.dataUrl(), PlcNextGdsDataProvider.PATH_VARIABLES);
		when(mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg) && //
					arg.method() == HttpMethod.POST && //
					arg.url().equals(dataEndpointUrl)))) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(readDataResponseBody)));

		//// Write
		JsonObject requestBodyVarSetActivePowerEquals = new JsonObject();
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_PATH,
				PlcNextGdsDataProvider.PLC_NEXT_OPENEMS_COMPONENT_NAME + "/" + //
						myConfig.dataInstanceName() + "." + PlcNextGdsDataProvider.PLC_NEXT_OUTPUT_CHANNEL + "." + //
						PlcNextPvInverterGdsDataWriteMappingDefinition.SET_ACTIVE_POWER.getIdentifier());
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE_TYPE,
				PlcNextGdsDataWriteValueType.VARIABLE.getIdentifier());
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE,
				setActivePowerEqualsValue);

		JsonObject writeDataResponseBody = new JsonObject();
		writeDataResponseBody.addProperty("apiVersion", "n/a");
		writeDataResponseBody.addProperty("projectCRC", "1234567890");
		writeDataResponseBody.addProperty("userAuthenticationRequired", "true");

		JsonArray writeVariables = new JsonArray();
		writeVariables.add(requestBodyVarSetActivePowerEquals);

		writeDataResponseBody.add(PlcNextGdsDataProvider.PLC_NEXT_VARIABLES, writeVariables);

		when(mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg) && //
					arg.method() == HttpMethod.PUT && //
					arg.url().equals(dataEndpointUrl)))) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(writeDataResponseBody)));

		// test + check
		this.test.activate(myConfig); //

		this.test.next(new TestCase("Trigger value consumption and check write value") //
					.input(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, setActivePowerEqualsValue)
					.onBeforeWriteCallbacks(
							assertIntegerWriteChannelValue(componentUnderTest, 
									ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, 
									setActivePowerEqualsValue)))
				.next(new TestCase("Check requested data dropped in asynchronously")
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

		this.test.deactivate();
	}
}
