package io.openems.backend.simulator.mailer;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.mail.MailContext;
import io.openems.backend.common.mail.Mailer;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Simulator.Mailer", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SimulatorMailer extends AbstractOpenemsBackendComponent implements Mailer {

	private final Logger log = LoggerFactory.getLogger(SimulatorMailer.class);
	private String id;

	@Activate
	public SimulatorMailer(ComponentContext context, Config config) {
		super("SimulatorMailer");
		this.id = config.id();
	}

	@Override
	public CompletableFuture<Integer> sendMail(ZonedDateTime sendAt, String templateId, List<MailContext> params) {
		this.log.info("[{}]: Mail(sendAt: {}, templateId: {}, params: {})", this.id, sendAt, templateId, params);
		return CompletableFuture.completedFuture(params.size());
	}
}
