package io.openems.edge.controller.io.fixdigitaloutput;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.FixDitigalOutput", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FixDigitalOutput extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(FixDigitalOutput.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OpenemsComponent outputComponent = null;

	private WriteChannel<Boolean> outputChannel = null;
	private boolean isOn = false;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		/*
		 * parse config
		 */
		this.isOn = config.isOn();
		ChannelAddress outputChannelAddress = ChannelAddress.fromString(config.outputChannelAddress());

		// update filter for 'Output' component
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "outputComponent",
				outputChannelAddress.getComponentId())) {
			return;
		}

		/*
		 * get actual output channel
		 */
		this.outputChannel = this.outputComponent.channel(outputChannelAddress.getChannelId());

		super.activate(context, config.service_pid(), config.id(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {

		if (this.isOn) {
			this.switchOn();
		} else {
			this.switchOff();
		}
	}

	/**
	 * Switch the output ON
	 */
	private void switchOn() {
		this.setOutput(true);
	}

	/**
	 * Switch the output OFF
	 */
	private void switchOff() {
		this.setOutput(false);
	}

	private void setOutput(boolean value) {
		try {
			this.outputChannel.setNextWriteValue(value);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to set output: [" + this.outputChannel.address() + "] " + e.getMessage());
		}
	}
}
