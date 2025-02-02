package io.openems.edge.controller.api.modbus;

import java.util.Arrays;

import io.openems.edge.common.meta.Meta;

public abstract class AbstractModbusConfig {
	private final String id;
	private final String alias;
	private final boolean enabled;
	private final Meta metaComponent;
	private final String[] componentIds;
	private final int apiTimeout;
	private final int maxConcurrentConnections;

	public AbstractModbusConfig(String id, String alias, boolean enabled, Meta metaComponent, String[] componentIds,
			int apiTimeout, int maxConcurrentConnections) {
		this.id = id;
		this.alias = alias;
		this.enabled = enabled;
		this.metaComponent = metaComponent;
		this.componentIds = componentIds;
		this.apiTimeout = apiTimeout;
		this.maxConcurrentConnections = maxConcurrentConnections;
	}

	/**
	 * Returns a unique ID for this OpenEMS component.
	 *
	 * @return the unique ID
	 */
	public String id() {
		return this.id;
	}

	/**
	 * Returns a unique ID for this OpenEMS component.
	 *
	 * @return the unique ID
	 */
	public String alias() {
		return this.alias;
	}
	
	/**
	 * Is this controller enabled?.
	 *
	 * @return boolean
	 */
	public boolean enabled() {
		return this.enabled;
	}

	/**
	 * Returns a metaComponent.
	 *
	 * @return the metaComponent
	 */
	public Meta metaComponent() {
		return this.metaComponent;
	}

	/**
	 * Returns an array of component ids.
	 *
	 * @return the componentIds
	 */
	public String[] componentIds() {
		return this.componentIds;
	}

	/**
	 * Returns the api timeout.
	 *
	 * @return the apiTimeout
	 */
	public int apiTimeout() {
		return this.apiTimeout;
	}

	/**
	 * Returns the max number of concurrent connections.
	 *
	 * @return the maxConcurrentConnections
	 */
	public int maxConcurrentConnections() {
		return this.maxConcurrentConnections;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		AbstractModbusConfig config = (AbstractModbusConfig) other;
		return this.enabled == config.enabled //
				&& this.apiTimeout == config.apiTimeout //
				&& this.maxConcurrentConnections == config.maxConcurrentConnections 
				&& this.id.equals(config.id) //
				&& this.alias.equals(config.alias) 
				&& this.metaComponent.equals(config.metaComponent) //
				&& Arrays.equals(this.componentIds, config.componentIds);
	}

}
