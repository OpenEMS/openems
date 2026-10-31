package io.openems.edge.controller.api.backend.services;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.openems.common.logger.DebugLoggable;
import io.openems.common.timedata.Timeout;
import io.openems.common.utils.BehaviorSubject;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;
import io.openems.edge.controller.api.backend.api.DataFetchType;

@Component(immediate = true)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})
public class BackendConnectionStateListenerImpl implements BackendConnectionStateListener, EventHandler, DebugLoggable {
	private final Logger log = LoggerFactory.getLogger(BackendConnectionStateListenerImpl.class);
	private final BehaviorSubject<BackendConnectionState> connectionState = new BehaviorSubject<>(
			new BackendConnectionState.StillInitializing());
	private final BehaviorSubject<DataFetchType> dataFetchType = new BehaviorSubject<>(DataFetchType.WAITING);
	private final List<ControllerApiBackend> backends = Collections.synchronizedList(new ArrayList<>());

	private final Timeout initializeTime = Timeout.ofSeconds(100);
	private Stopwatch noConnectionWatch;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = DYNAMIC, cardinality = MULTIPLE, target = "(&(enabled=true)(|(useDefaultFeatures=true)(supportedFeatures=DATA_PROVIDER)))")
	protected void bindBackend(ControllerApiBackend backend) {
		this.backends.add(backend);
	}

	protected void unbindBackend(ControllerApiBackend backend) {
		this.backends.remove(backend);
	}

	@Activate
	protected void activate(ComponentContext context) {
		this.initializeTime.start(this.componentManager.getClock());

		this.updateStatus();
	}

	@Override
	public BehaviorSubject<BackendConnectionState> getConnectionState() {
		return this.connectionState;
	}

	@Override
	public BehaviorSubject<DataFetchType> getDataFetchType() {
		return this.dataFetchType;
	}

	@Override
	public String debugLog() {
		return String.format("State:%s|DataFetch:%s", this.connectionState.getValue(), this.dataFetchType.getValue());
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_CONTROLLERS -> this.updateStatus();
		}
	}

	protected void updateStatus() {
		var newState = this.calculateBackendState();
		if (!newState.equals(this.connectionState.getValue())) {
			this.connectionState.setValue(newState);

			var newDataFetchType = newState.dataFetchType();
			if (!newDataFetchType.equals(this.dataFetchType.getValue())) {
				this.log.info("Changed DataFetchType from {} to {}", this.dataFetchType.getValue(), newDataFetchType);
				this.dataFetchType.setValue(newDataFetchType);
			}
		}
	}

	private BackendConnectionState calculateBackendState() {
		var state = calculateBackendStateFromBackends(this.backends);
		return switch (state) {
		case BackendConnectionState.Connected() -> {
			this.noConnectionWatch = null;
			yield state;
		}
		case BackendConnectionState.NoConnection(var ignored) -> {
			if (this.noConnectionWatch == null) {
				this.noConnectionWatch = Stopwatch.createStarted();
			}

			if (this.isStillInInitializePeriod()) {
				yield new BackendConnectionState.StillInitializing();
			} else {
				yield new BackendConnectionState.NoConnection(this.noConnectionWatch.elapsed());
			}
		}
		default -> {
			this.noConnectionWatch = null;
			if (this.isStillInInitializePeriod()) {
				yield new BackendConnectionState.StillInitializing();
			} else {
				yield state;
			}
		}
		};
	}

	private boolean isStillInInitializePeriod() {
		return !this.initializeTime.elapsed(this.componentManager.getClock());
	}

	private static BackendConnectionState calculateBackendStateFromBackends(List<ControllerApiBackend> backends) {
		if (backends.isEmpty()) {
			return new BackendConnectionState.NoBackendInstance();
		}

		var connectedBackends = backends.stream().filter(ControllerApiBackend::isConnected).toList();
		if (connectedBackends.isEmpty()) {
			return new BackendConnectionState.NoConnection(null);
		}

		return new BackendConnectionState.Connected();
	}
}
