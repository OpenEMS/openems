package io.openems.edge.bridge.onewire.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import com.dalsemi.onewire.adapter.DSPortAdapter;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.bridge.onewire.BridgeOnewire;
import io.openems.edge.bridge.onewire.jsonrpc.GetDevicesRequest;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bridge.Onewire", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class BridgeOnewireImpl extends AbstractOpenemsComponent implements BridgeOnewire, OpenemsComponent, JsonApi {

	private OneWireTaskWorker taskWorker = null;

	public BridgeOnewireImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeOnewire.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.taskWorker = new OneWireTaskWorker(this, config.port());

		if (config.enabled()) {
			this.taskWorker.activate(config.id());
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.taskWorker != null) {
			this.taskWorker.deactivate();
		}
		super.deactivate();
	}

	@Override
	public void addTask(Consumer<DSPortAdapter> task) {
		if (this.taskWorker != null) {
			this.taskWorker.addTask(task);
		}
	}

	@Override
	public void removeTask(Consumer<DSPortAdapter> task) {
		if (this.taskWorker != null) {
			this.taskWorker.removeTask(task);
		}
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest message)
			throws OpenemsNamedException {
		switch (message.getMethod()) {
		case GetDevicesRequest.METHOD:
			return CompletableFuture.completedFuture(this.taskWorker.handleGetDevicesRequest(message));
		}
		return null;
	}

}