package io.openems.backend.alerting;

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

import io.openems.backend.alerting.handler.OfflineEdgeHandler;
import io.openems.backend.alerting.scheduler.Scheduler;
import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Alerting", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ON_SET_ONLINE, //
		Metadata.Events.AFTER_IS_INITIALIZED //
})
public class Alerting extends AbstractOpenemsBackendComponent implements EventHandler {

	private final Logger log = LoggerFactory.getLogger(Alerting.class);

	@Reference
	protected Metadata metadata;

	@Reference
	protected Mailer mailer;

	protected final Scheduler scheduler;
	protected Handler<?>[] handler = {};

	protected Alerting(Scheduler scheduler) {
		super("Alerting");

		this.scheduler = scheduler;
	}

	public Alerting() {
		this(new Scheduler());
	}

	@Activate
	protected void activate(Config config) {
		this.logInfo(this.log, "Activate");
		this.scheduler.start();

		this.handler = new Handler[] {
				new OfflineEdgeHandler(this.scheduler, this.mailer, this.metadata, config.initialDelay()) };
	}

	@Deactivate
	protected void deactivate() {
		this.logInfo(this.log, "Deactivate");

		for (Handler<?> handlerInstance : this.handler) {
			handlerInstance.stop();
		}
		this.handler = new Handler<?>[0];
		this.scheduler.stop();
	}

	@Override
	public void handleEvent(Event event) {
		for (Handler<?> handlerInstance : this.handler) {
			handlerInstance.handleEvent(event);
		}
	}
}
