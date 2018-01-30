package io.openems.backend.browserwebsocket.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.backend.browserwebsocket.provider.internal.BrowserWebsocket;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.openemswebsocket.api.OpenemsWebsocketService;
import io.openems.backend.timedata.api.TimedataService;
import io.openems.common.exceptions.OpenemsException;

import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = BrowserWebsocketProvider.Config.class, factory = false)
@Component(name = "BrowserWebsocket", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BrowserWebsocketProvider {

	@ObjectClassDefinition
	@interface Config {
		int port();
	}

	@Activate
	void activate(Config config) {
		System.out.println("Activate BrowserWebsocket");
		try {
			this.browserWebsocket = new BrowserWebsocket(config.port());
		} catch (OpenemsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Deactivate
	void deactivate() {
		System.out.println("Deactivate BrowserWebsocket");
	}

	private BrowserWebsocket browserWebsocket = null;

	@Reference
	void setOpenemsWebsocketService(OpenemsWebsocketService openemsWebsocketService) {
		this.browserWebsocket.setOpenemsWebsocketService(openemsWebsocketService);
	}
	
	@Reference
	void setMetadataService(MetadataService metadataService) {
		this.browserWebsocket.setMetadataService(metadataService);
	}
	
	@Reference
	void setTimedataService(TimedataService timedataService) {
		this.browserWebsocket.setTimedataService(timedataService);
	}
}
