package io.openems.common.jsonrpc.syncable;

public enum SyncableProviderType {
	/**
	 * With this provider, BackendEdge is receiving the syncable data from Backend
	 * and is providing it to the Edge.
	 *
	 * <p>
	 * Positive: Great performance
	 *
	 * <p>
	 * Negative: Data must be identical for every edge system
	 */
	BACKEND_EDGE_SERVER,
	/**
	 * With this provider, Backend itself is providing the data individually to the
	 * Edge. This is useful if the data is different for every edge system.
	 *
	 * <p>
	 * Positive: Data is completely handled in edge; Different edge systems can have
	 * different data.
	 * 
	 * <p>
	 * Negative: Performance is worse because Backend is responsible for everything
	 */
	BACKEND
}
