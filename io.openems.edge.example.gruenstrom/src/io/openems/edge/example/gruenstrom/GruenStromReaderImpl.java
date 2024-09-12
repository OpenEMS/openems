package io.openems.edge.example.gruenstrom;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GruenStrom.Reader", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class GruenStromReaderImpl extends AbstractOpenemsComponent implements OpenemsComponent, GruenStromReader {

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public GruenStromReaderImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				GruenStromReader.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.httpBridge = this.httpBridgeFactory.get();
		// Hard code channel value so we can read it in UI
		this.getGreenLevelChannel().setNextValue(54);

		
		
		// TODO: Implement api

		//		this.httpBridge.subscribeCycle(30, new Endpoint(//
		//				null,// Url
		//				null, //
		//				0, //
		//				0, //
		//				null,//
		//				null //
		//				), null, null);
		//	
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

}
