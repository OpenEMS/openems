package io.openems.edge.controller.ess.limiter14a.eebus;

import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.eebus.api.BridgeEebus;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.ILimitPowerConsumptionHandler;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.LimitPowerState;
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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.limiter14a.ControllerEssLimiter14a;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Limiter14a.Eebus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences({"ess", "eebusBridge"})
public class ControllerEssLimiter14aEebusImpl extends AbstractOpenemsComponent implements //
		ControllerEssLimiter14aEebus, ControllerEssLimiter14a, Controller, OpenemsComponent, TimedataProvider, ILimitPowerConsumptionHandler {

	@Reference
	private Sum sum;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(//
			target = "(&(id=${config.eebus_id})(enabled=true))",
			policy = ReferencePolicy.STATIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MANDATORY //
	)
	private volatile BridgeEebus eebusBridge;

	@Reference(target = "(&(id=${config.ess_id})(enabled=true))", //
			policy = ReferencePolicy.STATIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MANDATORY)
	private volatile ManagedSymmetricEss ess;

	private Double currentLimitInW;

	private final CalculateActiveTime cumulatedRestrictionTime = new CalculateActiveTime(this,
			ControllerEssLimiter14a.ChannelId.CUMULATED_RESTRICTION_TIME);

	private final Logger logger = LoggerFactory.getLogger(ControllerEssLimiter14aEebusImpl.class);

	public ControllerEssLimiter14aEebusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssLimiter14a.ChannelId.values(), //
				ControllerEssLimiter14aEebus.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);

		this.eebusBridge.getUseCaseManager().addLimitPowerConsumptionHandler(this);
	}

	@Modified
	protected void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled()); //
		this.applyConfig(config);
	}

	protected void applyConfig(Config config) throws OpenemsNamedException {

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.eebusBridge.getUseCaseManager().removeLimitPowerConsumptionHandler(this);
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (this.isLimitActive()) {
			this.ess.setActivePowerGreaterOrEquals(this.currentLimitInW.intValue());
			this.cumulatedRestrictionTime.update(true);
		} else {
			this.cumulatedRestrictionTime.update(false);
		}
	}

	public boolean isLimitActive() {
		return this.sum.getGridMode() == GridMode.ON_GRID && this.currentLimitInW != null;
	}

	@Override
	public String debugLog() {
		if (this.currentLimitInW == null) {
			return "No limit";
		}

		return "Limit: " + this.currentLimitInW + "W";
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public long getNominalMaxConsumption() {
		return 0;
	}

	@Override
	public void handleLimitPowerConsumption(LimitPowerState state, Double currentLimitInW) {
		this.logInfo(this.logger, "Received eebus limit: " + currentLimitInW);
		this.currentLimitInW = currentLimitInW;

		var isRestrictionActive = currentLimitInW != null;
		setValue(this, ControllerEssLimiter14a.ChannelId.RESTRICTION_MODE, isRestrictionActive);
		this.setRestrictionReason(state, isRestrictionActive);
	}

	private void setRestrictionReason(LimitPowerState state, boolean isRestrictionActive) {
		if (!isRestrictionActive) {
			setValue(this, ControllerEssLimiter14aEebus.ChannelId.RESTRICTION_MODE_REASON, RestrictionModeReason.NO_LIMIT);
		} else if (state == LimitPowerState.FAILSAFE) {
			setValue(this, ControllerEssLimiter14aEebus.ChannelId.RESTRICTION_MODE_REASON, RestrictionModeReason.ACTIVE_FAILSAFE);
		} else {
			setValue(this, ControllerEssLimiter14aEebus.ChannelId.RESTRICTION_MODE_REASON, RestrictionModeReason.LIMITED);
		}
	}
}