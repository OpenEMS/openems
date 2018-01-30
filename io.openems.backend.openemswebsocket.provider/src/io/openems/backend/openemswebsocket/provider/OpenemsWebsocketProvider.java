package io.openems.backend.openemswebsocket.provider;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.google.gson.JsonObject;

import io.openems.backend.browserwebsocket.api.BrowserWebsocketService;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.openemswebsocket.api.OpenemsWebsocketService;
import io.openems.backend.openemswebsocket.provider.internal.OpenemsSession;
import io.openems.backend.openemswebsocket.provider.internal.OpenemsWebsocket;
import io.openems.backend.timedata.api.TimedataService;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.websocket.WebSocketUtils;

import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = OpenemsWebsocketProvider.Config.class, factory = false)
@Component(name = "OpenemsWebsocket", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class OpenemsWebsocketProvider implements OpenemsWebsocketService {

	@ObjectClassDefinition
	@interface Config {
		int port();
	}

	@Activate
	void activate(Config config) {
		System.out.println("Activate OpenemsWebsocket");
		this.openemsWebsocket = new OpenemsWebsocket(config.port());
	}

	@Deactivate
	void deactivate() {
		System.out.println("Deactivate OpenemsWebsocket");
	}

	private OpenemsWebsocket openemsWebsocket = null;

	@Reference
	void setMetadataService(MetadataService metadataService) {
		this.openemsWebsocket.setMetadataService(metadataService);
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL) // avoids recursive dependency
	void setBrowserWebsocketService(BrowserWebsocketService browserWebsocketService) {
		this.openemsWebsocket.setBrowserWebsocketService(browserWebsocketService);
	}

	@Reference
	void setTimedataService(TimedataService timedataService) {
		this.openemsWebsocket.setTimedataService(timedataService);
	}

	@Override
	public boolean isWebsocketConnected(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Optional<JsonObject> getConfig(String deviceName) {
		Optional<OpenemsSession> openemsSessionOpt = this.openemsWebsocket.getOpenemsSession(deviceName);
		if (openemsSessionOpt.isPresent()) {
			return openemsSessionOpt.get().getData().getOpenemsConfigOpt();
		}
		return Optional.empty();
	}

	@Override
	public void send(String deviceName, JsonObject j) throws OpenemsException {
		Optional<WebSocket> openemsWebsocketOpt = this.openemsWebsocket.getOpenemsWebsocket(deviceName);
		if (openemsWebsocketOpt.isPresent()) {
			WebSocket openemsWebsocket = openemsWebsocketOpt.get();
			if (WebSocketUtils.send(openemsWebsocket, j)) {
				return;
			} else {
				throw new OpenemsException("Sending failed");
			}
		} else {
			throw new OpenemsException("Device is not connected.");
		}
	}
}
