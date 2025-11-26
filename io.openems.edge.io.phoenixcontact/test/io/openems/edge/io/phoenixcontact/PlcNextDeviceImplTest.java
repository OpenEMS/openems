package io.openems.edge.io.phoenixcontact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.bridge.http.BridgeHttpImpl;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceImpl;
import io.openems.common.types.HttpStatus;
import io.openems.edge.bridge.http.cycle.CycleSubscriber;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.io.phoenixcontact.auth.PlcNextTokenManager;
import io.openems.edge.io.phoenixcontact.gds.PlcNextApiCommand;
import io.openems.edge.io.phoenixcontact.gds.PlcNextGdsProvider;
import io.openems.edge.io.phoenixcontact.gds.PlcNextReadFromApiResourceCommand;

public class PlcNextDeviceImplTest {

	private static final String COMPONENT_ID = "component0";
	
	private TestConfig myConfig;
	
	private BridgeHttp dummyAuthBridgeHttp;
	private BridgeHttp dummyDataBridgeHttp;
	
	private PlcNextAuthClient authClient;
	private PlcNextDataClient dataClient;
	
	private PlcNextTokenManager tokenManager;
	private PlcNextGdsProvider dataProvider;

	private CycleSubscriber cycleSubscriber;
	
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
				return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(), "{'jwtToken': 'dummy'}"));
			}
			
			@Override
			public <T extends HttpBridgeService> T createService(HttpBridgeServiceDefinition<T> serviceDefinition) {
				return (T)new HttpBridgeTimeServiceImpl(this, new DummyBridgeHttpExecutor(), new DummyEndpointFetcher());
			}
		};
		this.dummyDataBridgeHttp = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(), "{'read_test_value': 123}"));
			}			
		};
		
		this.authClient = new PlcNextAuthClient(dummyAuthBridgeHttp, myConfig);
		this.tokenManager = new PlcNextTokenManager(this.authClient);
		
		this.dataClient = new PlcNextDataClient(dummyDataBridgeHttp, tokenManager, myConfig);
		this.dataProvider = new PlcNextGdsProvider(this.dataClient);
		this.dataProvider.setPlcNextDeviceComponent(componentUnderTest);
		
		this.cycleSubscriber = new CycleSubscriber();
	}
	
	
	// WIP: make dummy request return sth.
	@Test
	public void test() throws Exception {
		ComponentTest test = new ComponentTest(componentUnderTest) //
				.addReference("gdsProvider", this.dataProvider) //
				.addReference("cycleSubscriber", this.cycleSubscriber)
				.activate(this.myConfig); //
		
		test.next(new TestCase()); //
		
		test.deactivate();
	}
}
