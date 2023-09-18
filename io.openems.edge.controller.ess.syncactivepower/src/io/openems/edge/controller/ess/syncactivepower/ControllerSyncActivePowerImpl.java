package io.openems.edge.controller.ess.syncactivepower;

import org.osgi.framework.InvalidSyntaxException;
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

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
	name = "Controller.Ess.SyncActivePower", //
	immediate = true, //
	configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerSyncActivePowerImpl extends AbstractOpenemsComponent
	implements ControllerSyncActivePower, Controller, OpenemsComponent {

    private Config config;

    private int delayCounter = 0;
    private Integer lastActivePowerReadOnlyEss = 0;

    @Reference
    private ConfigurationAdmin configAdmin;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private SymmetricEss readOnlyEss;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private ManagedSymmetricEss managedEss;

    public ControllerSyncActivePowerImpl() {
	super(//
		OpenemsComponent.ChannelId.values(), //
		Controller.ChannelId.values(), //
		ControllerSyncActivePower.ChannelId.values() //
	);
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsNamedException, InvalidSyntaxException {
	super.activate(context, config.id(), config.alias(), config.enabled());
	this.config = config;
	if (OpenemsComponent.updateReferenceFilter(this.configAdmin, this.servicePid(), "readOnlyEss",
		config.readOnlyEss_id())) {
	    return;
	}
	if (OpenemsComponent.updateReferenceFilter(this.configAdmin, this.servicePid(), "managedEss",
		config.managedEss_id())) {
	    return;
	}

    }

    @Override
    @Deactivate
    protected void deactivate() {
	super.deactivate();
    }

    @Override
    public void run() throws OpenemsNamedException {
	try {
	    var readOnlyActivePower = this.readOnlyEss.getActivePower().getOrError();
	    var managedEssActivePower = this.managedEss.getActivePower().getOrError();
	    Integer overallEssActivePower = readOnlyActivePower + managedEssActivePower;
	    // If readOnlyEss changes from charging to discharging or vice versa, we wait
	    // for a number of cycles
	    if (Math.signum(readOnlyActivePower) != Math.signum(this.lastActivePowerReadOnlyEss)) {
		this.managedEss.setActivePowerEqualsWithPid(0);
		this._setDebugSetActivePowerBeforePid(0);
		this.delayCounter = 0;
	    } else {
		this.delayCounter = this.delayCounter >= this.config.delay() ? this.config.delay()
			: this.delayCounter + 1;
	    }
	    if (this.delayCounter >= this.config.delay()) {

		if (overallEssActivePower > this.config.lowerThreshold()) {
		    this.syncWithDischargingEss(overallEssActivePower);
		} else if (overallEssActivePower < -this.config.lowerThreshold()) {
		    this.syncWithChargingEss(overallEssActivePower);
		} else {
		    this.managedEss.setActivePowerEqualsWithPid(0);
		    this._setDebugSetActivePowerBeforePid(0);
		}
	    } else {
		this.managedEss.setActivePowerEqualsWithPid(0);
		this._setDebugSetActivePowerBeforePid(0);
	    }

	    this.lastActivePowerReadOnlyEss = readOnlyActivePower;
	    this._setRunFailed(false);
	    this.getStateChannel().setNextValue(Level.OK);

	} catch (OpenemsException e) {
	    this._setRunFailed(true);
	    this.getStateChannel().setNextValue(Level.FAULT);
	}

    }

    private void syncWithDischargingEss(Integer overallEssActivePower) throws OpenemsNamedException {
	// Integer readOnlySoc = this.readOnlyEss.getSoc().getOrError();
	// if (readOnlySoc < 50) {
	this.managedEss.setActivePowerEqualsWithPid(TypeUtils.divide(overallEssActivePower, 2));
	this._setDebugSetActivePowerBeforePid(TypeUtils.divide(overallEssActivePower, 2));
	// return;
	// }
	// if (readOnlySoc > 80) {
	// this.managedEss.setActivePowerEqualsWithPid(TypeUtils.divide(overallEssActivePower,5));
	// this._setDebugSetActivePowerBeforePid(TypeUtils.divide(overallEssActivePower,5));
	// return;
	// }
	// this.managedEss.setActivePowerEqualsWithPid(TypeUtils.divide(overallEssActivePower,
	// 3));
	// this._setDebugSetActivePowerBeforePid(TypeUtils.divide(overallEssActivePower,
	// 3));
    }

    private void syncWithChargingEss(Integer overallEssActivePower) throws OpenemsNamedException {
	var readOnlySoc = this.readOnlyEss.getSoc().getOrError();
	if (readOnlySoc < this.config.minSocReadOnly()) {
	    this.managedEss.setActivePowerEqualsWithPid(0);
	    this._setDebugSetActivePowerBeforePid(0);
	    return;
	}
	if (readOnlySoc > 50) {
	    this.managedEss.setActivePowerEqualsWithPid(TypeUtils.divide(overallEssActivePower, 2));
	    this._setDebugSetActivePowerBeforePid(TypeUtils.divide(overallEssActivePower, 2));
	    return;
	}
	this.managedEss.setActivePowerEqualsWithPid(TypeUtils.divide(overallEssActivePower, 4));
	this._setDebugSetActivePowerBeforePid(TypeUtils.divide(overallEssActivePower, 4));
    }

}
