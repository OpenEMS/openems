package io.openems.edge.chp.ecpower.manager;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.chp.ecpower.control.XrgiControl;
import io.openems.edge.chp.ecpower.ro.XrgiRo;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.generator.api.ManagedSymmetricGenerator;
import io.openems.edge.generator.api.SymmetricGenerator;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "CHP.ECcpower.manager", //		
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE })
public class XrgiManagerImpl extends AbstractOpenemsComponent implements XrgiManager, ManagedSymmetricGenerator, SymmetricGenerator, OpenemsComponent, EventHandler {

	@Reference
	private ConfigurationAdmin cm;

	private final Logger log = LoggerFactory.getLogger(XrgiManagerImpl.class);

	@Reference(name = "XrgiRo", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile XrgiRo xrgiRo = null;

	@Reference(name = "XrgiControl", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile XrgiControl xrgiControl = null;

	private Config config = null;
	
	private State state = State.UNDEFINED;	

	public XrgiManagerImpl() {
		super(
				OpenemsComponent.ChannelId.values(), 
				//ElectricityMeter.ChannelId.values(),
				ManagedSymmetricGenerator.ChannelId.values(),
				SymmetricGenerator.ChannelId.values(),
				XrgiManager.ChannelId.values()

		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.xrgiRo_id().isEmpty()) {
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "XrgiRo", config.xrgiRo_id());
		}

		if (!config.xrgiControl_id().isEmpty()) {
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "XrgiControl", config.xrgiControl_id());
		}

		this.config = config;
	}

	
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	
	@Override
	public void applyPower(int activePowerTarget) {
		this.applyPower((Integer) activePowerTarget);
	}

	@Override
	public void applyPower(Integer activePowerTarget) {
		if (this.xrgiControl == null || this.xrgiRo == null) {
			log.warn("XrgiControl or XrgiRO is not set!");
			return;
		}
		this.xrgiControl.applyPower(activePowerTarget);		
	}

	@Override
	public void handleEvent(Event event) {

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			//this._setActivePower(this.xrgiRo.getActivePower().get());
			//this.applyPower(19000);
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.checkState();
			break;

		}
	}
	
	public boolean awaitingStepTransitionHysteresis() {
		if (this.state ==  State.AWAITING_HYSTERESIS ) {
			return true;
		} else {
			return false;
		}
	}
	
	public void checkState() {
		if (this.xrgiRo == null) {
			this.state = State.ERROR;
		}
		
		if (this.xrgiControl == null) {
			this.state = State.ERROR;
		}
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public String debugLog() {

		if (this.xrgiRo == null) {
			log.warn("xrgiRo is not set!");
			return "NO XRGI";
		}

		if (this.xrgiControl == null) {
			log.warn("xrgiControl is not set!");
			return "NO xrgiControl";
		}
		// return null;
		return "Power: " + this.xrgiRo.getActivePower().asString();
	}

	@Override
	public Value<Integer> getGeneratorActivePower() {
		return this.xrgiRo.getActivePower();
	}

}
