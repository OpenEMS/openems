package io.openems.edge.io.phoenixcontact;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.io.phoenixcontact.auth.PlcNextTokenManager;
import io.openems.edge.io.phoenixcontact.gds.PlcNextGdsDataProvider;
import io.openems.edge.io.phoenixcontact.gds.PlcNextGdsDataToChannelMapper;

public class PlcNextDeviceImplTest {

	private static final String COMPONENT_ID = "component0";
	
	private TestConfig myConfig;
	
	private BridgeHttp dummyAuthBridgeHttp;
	private BridgeHttp dummyDataBridgeHttp;
	
	private PlcNextTokenManager tokenManager;
	private PlcNextGdsDataProvider dataProvider;
	private PlcNextGdsDataToChannelMapper dataMapper;
	
	private PlcNextDeviceImpl componentUnderTest;
	
	@Before
	public void setupBefore() {
		this.myConfig = TestConfig.create() //
				.setId(COMPONENT_ID) //
				.build();
		this.componentUnderTest = new PlcNextDeviceImpl();
		
		this.dummyAuthBridgeHttp = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
					return CompletableFuture.supplyAsync(
							() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
									"{'code': 'dummy_auth', 'expires_in': 600 }"));
				} else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
							"{'access_token': 'dummy_access'}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};

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

		this.dummyDataBridgeHttp = Mockito.mock(DummyBridgeHttp.class);
		when(this.dummyDataBridgeHttp.requestJson(any(Endpoint.class)))//
				.thenReturn(CompletableFuture.supplyAsync(() -> HttpResponse.ok(responseBody)));
		
		this.tokenManager = new PlcNextTokenManager(dummyAuthBridgeHttp);
		
		this.dataMapper = new PlcNextGdsDataToChannelMapper();
		this.dataProvider = new PlcNextGdsDataProvider(dummyDataBridgeHttp, this.tokenManager, this.dataMapper);
	}
	
	@Test
	public void testRunModule() throws Exception {
		ComponentTest test = new ComponentTest(componentUnderTest) //
				.addReference("gdsProvider", this.dataProvider) //
				.addReference("tokenManager", this.tokenManager)
				.activate(this.myConfig); //
		
		test.next(new TestCase()); //
		
		test.deactivate();
	}
}
