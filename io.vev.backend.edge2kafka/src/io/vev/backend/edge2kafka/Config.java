package io.vev.backend.edge2kafka;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "VEV Edge2Kafka Bridge", //
		description = "Bridge for forwarding selected Edge data to Kafka and processing external commands")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this component")
	String id() default "edge2kafka0";

	@AttributeDefinition(name = "Component prefixes",
			description = "Component-ID prefixes to forward (comma separated). Example: ess,meter")
	String component_prefixes() default "ess,meter";

	@AttributeDefinition(name = "Log aggregated data", description = "If true, aggregated data notifications are logged")
	boolean log_aggregated() default true;

	@AttributeDefinition(name = "Kafka bootstrap servers",
			description = "Comma separated list of Kafka bootstrap servers, e.g. kafka1:9092,kafka2:9092")
	String kafka_bootstrap_servers() default "";

	@AttributeDefinition(name = "Enable Kafka producer",
			description = "If enabled, data messages are forwarded to the configured Kafka topic")
	boolean kafka_enable_producer() default true;

	@AttributeDefinition(name = "Enable Kafka consumer",
			description = "If enabled, the service consumes commands from Kafka and forwards them to the Edge")
	boolean kafka_enable_consumer() default false;

	@AttributeDefinition(name = "Kafka data topic", description = "Topic used for publishing Edge data")
	String kafka_data_topic() default "vev.edge.data";

	@AttributeDefinition(name = "Kafka command topic", description = "Topic used for consuming control commands")
	String kafka_command_topic() default "vev.edge.commands";

	@AttributeDefinition(name = "Kafka producer client-id")
	String kafka_producer_client_id() default "edge2kafka-producer";

	@AttributeDefinition(name = "Kafka consumer client-id")
	String kafka_consumer_client_id() default "edge2kafka-consumer";

	@AttributeDefinition(name = "Kafka consumer group-id")
	String kafka_consumer_group_id() default "edge2kafka";

	@AttributeDefinition(name = "Kafka consumer poll interval [ms]")
	int kafka_consumer_poll_interval_ms() default 1000;

	@AttributeDefinition(name = "Kafka consumer shutdown timeout [ms]")
	int kafka_consumer_shutdown_timeout_ms() default 10000;

	String webconsole_configurationFactory_nameHint() default "Edge2Kafka Bridge";

}
