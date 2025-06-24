package io.openems.backend.simulator.mailer;

import java.time.ZonedDateTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.metadata.Mailer;

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
	public void sendMail(ZonedDateTime sendAt, String templateId, JsonElement params) {
		this.log.info("[{}]: Mail(sendAt: {}, templateId: {}, params: {})", this.id, sendAt, templateId, params);
	}
}
