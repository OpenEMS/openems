package io.openems.backend.alerting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;

import io.openems.backend.alerting.handler.OfflineEdgeHandler;
import io.openems.backend.alerting.handler.SumStateHandler;
import io.openems.backend.alerting.scheduler.Scheduler;
import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.event.EventReader;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Alerting", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ON_SET_ONLINE, //
		Edge.Events.ON_SET_SUM_STATE, //
		Metadata.Events.AFTER_IS_INITIALIZED //
})
public class Alerting extends AbstractOpenemsBackendComponent implements EventHandler, DebugLoggable {

	// Maximum number of messages constructed at the same time
	private static final byte THREAD_POOL_SIZE = 2;
	// Queue size from which warnings are issued
	private static final byte THREAD_QUEUE_WARNING_THRESHOLD = 50;

	private static final ThreadPoolExecutor createDefaultExecutorService() {
		final var threadFactory = new ThreadFactoryBuilder()
				.setNameFormat(Alerting.class.getSimpleName() + ".EventHandler-%d").build();
		return (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_POOL_SIZE, threadFactory);
	}

	private final Logger log = LoggerFactory.getLogger(Alerting.class);
	private final ThreadPoolExecutor executor;

	@Reference
	protected Metadata metadata;

	@Reference
	protected Mailer mailer;

	private final Scheduler scheduler;

	protected final List<Handler<?>> handler = new ArrayList<>(2);

	protected Alerting(Scheduler scheduler, ThreadPoolExecutor executor) {
		super("Alerting");
		this.executor = executor;
		this.scheduler = scheduler;
	}

	public Alerting() {
		this(new Scheduler(), Alerting.createDefaultExecutorService());
	}

	@Activate
	protected void activate(Config config) {
		this.logInfo(this.log, "Activate");
		this.scheduler.start();

		if (config.notifyOnOffline()) {
			var handler = new OfflineEdgeHandler(this.scheduler, this.scheduler, this.mailer, this.metadata, //
					config.initialDelay());
			this.handler.add(handler);
		}

		if (config.notifyOnSumStateChange()) {
			var handler = new SumStateHandler(this.scheduler, this.scheduler, this.mailer, this.metadata, //
					config.initialDelay());
			this.handler.add(handler);
		}
	}

	@Deactivate
	protected void deactivate() {
		this.logInfo(this.log, "Deactivate");
		this.handler.forEach(Handler::stop);
		this.handler.clear();
		this.scheduler.stop();
	}

	@Override
	public void handleEvent(Event event) {
		var reader = new EventReader(event);
		for (var h : this.handler) {
			var task = h.getEventHandler(reader.getTopic());
			if (task != null) {
				this.execute(task, reader);
			}
		}
	}

	private void execute(Consumer<EventReader> consumer, EventReader reader) {
		this.executor.execute(() -> consumer.accept(reader));
	}

	@Override
	public String debugLog() {
		int queueSize = this.executor.getQueue().size();
		if (queueSize >= THREAD_QUEUE_WARNING_THRESHOLD) {
			return "%d tasks in the EventHandlerQueue!".formatted(queueSize);
		} else {
			return null;
		}
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {
		return null;
	}

}
