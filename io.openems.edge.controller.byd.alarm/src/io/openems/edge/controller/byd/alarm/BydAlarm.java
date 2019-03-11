package io.openems.edge.controller.byd.alarm;

import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;





@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.byd.alarm", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BydAlarm extends AbstractOpenemsComponent implements Controller, OpenemsComponent {
	
	private final Logger log = LoggerFactory.getLogger(BydAlarm.class);

	@Reference
	protected ComponentManager componentManager;

	public BydAlarm() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}
	
	private ChannelAddress inputChannelAddress;
	private ChannelAddress outputChannelAddress;
	
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		STATE_MACHINE(new Doc() //
				.level(Level.INFO) //
				.text("Current State of State-Machine") //
				.options(State.values()));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		/*
		 * parse config
		 */

		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
		this.outputChannelAddress = ChannelAddress.fromString(config.outputChannelAddress());

		super.activate(context, config.id(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	
	@Override
	public void run() throws IllegalArgumentException, OpenemsNamedException {
		
	}

}
