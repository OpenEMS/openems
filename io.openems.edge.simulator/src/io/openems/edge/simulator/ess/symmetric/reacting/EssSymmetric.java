package io.openems.edge.simulator.ess.symmetric.reacting;

import java.io.IOException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;
import io.openems.edge.simulator.ess.EssUtils;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.EssSymmetric.Reacting", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class EssSymmetric extends AbstractOpenemsComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	/**
	 * Current state of charge
	 */
	private float soc = 0;

	/**
	 * Total configured capacity in Wh
	 */
	private int capacity = 0;

	/**
	 * Configured max Apparent Power in VA
	 */
	private int maxApparentPower = 0;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Reference
	private Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SimulatorDatasource datasource;

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.getSoc().setNextValue(config.initialSoc());
		this.soc = config.initialSoc();
		this.capacity = config.capacity();
		this.maxApparentPower = config.maxApparentPower();
		this.getMaxApparentPower().setNextValue(config.maxApparentPower());
		this.getAllowedCharge().setNextValue(this.maxApparentPower * -1);
		this.getAllowedDischarge().setNextValue(this.maxApparentPower);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public EssSymmetric() {
		EssUtils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.updateChannels();
			break;
		}
	}

	private void updateChannels() {
		// nothing to do
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|" + this.getGridMode().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		/*
		 * calculate State of charge
		 */
		float watthours = (float) activePower * this.datasource.getTimeDelta() / 3600;
		float socChange = watthours / this.capacity;
		this.soc -= socChange;
		if (this.soc > 100) {
			this.soc = 100;
		} else if (this.soc < 0) {
			this.soc = 0;
		}
		this.getSoc().setNextValue(this.soc);
		/*
		 * Apply Active/Reactive power to simulated channels
		 */
		if (soc == 0 && activePower > 0) {
			activePower = 0;
		}
		if (soc == 100 && activePower < 0) {
			activePower = 0;
		}
		this.getActivePower().setNextValue(activePower);
		if (soc == 0 && reactivePower > 0) {
			reactivePower = 0;
		}
		if (soc == 100 && reactivePower < 0) {
			reactivePower = 0;
		}
		this.getReactivePower().setNextValue(reactivePower);
		/*
		 * Set AllowedCharge / Discharge based on SoC
		 */
		if (this.soc == 100) {
			this.getAllowedCharge().setNextValue(0);
		} else {
			this.getAllowedCharge().setNextValue(this.maxApparentPower * -1);
		}
		if (this.soc == 0) {
			this.getAllowedDischarge().setNextValue(0);
		} else {
			this.getAllowedDischarge().setNextValue(this.maxApparentPower);
		}
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}
}
