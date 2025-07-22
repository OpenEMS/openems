package io.openems.backend.metrics.prometheus;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Info;

public class PrometheusMetrics {

	public static final Gauge WEBSOCKET_CONNECTION = Gauge.builder() //
			.name("websocket_connections") //
			.labelNames("component") //
			.build();

	public static final Gauge THREAD_POOL_QUEUE = Gauge.builder() //
			.name("thread_pool_queue") //
			.labelNames("component") //
			.build();

	public static final Gauge THREAD_POOL_ACTIVE_COUNT = Gauge.builder() //
			.name("thread_pool_active_count") //
			.labelNames("component") //
			.build();

	public static final Gauge THREAD_POOL_COMPLETED_TASKS = Gauge.builder() //
			.name("thread_pool_completed_tasks") //
			.labelNames("component") //
			.build();

	public static final Gauge THREAD_POOL_CURRENT_SIZE = Gauge.builder() //
			.name("thread_pool_current_size") //
			.labelNames("component") //
			.build();

	public static final Gauge THREAD_POOL_MAX_SIZE = Gauge.builder() //
			.name("thread_pool_max_size") //
			.labelNames("component") //
			.build();

	public static final Gauge ALERTING_MESSAGES_SENT = Gauge.builder() //
			.name("alerting_messages_sent") //
			.labelNames("component") //
			.build();

	public static final Gauge ALERTING_MESSAGES_QUEUE = Gauge.builder() //
			.name("alerting_messages_queue") //
			.labelNames("component") //
			.build();

	public static final Info OPENEMS_VERSION = Info.builder() //
			.name("openems_version") //
			.help("OpenEMS Version") //
			.labelNames("version") //
			.build();

}
