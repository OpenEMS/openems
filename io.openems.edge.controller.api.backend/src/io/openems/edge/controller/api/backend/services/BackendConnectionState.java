package io.openems.edge.controller.api.backend.services;

import java.time.Duration;

import io.openems.edge.controller.api.backend.api.DataFetchType;

public sealed interface BackendConnectionState {
	/**
	 * Returns how data should be fetched.
	 *
	 * @return Enum that signals how data should be fetched.
	 */
	DataFetchType dataFetchType();

	record StillInitializing() implements BackendConnectionState {
		@Override
		public DataFetchType dataFetchType() {
			return DataFetchType.WAITING;
		}
	}

	record NoConnection(Duration durationWithoutConnection) implements BackendConnectionState {
		private static final int WAIT_SECONDS_BEFORE_LOCAL_FETCH_SWITCH = 300;

		@Override
		public DataFetchType dataFetchType() {
			if (this.durationWithoutConnection.getSeconds() > WAIT_SECONDS_BEFORE_LOCAL_FETCH_SWITCH) {
				return DataFetchType.FETCH_LOCALLY;
			} else {
				return DataFetchType.WAITING;
			}
		}
	}

	record NoBackendInstance() implements BackendConnectionState {
		@Override
		public DataFetchType dataFetchType() {
			return DataFetchType.FETCH_LOCALLY;
		}
	}

	record Connected() implements BackendConnectionState {
		@Override
		public DataFetchType dataFetchType() {
			return DataFetchType.FETCH_VIA_BACKEND;
		}
	}
}
