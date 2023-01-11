package io.openems.backend.alerting;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
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
		Edge.Events.ON_SET_ONLINE //
})
public class Alerting extends AbstractOpenemsBackendComponent implements EventHandler {

	@Component(//
			service = StartParameter.class //
	)
	public static class StartParameter {

		protected final Metadata metadata;
		protected final Mailer mailer;

		@Activate
		public StartParameter(@Reference Metadata metadata, @Reference Mailer mailer) {
			this.metadata = metadata;
			this.mailer = mailer;
		}

	}

	private final Logger log = LoggerFactory.getLogger(Alerting.class);

	private final int initialDelay;
	protected final Scheduler scheduler;
	protected Handler<?>[] handler = {};

	@Activate
	public Alerting(Config config) {
		super("Alerting");
		this.initialDelay = config.initialDelay();
		this.scheduler = new Scheduler();

		this.logInfo(this.log, "Activate");
	}

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MANDATORY //
	)
	protected void bindStartParameter(StartParameter params) {
		this.scheduler.start();
		this.handler = new Handler[] {
				new OfflineEdgeHandler(this.scheduler, params.mailer, params.metadata, this.initialDelay) };
	}

	protected void unbindStartParameter(StartParameter params) {
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
