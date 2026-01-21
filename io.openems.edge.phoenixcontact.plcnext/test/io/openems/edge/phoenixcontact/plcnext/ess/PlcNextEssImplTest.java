package io.openems.edge.phoenixcontact.plcnext.ess;

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
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManagerImpl;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataAccessConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProviderImpl;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataWriteValueType;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextChannelToGdsDataMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextChannelToGdsDataMapperImpl;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapperImpl;

public class PlcNextEssImplTest {

	private static final String COMPONENT_ID = "ess0";
	private static final String SESSION_ID = "1234567890";

	private static ThrowingRunnable<Exception> assertChannelValue(PlcNextEssImpl sut, ChannelId channelId,
			Object expectedValue) {
		return () -> assertEquals(expectedValue, sut.channel(channelId).value().get());
	}

	private TestConfig myConfig;

	private BridgeHttp dummyAuthBridgeHttp;
	private BridgeHttp mockDummyDataBridgeHttp;

	private PlcNextTokenManager tokenManager;

	private PlcNextGdsDataAccessConfig dataProviderConfig;
	private PlcNextGdsDataProviderImpl dataProvider;

	private PlcNextGdsDataToChannelMapper dataToChannelMapper;
	private PlcNextChannelToGdsDataMapper channelToDataMapper;

	private Power dummyPower;

	private PlcNextEssImpl componentUnderTest;
	private ComponentTest test;

	private String accessToken;

	@Before
	public void setupBefore() throws Exception {
		this.myConfig = TestConfig.create() //
				.setId(COMPONENT_ID) //
				.build();
		this.componentUnderTest = new PlcNextEssImpl();

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

		this.dummyPower = new DummyPower();

		this.test = new ComponentTest(componentUnderTest) //
				.addReference("gdsDataProvider", this.dataProvider) //
				.addReference("gdsDataToChannelMapper", this.dataToChannelMapper) //
				.addReference("gdsChannelToGdsDataMapper", this.channelToDataMapper) //
				.addReference("power", dummyPower);
	}

