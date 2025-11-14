package io.openems.edge.io.phoenixcontact;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.bridge.http.api.BridgeHttp;
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
	
	private BridgeHttp dummyBridgeHttp;
	
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
		
		this.dummyBridgeHttp = new DummyBridgeHttp();
		
		this.authClient = new PlcNextAuthClient(dummyBridgeHttp, myConfig);
		this.tokenManager = new PlcNextTokenManager(this.authClient);
		
		this.dataClient = new PlcNextDataClient(dummyBridgeHttp, tokenManager, myConfig);
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
		
		test.next(new TestCase()
				.onBeforeProcessImage(null)); //
		
		test.deactivate();
	}
}
