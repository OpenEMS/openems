package io.openems.edge.controller.ess.ripplecontrolreceiver.eebus;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.eebus.api.BridgeEebus;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.ILimitPowerProductionHandler;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.LimitPowerState;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.ripplecontrolreceiver.PowerProductionLimiter;
import io.openems.edge.controller.ess.ripplecontrolreceiver.PowerProductionLimiterComponent;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.RippleControlReceiver.Eebus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences({ "eebusBridge" })
public class EebusPowerProductionLimiterImpl extends AbstractOpenemsComponent
		implements EebusPowerProductionLimiter, PowerProductionLimiterComponent, PowerProductionLimiter,
		OpenemsComponent, TimedataProvider, EventHandler, ILimitPowerProductionHandler {

	@Reference
	private Sum sum;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference(//
			target = "(&(id=${config.eebus_id})(enabled=true))", policy = ReferencePolicy.STATIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MANDATORY //
	)
	private volatile BridgeEebus eebusBridge;

	@Reference
	private ConfigurationAdmin cm;

	private Integer maxNominalProductionPower = 15_000;
	private Integer eebusLimit;

	private final CalculateActiveTime cumulatedRestrictionTime = new CalculateActiveTime(this,
			PowerProductionLimiterComponent.ChannelId.CUMULATED_RESTRICTION_TIME);

	private final Logger log = LoggerFactory.getLogger(EebusPowerProductionLimiterImpl.class);

	public EebusPowerProductionLimiterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PowerProductionLimiterComponent.ChannelId.values(), //
				EebusPowerProductionLimiter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.eebusBridge.getUseCaseManager().addLimitPowerProductionHandler(this);
	}

	@Modified
	protected void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled()); //
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.eebusBridge.getUseCaseManager().removeLimitPowerProductionHandler(this);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void setMaxNominalProductionPower(int maxNominalProductionPowerInW) {
		this.maxNominalProductionPower = maxNominalProductionPowerInW;
	}

	@Override
	public long getNominalMaxProduction() {
		return this.maxNominalProductionPower;
	}

	@Override
	public void handleLimitPowerProduction(LimitPowerState state, Double currentLimitInW) {
		this.logInfo(this.log, "Received eebus limit: " + currentLimitInW);
		this.eebusLimit = currentLimitInW != null ? currentLimitInW.intValue() : null;
	}

	protected void run() {
		try {
			if (this.sum.getGridMode() != GridMode.ON_GRID) {
				setValue(this, PowerProductionLimiterComponent.ChannelId.RESTRICTION, null);
				this.cumulatedRestrictionTime.update(false);
				return;
			}

			setValue(this, PowerProductionLimiterComponent.ChannelId.RESTRICTION, this.eebusLimit);
			this.cumulatedRestrictionTime.update(this.eebusLimit != null);
		} catch (Exception ex) {
			// TODO
			setValue(this, EebusPowerProductionLimiter.ChannelId.UPDATE_FAILURE, true);
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.run();
			break;
		}
	}

	@Override
	public String debugLog() {
		return "MaxPowerConsumption: " + this.formatInteger(this.maxNominalProductionPower) //
				+ "|Limit: " + this.formatInteger(this.eebusLimit);
	}

	private String formatInteger(Integer value) {
		if (value == null) {
			return "None";
		} else {
			return value.toString();
		}
	}
}
