package io.openems.edge.io.phoenixcontact;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.types.HttpStatus;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttp;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
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

	private List<PlcNextApiCommand> apiCommands;
	
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
				return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, "{'jwtToken': 'dummy'}"));
			}
		};
		this.dummyDataBridgeHttp = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, "{'read_test_value': 123}"));
			}			
		};
		
		this.authClient = new PlcNextAuthClient(dummyAuthBridgeHttp, myConfig);
		this.tokenManager = new PlcNextTokenManager(this.authClient);
		
		this.dataClient = new PlcNextDataClient(dummyDataBridgeHttp, tokenManager, myConfig);
		this.dataProvider = new PlcNextGdsProvider(this.dataClient);
		this.dataProvider.setPlcNextDeviceComponent(componentUnderTest);
		
		this.apiCommands = new ArrayList<PlcNextApiCommand>();
		this.apiCommands.add(new PlcNextReadFromApiResourceCommand(this.dataProvider));	
	}
	
	
	// WIP: make dummy request return sth.
	@Test
	public void test() throws Exception {
		ComponentTest test = new ComponentTest(componentUnderTest) //
				.addReference("apiCommands", this.apiCommands) //
				.activate(this.myConfig); //
		
		test.next(new TestCase()); //
		
		test.deactivate();
	}
}
