package io.openems.edge.controller.api.backend.services;

import io.openems.common.utils.BehaviorSubject;
import io.openems.edge.controller.api.backend.api.DataFetchType;

public interface BackendConnectionStateListener {
	/**
	 * Returns a {@link BehaviorSubject} with the current backend connection state.
	 * The value is updated every second.
	 *
	 * @return {@link BehaviorSubject} with current backend conection state
	 */
	BehaviorSubject<BackendConnectionState> getConnectionState();

	/**
	 * Returns a {@link BehaviorSubject} with the current data fetch type. This type
	 * determinates how edge should fetch data. Either via backend or locally via
	 * API's.
	 *
	 * @return {@link BehaviorSubject} with current data fetch type
	 */
	BehaviorSubject<DataFetchType> getDataFetchType();
}