	@Test
	public void testRunModuleSuccessfully() throws Exception {
		// prep
		int expectedSocValue = 110001;
		int expectedCapacityValue = 210001;
		int setActivePowerEqualsValue = 140002;
		GridMode gridModeValue = GridMode.ON_GRID;
		int expectedGridModeValue = gridModeValue.getValue();

		//// Read
		JsonObject readDataResponseBody = new JsonObject();
		JsonArray variables = new JsonArray();

		JsonObject varMaxPowerExport = new JsonObject();
		varMaxPowerExport.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.essMeter.Soc");
		varMaxPowerExport.addProperty("value", expectedSocValue);
		variables.add(varMaxPowerExport);

		JsonObject varMaxPowerImport = new JsonObject();
		varMaxPowerImport.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.essMeter.Capacity");
		varMaxPowerImport.addProperty("value", expectedCapacityValue);
		variables.add(varMaxPowerImport);

		JsonObject varSetReactivePower = new JsonObject();
		varSetReactivePower.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.essMeter.GridMode");
		varSetReactivePower.addProperty("value", gridModeValue.getName());
		variables.add(varSetReactivePower);

		readDataResponseBody.add("variables", variables);

		List<String> readVariableIdentifiers = Stream.of(PlcNextEssGdsDataReadMappingDefinition.values())//
				.map(PlcNextGdsDataMappingDefinition::getIdentifier).toList();
		String readDataRequestBody = this.dataProvider.buildPostBodyForRead(SESSION_ID, readVariableIdentifiers,
				dataProviderConfig);
		Endpoint readDataEndpoint = this.dataProvider.buildDataEndpointRepresentation(this.accessToken, HttpMethod.POST,
				readDataRequestBody, this.dataProviderConfig);
		when(mockDummyDataBridgeHttp.requestJson(readDataEndpoint)) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(readDataResponseBody)));

		//// Write
		JsonObject requestBodyVarSetActivePowerEquals = new JsonObject();
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_PATH,
				PlcNextGdsDataProvider.PLC_NEXT_OPENEMS_COMPONENT_NAME + "/" + //
						myConfig.dataInstanceName() + "." + PlcNextGdsDataProvider.PLC_NEXT_OUTPUT_CHANNEL + "." + //
						PlcNextEssGdsDataWriteMappingDefinition.SET_ACTIVE_POWER_EQUALS.getIdentifier());
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

		String writeDataRequestBody = this.dataProvider.buildPutBodyForWrite(SESSION_ID,
				List.of(requestBodyVarSetActivePowerEquals));
		Endpoint writeDataEndpoint = this.dataProvider.buildDataEndpointRepresentation(accessToken, HttpMethod.PUT,
				writeDataRequestBody, dataProviderConfig);
		when(mockDummyDataBridgeHttp.requestJson(writeDataEndpoint)) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(writeDataResponseBody)));

		// test + check
		this.test.activate(myConfig); //

		this.test.next(new TestCase() //
				.input(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, setActivePowerEqualsValue)
				.onAfterProcessImage(
						assertChannelValue(componentUnderTest, SymmetricEss.ChannelId.SOC, expectedSocValue)) //
				.onAfterProcessImage(
						assertChannelValue(componentUnderTest, SymmetricEss.ChannelId.CAPACITY, expectedCapacityValue)) //
				.onAfterProcessImage(assertChannelValue(componentUnderTest, SymmetricEss.ChannelId.GRID_MODE,
						expectedGridModeValue))); //

		this.test.deactivate();
	}


	@Test
	public void testRunModuleGridModeWrongEnum() throws Exception {
		// prep
		int expectedSocValue = 110001;
		int expectedCapacityValue = 210001;
		int setActivePowerEqualsValue = 140002;
		String gridModeValue = "OffGrid";
		int expectedGridModeValue = GridMode.UNDEFINED.getValue();

		//// Read
		JsonObject readDataResponseBody = new JsonObject();
		JsonArray variables = new JsonArray();

		JsonObject varMaxPowerExport = new JsonObject();
		varMaxPowerExport.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.essMeter.Soc");
		varMaxPowerExport.addProperty("value", expectedSocValue);
		variables.add(varMaxPowerExport);

		JsonObject varMaxPowerImport = new JsonObject();
		varMaxPowerImport.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.essMeter.Capacity");
		varMaxPowerImport.addProperty("value", expectedCapacityValue);
		variables.add(varMaxPowerImport);

		JsonObject varSetReactivePower = new JsonObject();
		varSetReactivePower.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.essMeter.GridMode");
		varSetReactivePower.addProperty("value", gridModeValue);
		variables.add(varSetReactivePower);

		readDataResponseBody.add("variables", variables);

		List<String> readVariableIdentifiers = Stream.of(PlcNextEssGdsDataReadMappingDefinition.values())//
				.map(PlcNextGdsDataMappingDefinition::getIdentifier).toList();
		String readDataRequestBody = this.dataProvider.buildPostBodyForRead(SESSION_ID, readVariableIdentifiers,
				dataProviderConfig);
		Endpoint readDataEndpoint = this.dataProvider.buildDataEndpointRepresentation(this.accessToken, HttpMethod.POST,
				readDataRequestBody, this.dataProviderConfig);
		when(mockDummyDataBridgeHttp.requestJson(readDataEndpoint)) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(readDataResponseBody)));

		//// Write
		JsonObject requestBodyVarSetActivePowerEquals = new JsonObject();
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_PATH,
				PlcNextGdsDataProvider.PLC_NEXT_OPENEMS_COMPONENT_NAME + "/" + //
						myConfig.dataInstanceName() + "." + PlcNextGdsDataProvider.PLC_NEXT_OUTPUT_CHANNEL + "." + //
						PlcNextEssGdsDataWriteMappingDefinition.SET_ACTIVE_POWER_EQUALS.getIdentifier());
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

		String writeDataRequestBody = this.dataProvider.buildPutBodyForWrite(SESSION_ID,
				List.of(requestBodyVarSetActivePowerEquals));
		Endpoint writeDataEndpoint = this.dataProvider.buildDataEndpointRepresentation(accessToken, HttpMethod.PUT,
				writeDataRequestBody, dataProviderConfig);
		when(mockDummyDataBridgeHttp.requestJson(writeDataEndpoint)) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(writeDataResponseBody)));

		// test + check
		this.test.activate(myConfig); //

		this.test.next(new TestCase() //
				.input(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, setActivePowerEqualsValue)
				.onAfterProcessImage(
						assertChannelValue(componentUnderTest, SymmetricEss.ChannelId.SOC, expectedSocValue)) //
				.onAfterProcessImage(
						assertChannelValue(componentUnderTest, SymmetricEss.ChannelId.CAPACITY, expectedCapacityValue)) //
				.onAfterProcessImage(assertChannelValue(componentUnderTest, SymmetricEss.ChannelId.GRID_MODE,
						expectedGridModeValue))); //

		this.test.deactivate();
	}

	@Test
	public void testRunModuleGridModeWrongInt() throws Exception {
		// prep
		int expectedSocValue = 110001;
		int expectedCapacityValue = 210001;
		int setActivePowerEqualsValue = 140002;
		int gridModeValue = 345678;
		int expectedGridModeValue = GridMode.UNDEFINED.getValue();

		//// Read
		JsonObject readDataResponseBody = new JsonObject();
		JsonArray variables = new JsonArray();

		JsonObject varMaxPowerExport = new JsonObject();
		varMaxPowerExport.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.essMeter.Soc");
		varMaxPowerExport.addProperty("value", expectedSocValue);
		variables.add(varMaxPowerExport);

		JsonObject varMaxPowerImport = new JsonObject();
		varMaxPowerImport.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.essMeter.Capacity");
		varMaxPowerImport.addProperty("value", expectedCapacityValue);
		variables.add(varMaxPowerImport);

		JsonObject varSetReactivePower = new JsonObject();
		varSetReactivePower.addProperty("path",
				"OpenEMS_V1Component1/" + myConfig.dataInstanceName() + ".udtIn.essMeter.GridMode");
		varSetReactivePower.addProperty("value", gridModeValue);
		variables.add(varSetReactivePower);

		readDataResponseBody.add("variables", variables);

		List<String> readVariableIdentifiers = Stream.of(PlcNextEssGdsDataReadMappingDefinition.values())//
				.map(PlcNextGdsDataMappingDefinition::getIdentifier).toList();
		String readDataRequestBody = this.dataProvider.buildPostBodyForRead(SESSION_ID, readVariableIdentifiers,
				dataProviderConfig);
		Endpoint readDataEndpoint = this.dataProvider.buildDataEndpointRepresentation(this.accessToken, HttpMethod.POST,
				readDataRequestBody, this.dataProviderConfig);
		when(mockDummyDataBridgeHttp.requestJson(readDataEndpoint)) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(readDataResponseBody)));

		//// Write
		JsonObject requestBodyVarSetActivePowerEquals = new JsonObject();
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_PATH,
				PlcNextGdsDataProvider.PLC_NEXT_OPENEMS_COMPONENT_NAME + "/" + //
						myConfig.dataInstanceName() + "." + PlcNextGdsDataProvider.PLC_NEXT_OUTPUT_CHANNEL + "." + //
						PlcNextEssGdsDataWriteMappingDefinition.SET_ACTIVE_POWER_EQUALS.getIdentifier());
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

		String writeDataRequestBody = this.dataProvider.buildPutBodyForWrite(SESSION_ID,
				List.of(requestBodyVarSetActivePowerEquals));
		Endpoint writeDataEndpoint = this.dataProvider.buildDataEndpointRepresentation(accessToken, HttpMethod.PUT,
				writeDataRequestBody, dataProviderConfig);
		when(mockDummyDataBridgeHttp.requestJson(writeDataEndpoint)) //
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(writeDataResponseBody)));

		// test + check
		this.test.activate(myConfig); //

		this.test.next(new TestCase() //
				.input(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, setActivePowerEqualsValue)
				.onAfterProcessImage(
						assertChannelValue(componentUnderTest, SymmetricEss.ChannelId.SOC, expectedSocValue)) //
				.onAfterProcessImage(
						assertChannelValue(componentUnderTest, SymmetricEss.ChannelId.CAPACITY, expectedCapacityValue)) //
				.onAfterProcessImage(assertChannelValue(componentUnderTest, SymmetricEss.ChannelId.GRID_MODE,
						expectedGridModeValue))); //

		this.test.deactivate();
	}
}
