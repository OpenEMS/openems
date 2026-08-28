package io.openems.edge.phoenixcontact.plcnext.ess;

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
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProviderImpl;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataWriteValueType;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextChannelToGdsDataMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextChannelToGdsDataMapperImpl;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapperImpl;
import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextUrlStringHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlcNextEssImplTest {

	private static final String COMPONENT_ID = "ess0";
	private static final String SESSION_ID = "1234567890";

	private static ThrowingRunnable<Exception> assertChannelValue(PlcNextEssImpl sut, ChannelId channelId,
			Object expectedValue) {
		return () -> assertEquals(expectedValue, sut.channel(channelId).value().get());
	}

	private static ThrowingRunnable<Exception> assertIntegerWriteChannelValue(PlcNextEssImpl sut, ChannelId channelId,
			Object expectedValue) {
		return () -> assertEquals(expectedValue,
				((IntegerWriteChannel) sut.channel(channelId)).getNextWriteValue().get());
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

		this.dummyAuthBridgeHttp = new PlcNextDummyBridgeHttp(this.accessToken) {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<>(HttpStatus.OK, Map.of(),
							"{'code': 'dummy_auth', 'expires_in': 600 }"));
				} else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<>(HttpStatus.OK, Map.of(),
							"{'access_token': '" + this.accessToken + "'}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};

		this.mockDummyDataBridgeHttp = mock(DummyBridgeHttp.class);
		when(this.mockDummyDataBridgeHttp.createService(any())).thenReturn(
                new HttpBridgeTimeServiceImpl(this.mockDummyDataBridgeHttp, new DummyBridgeHttpExecutor()));

		this.tokenManager = new PlcNextTokenManagerImpl(this.dummyAuthBridgeHttp);
		this.dataProvider = new PlcNextGdsDataProviderImpl(this.mockDummyDataBridgeHttp, this.tokenManager);

		this.channelToDataMapper = new PlcNextChannelToGdsDataMapperImpl();
		this.dataToChannelMapper = new PlcNextGdsDataToChannelMapperImpl();

		this.dataProviderConfig = new PlcNextGdsDataAccessConfig(
                this.myConfig.baseUrl(), this.myConfig.dataInstanceName(), COMPONENT_ID);

		var createSessionEndpointUrl = PlcNextUrlStringHelper.buildUrlString(
                this.dataProviderConfig.dataUrl(),
				PlcNextGdsDataProvider.PATH_SESSIONS);
		var createSessionResponseBody = new JsonObject();
		createSessionResponseBody.addProperty("sessionID", SESSION_ID);
		createSessionResponseBody.addProperty("timeout", PlcNextGdsDataProvider.PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS);
		when(this.mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg)
                && arg.method() == HttpMethod.POST
                && arg.url().startsWith(createSessionEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(
						() -> new HttpResponse<>(HttpStatus.CREATED, Map.of(), createSessionResponseBody)));

		var maintainSessionEndpointUrl = new StringBuilder(PlcNextUrlStringHelper
				.buildUrlString(this.dataProviderConfig.dataUrl(), PlcNextGdsDataProvider.PATH_SESSIONS))//
				.append("/").append(SESSION_ID).toString();
		var maintainSessionResponseBody = new JsonObject();
		maintainSessionResponseBody.addProperty("sessionID", SESSION_ID);
		when(this.mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg)
                && arg.method() == HttpMethod.POST
                && arg.url().startsWith(maintainSessionEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(maintainSessionResponseBody)));

		this.dummyPower = new DummyPower();

		this.test = new ComponentTest(this.componentUnderTest) //
				.addReference("gdsDataProvider", this.dataProvider) //
				.addReference("gdsDataToChannelMapper", this.dataToChannelMapper) //
				.addReference("gdsChannelToGdsDataMapper", this.channelToDataMapper) //
				.addReference("power", this.dummyPower);
	}

	@Test
	public void testRunModuleSuccessfully() throws Exception {
		// prep

		// -- Read
		var variables = new JsonArray();

        var expectedSocValue = 110001;
		var varMaxPowerExport = new JsonObject();
		varMaxPowerExport.addProperty("path", this.myConfig.dataInstanceName() + "Soc");
		varMaxPowerExport.addProperty("value", expectedSocValue);
		variables.add(varMaxPowerExport);

        var expectedCapacityValue = 210001;
		var varMaxPowerImport = new JsonObject();
		varMaxPowerImport.addProperty("path", this.myConfig.dataInstanceName() + "Capacity");
		varMaxPowerImport.addProperty("value", expectedCapacityValue);
		variables.add(varMaxPowerImport);

        var gridModeValue = GridMode.ON_GRID;
		var varSetReactivePower = new JsonObject();
		varSetReactivePower.addProperty("path", this.myConfig.dataInstanceName() + "GridMode");
		varSetReactivePower.addProperty("value", gridModeValue.getName());
		variables.add(varSetReactivePower);

        var readDataResponseBody = new JsonObject();
		readDataResponseBody.add("variables", variables);

		var dataEndpointUrl = PlcNextUrlStringHelper.buildUrlString(this.dataProviderConfig.dataUrl(),
				PlcNextGdsDataProvider.PATH_VARIABLES);
		when(this.mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg)
                && arg.method() == HttpMethod.POST
                && arg.url().equals(dataEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(readDataResponseBody)));

        // -- Write
        var setActivePowerEqualsValue = 140002;
		var requestBodyVarSetActivePowerEquals = new JsonObject();
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_PATH,
				"/" + this.myConfig.dataInstanceName() + PlcNextEssGdsDataWriteMappingDefinition.SET_ACTIVE_POWER_EQUALS.getIdentifier());
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

		when(this.mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg)
                && arg.method() == HttpMethod.PUT
                && arg.url().equals(dataEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(writeDataResponseBody)));

        // test + check
        var expectedGridModeValue = gridModeValue.getValue();

		this.test.activate(this.myConfig);

		this.test.next(new TestCase("Trigger value consumption and check write value") //
				.input(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, setActivePowerEqualsValue)
				.onBeforeWriteCallbacks(assertIntegerWriteChannelValue(this.componentUnderTest,
						ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, setActivePowerEqualsValue)))
				.next(new TestCase("Check requested data dropped in asynchronously")
						.onAfterProcessImage(
								assertChannelValue(this.componentUnderTest, SymmetricEss.ChannelId.SOC, expectedSocValue)) //
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest, SymmetricEss.ChannelId.CAPACITY,
								expectedCapacityValue)) //
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest, SymmetricEss.ChannelId.GRID_MODE,
								expectedGridModeValue))); //

		this.test.deactivate();
	}

	@Test
	public void testRunModuleGridModeWrongEnum() throws Exception {
		// prep

		// -- Read
		var variables = new JsonArray();

        var expectedSocValue = 110001;
		var varMaxPowerExport = new JsonObject();
		varMaxPowerExport.addProperty("path", this.myConfig.dataInstanceName() + "Soc");
		varMaxPowerExport.addProperty("value", expectedSocValue);
		variables.add(varMaxPowerExport);

        var expectedCapacityValue = 210001;
		var varMaxPowerImport = new JsonObject();
		varMaxPowerImport.addProperty("path", this.myConfig.dataInstanceName() + "Capacity");
		varMaxPowerImport.addProperty("value", expectedCapacityValue);
		variables.add(varMaxPowerImport);

        var gridModeValue = "Off@Grid";
		var varSetReactivePower = new JsonObject();
		varSetReactivePower.addProperty("path", this.myConfig.dataInstanceName() + "GridMode");
		varSetReactivePower.addProperty("value", gridModeValue);
		variables.add(varSetReactivePower);

        var readDataResponseBody = new JsonObject();
		readDataResponseBody.add("variables", variables);

		var dataEndpointUrl = PlcNextUrlStringHelper.buildUrlString(this.dataProviderConfig.dataUrl(),
				PlcNextGdsDataProvider.PATH_VARIABLES);
		when(this.mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg)
                && arg.method() == HttpMethod.POST
                && arg.url().equals(dataEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(readDataResponseBody)));

        // -- Write
        var setActivePowerEqualsValue = 140002;
		var requestBodyVarSetActivePowerEquals = new JsonObject();
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_PATH,
				this.myConfig.dataInstanceName() + PlcNextEssGdsDataWriteMappingDefinition.SET_ACTIVE_POWER_EQUALS.getIdentifier());
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

		when(this.mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg)
                && arg.method() == HttpMethod.PUT
                && arg.url().equals(dataEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(writeDataResponseBody)));

        // test + check
        var expectedGridModeValue = GridMode.UNDEFINED.getValue();

        this.test.activate(this.myConfig); //

		this.test.next(new TestCase("Trigger value consumption and check write value") //
				.input(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, setActivePowerEqualsValue)
				.onBeforeWriteCallbacks(assertIntegerWriteChannelValue(this.componentUnderTest,
						ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, setActivePowerEqualsValue)))
				.next(new TestCase("Check requested data dropped in asynchronously")
						.onAfterProcessImage(
								assertChannelValue(this.componentUnderTest, SymmetricEss.ChannelId.SOC, expectedSocValue)) //
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest, SymmetricEss.ChannelId.CAPACITY,
								expectedCapacityValue)) //
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest, SymmetricEss.ChannelId.GRID_MODE,
								expectedGridModeValue))); //

		this.test.deactivate();
	}

	@Test
	public void testRunModuleGridModeWrongInt() throws Exception {
		// prep

		// -- Read
		var variables = new JsonArray();

        var expectedSocValue = 110001;
		var varMaxPowerExport = new JsonObject();
		varMaxPowerExport.addProperty("path", this.myConfig.dataInstanceName() + "Soc");
		varMaxPowerExport.addProperty("value", expectedSocValue);
		variables.add(varMaxPowerExport);

        var expectedCapacityValue = 210001;
		var varMaxPowerImport = new JsonObject();
		varMaxPowerImport.addProperty("path", this.myConfig.dataInstanceName() + "Capacity");
		varMaxPowerImport.addProperty("value", expectedCapacityValue);
		variables.add(varMaxPowerImport);

        var gridModeValue = 345678;
		var varSetReactivePower = new JsonObject();
		varSetReactivePower.addProperty("path", this.myConfig.dataInstanceName() + "GridMode");
		varSetReactivePower.addProperty("value", gridModeValue);
		variables.add(varSetReactivePower);

        var readDataResponseBody = new JsonObject();
		readDataResponseBody.add("variables", variables);

		var dataEndpointUrl = PlcNextUrlStringHelper.buildUrlString(this.dataProviderConfig.dataUrl(),
				PlcNextGdsDataProvider.PATH_VARIABLES);
		when(this.mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg)
                && arg.method() == HttpMethod.POST
                && arg.url().equals(dataEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(readDataResponseBody)));

        // -- Write
        var setActivePowerEqualsValue = 140002;
		var requestBodyVarSetActivePowerEquals = new JsonObject();
		requestBodyVarSetActivePowerEquals.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_PATH,
				"/" + this.myConfig.dataInstanceName() + PlcNextEssGdsDataWriteMappingDefinition.SET_ACTIVE_POWER_EQUALS.getIdentifier());
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

		when(this.mockDummyDataBridgeHttp.requestJson(argThat(arg -> Objects.nonNull(arg)
                && arg.method() == HttpMethod.PUT
                && arg.url().equals(dataEndpointUrl))))
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(writeDataResponseBody)));

        // test + check
        var expectedGridModeValue = GridMode.UNDEFINED.getValue();

		this.test.activate(this.myConfig); //

		this.test.next(new TestCase("Trigger value consumption and check write value") //
				.input(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, setActivePowerEqualsValue)
				.onBeforeWriteCallbacks(assertIntegerWriteChannelValue(this.componentUnderTest,
						ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, setActivePowerEqualsValue)))
				.next(new TestCase("Check requested data dropped in asynchronously")
						.onAfterProcessImage(
								assertChannelValue(this.componentUnderTest, SymmetricEss.ChannelId.SOC, expectedSocValue)) //
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest, SymmetricEss.ChannelId.CAPACITY,
								expectedCapacityValue)) //
						.onAfterProcessImage(assertChannelValue(this.componentUnderTest, SymmetricEss.ChannelId.GRID_MODE,
								expectedGridModeValue))); //

		this.test.deactivate();
	}
}
