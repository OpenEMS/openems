package io.openems.edge.controller.api.backend.services;

import io.openems.common.utils.BehaviorSubject;
import io.openems.edge.controller.api.backend.api.DataFetchType;

public class DummyBackendConnectionStateListener implements BackendConnectionStateListener {

	private final BehaviorSubject<BackendConnectionState> connectionState = new BehaviorSubject<>(
			new BackendConnectionState.StillInitializing());
	private final BehaviorSubject<DataFetchType> dataFetchType = new BehaviorSubject<>(DataFetchType.WAITING);

	public void setConnectionState(BackendConnectionState connectionState) {
		this.connectionState.setValue(connectionState);
		this.dataFetchType.setValue(connectionState.dataFetchType());
	}

	@Override
	public BehaviorSubject<BackendConnectionState> getConnectionState() {
		return this.connectionState;
	}

	@Override
	public BehaviorSubject<DataFetchType> getDataFetchType() {
		return this.dataFetchType;
	}
}
