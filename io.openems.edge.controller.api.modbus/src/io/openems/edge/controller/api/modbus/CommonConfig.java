package io.openems.edge.controller.api.modbus;

import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.meta.Meta;

public sealed interface CommonConfig {

	public static final int DEFAULT_UNIT_ID = 1;
	public static final int DEFAULT_API_TIMEOUT_SECONDS = 60;
	public static final int DEFAULT_MAX_CONCURRENT_CONNECTIONS = 5;

	/**
	 * The Component-ID.
	 * 
	 * @return id
	 */
	public String id();

	/**
	 * The Component Alias.
	 * 
	 * @return alias
	 */
	public String alias();

	/**
	 * Component enabled?.
	 * 
	 * @return enabled
	 */
	public boolean enabled();

	/**
	 * The {@link Meta} component.
	 * 
	 * @return component
	 */
	public Meta metaComponent();

	/**
	 * The Component-IDs.
	 * 
	 * @return Component-IDs
	 */
	public String[] componentIds();

	/**
	 * The API timeout for Write implementations.
	 * 
	 * @return timeout; or null for Read implementations
	 */
	public Integer apiTimeout();

	/**
	 * The Max-Concurrent-Connections.
	 * 
	 * @return Max-Concurrent-Connections
	 */
	public int maxConcurrentConnections();

	/**
	 * The {@link LogVerbosity}.
	 * 
	 * @return logVerbosity
	 */
	public LogVerbosity logVerbosity();

	public record Tcp(String id, String alias, boolean enabled, Meta metaComponent, String[] componentIds,
			Integer apiTimeout, int port, int maxConcurrentConnections, LogVerbosity logVerbosity)
			implements CommonConfig {

		public static final int DEFAULT_PORT = 502;

		/**
		 * Build Read-Only {@link CommonConfig.Tcp}.
		 * 
		 * @param src           the source Config
		 * @param metaComponent the {@link Meta} component
		 * @return Common-Config
		 */
		public static Tcp from(io.openems.edge.controller.api.modbus.readonly.tcp.Config src, Meta metaComponent) {
			return new Tcp(src.id(), src.alias(), src.enabled(), metaComponent, src.component_ids(), null, src.port(),
					src.maxConcurrentConnections(), src.logVerbosity());
		}

		/**
		 * Build Read-Write {@link CommonConfig.Tcp}.
		 * 
		 * @param src           the source Config
		 * @param metaComponent the {@link Meta} component
		 * @return Common-Config
		 */
		public static Tcp from(io.openems.edge.controller.api.modbus.readwrite.tcp.Config src, Meta metaComponent) {
			return new Tcp(src.id(), src.alias(), src.enabled(), metaComponent, src.component_ids(),
					DEFAULT_API_TIMEOUT_SECONDS, src.port(), src.maxConcurrentConnections(), src.logVerbosity());
		}
	}

	public record Rtu(String id, String alias, boolean enabled, Meta metaComponent, String[] componentIds,
			Integer apiTimeout, String portName, int baudRate, int databits, Stopbit stopbits, Parity parity,
			int maxConcurrentConnections, LogVerbosity logVerbosity) implements CommonConfig {

		/**
		 * Build Read-Only {@link CommonConfig.Rtu}.
		 * 
		 * @param src           the source Config
		 * @param metaComponent the {@link Meta} component
		 * @return Common-Config
		 */
		public static Rtu from(io.openems.edge.controller.api.modbus.readonly.rtu.Config src, Meta metaComponent) {
			return new Rtu(src.id(), src.alias(), src.enabled(), metaComponent, src.component_ids(), null,
					src.portName(), src.baudRate(), src.databits(), src.stopbits(), src.parity(),
					src.maxConcurrentConnections(), src.logVerbosity());
		}

		/**
		 * Build Read-Write {@link CommonConfig.Rtu}.
		 * 
		 * @param src           the source Config
		 * @param metaComponent the {@link Meta} component
		 * @return Common-Config
		 */
		public static Rtu from(io.openems.edge.controller.api.modbus.readwrite.rtu.Config src, Meta metaComponent) {
			return new Rtu(src.id(), src.alias(), src.enabled(), metaComponent, src.component_ids(), src.apiTimeout(),
					src.portName(), src.baudRate(), src.databits(), src.stopbits(), src.parity(),
					src.maxConcurrentConnections(), src.logVerbosity());
		}

	}
}