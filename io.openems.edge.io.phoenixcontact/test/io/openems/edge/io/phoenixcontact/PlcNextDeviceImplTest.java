package io.openems.edge.io.phoenixcontact;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.io.phoenixcontact.auth.PlcNextAuthClient;
import io.openems.edge.io.phoenixcontact.auth.PlcNextTokenManager;
import io.openems.edge.io.phoenixcontact.gds.PlcNextGdsDataClient;
import io.openems.edge.io.phoenixcontact.gds.PlcNextGdsProvider;

public class PlcNextDeviceImplTest {

	private static final String COMPONENT_ID = "component0";
	
	private TestConfig myConfig;
	
	private BridgeHttp dummyAuthBridgeHttp;
	private BridgeHttp dummyDataBridgeHttp;
	
	private PlcNextAuthClient authClient;
	private PlcNextGdsDataClient dataClient;
	
	private PlcNextTokenManager tokenManager;
	private PlcNextGdsProvider dataProvider;

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
				if (endpoint.url().contains(PlcNextAuthClient.PATH_AUTH_TOKEN)) {
					return CompletableFuture.supplyAsync(
							() -> new HttpResponse<String>(HttpStatus.OK, Map.of(), "{'code': 'dummy_auth'}"));
				} else if (endpoint.url().contains(PlcNextAuthClient.PATH_ACCESS_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
							"{'access_token': 'dummy_access'}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};
		this.dummyDataBridgeHttp = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(), "{'read_test_value': 123}"));
			}			
		};
		
		this.authClient = new PlcNextAuthClient(dummyAuthBridgeHttp);
		this.tokenManager = new PlcNextTokenManager(this.authClient);
		
		this.dataClient = new PlcNextGdsDataClient(dummyDataBridgeHttp, tokenManager);
		this.dataProvider = new PlcNextGdsProvider(this.dataClient);
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
