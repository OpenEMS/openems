package io.openems.backend.metrics.prometheus;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.debugcycle.MetricsConsumer;
import io.openems.common.types.ChannelAddress;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metrics.Prometheus", //
		immediate = true, //
		scope = ServiceScope.SINGLETON, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PrometheusClient extends AbstractOpenemsBackendComponent implements MetricsConsumer {

	private final Logger log = LoggerFactory.getLogger(PrometheusClient.class);

	private final PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
	private HTTPServer server;

	public PrometheusClient() {
		super("Prometheus");
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.logInfo(this.log, "Activate");

		JvmMetrics.builder().register(this.prometheusRegistry);
		this.prometheusRegistry.register(PrometheusMetrics.WEBSOCKET_CONNECTION);
		this.prometheusRegistry.register(PrometheusMetrics.THREAD_POOL_QUEUE);
		this.prometheusRegistry.register(PrometheusMetrics.THREAD_POOL_ACTIVE_COUNT);
		this.prometheusRegistry.register(PrometheusMetrics.THREAD_POOL_COMPLETED_TASKS);
		this.prometheusRegistry.register(PrometheusMetrics.THREAD_POOL_CURRENT_SIZE);
		this.prometheusRegistry.register(PrometheusMetrics.THREAD_POOL_MAX_SIZE);
		this.prometheusRegistry.register(PrometheusMetrics.ALERTING_MESSAGES_QUEUE);
		this.prometheusRegistry.register(PrometheusMetrics.ALERTING_MESSAGES_SENT);

		this.startServer(config.port(), config.bearerToken());
	}

	@Deactivate
	protected void deactivate() {
		this.logInfo(this.log, "Deactivate");

		this.stopServer();
		this.prometheusRegistry.clear();
	}

	private void startServer(int port, String bearerToken) {
		try {
			final var httpServerBuilder = HTTPServer.builder() //
					.port(port) //
					.registry(this.prometheusRegistry);
			if (bearerToken != null && !bearerToken.isBlank()) {
				httpServerBuilder.authenticator(new Authenticator() {
					@Override
					public Result authenticate(HttpExchange exchange) {
						String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
						if (authHeader == null || !authHeader.equals("Bearer " + bearerToken)) {
							return new Failure(401);
						}
						return new Success(new HttpPrincipal("prometheus", "metrics"));
					}
				});
			}
			this.server = httpServerBuilder.buildAndStart();
			this.log.info("Started /metrics endpoint on port %s".formatted(this.server.getPort()));
		} catch (IOException e) {
			this.log.error(e.getMessage());
		}
	}

	private void stopServer() {
		if (this.server != null) {
			this.server.stop();
			this.server = null;
			this.log.info("Stopped /metrics endpoint");
		}
	}

	@Override
	public void consumeMetrics(ZonedDateTime now, Map<String, JsonElement> metrics) {
		for (var entry : metrics.entrySet()) {
			final ChannelAddress channelAddress;
			try {
				channelAddress = ChannelAddress.fromString(entry.getKey());
			} catch (Exception e) {
				this.log.warn(e.getMessage());
				continue;
			}
			final var metricCollector = switch (channelAddress.getChannelId()) {
			case "Connections" -> PrometheusMetrics.WEBSOCKET_CONNECTION;
			case "Pending" -> PrometheusMetrics.THREAD_POOL_QUEUE;
			case "Active" -> PrometheusMetrics.THREAD_POOL_ACTIVE_COUNT;
			case "Completed" -> PrometheusMetrics.THREAD_POOL_COMPLETED_TASKS;
			case "PoolSize" -> PrometheusMetrics.THREAD_POOL_CURRENT_SIZE;
			case "MaxPoolSize" -> PrometheusMetrics.THREAD_POOL_MAX_SIZE;
			case "AlertingMessagesSent" -> PrometheusMetrics.ALERTING_MESSAGES_SENT;
			case "AlertingMessagesQueue" -> PrometheusMetrics.ALERTING_MESSAGES_QUEUE;
			default -> null;
			};
			if (metricCollector != null) {
				metricCollector.labelValues(channelAddress.getComponentId()).set(entry.getValue().getAsDouble());
			}
		}
	}
}
