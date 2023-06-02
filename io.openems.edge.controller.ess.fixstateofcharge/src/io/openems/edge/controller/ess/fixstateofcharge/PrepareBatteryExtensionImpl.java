package io.openems.edge.controller.ess.fixstateofcharge;

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
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.fixstateofcharge.api.AbstractFixStateOfCharge;
import io.openems.edge.controller.ess.fixstateofcharge.api.ConfigProperties;
import io.openems.edge.controller.ess.fixstateofcharge.api.FixStateOfCharge;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = ConfigPrepareBatteryExtension.class, factory = true)
@Component(//
		name = "Controller.Ess.PrepareBatteryExtension", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PrepareBatteryExtensionImpl extends AbstractFixStateOfCharge
		implements FixStateOfCharge, Controller, OpenemsComponent {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	@Reference
	private ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public PrepareBatteryExtensionImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				FixStateOfCharge.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, ConfigPrepareBatteryExtension config) {
		ConfigProperties cp = new ConfigProperties(config.isRunning(), config.targetSoc(), config.targetTimeSpecified(),
				config.targetTime(), config.targetTimeBuffer(), config.selfTermination(), config.terminationBuffer(),
				config.conditionalTermination(), config.endCondition());

		super.activate(context, config.id(), config.alias(), config.enabled(), cp);
	}

	@Modified
	private void modified(ComponentContext context, ConfigPrepareBatteryExtension config) throws OpenemsNamedException {
		ConfigProperties cp = new ConfigProperties(config.isRunning(), config.targetSoc(), config.targetTimeSpecified(),
				config.targetTime(), config.targetTimeBuffer(), config.selfTermination(), config.terminationBuffer(),
				config.conditionalTermination(), config.endCondition());

		super.modified(context, config.id(), config.alias(), config.enabled(), cp);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		super.run();
	}

	@Override
	public ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	public Sum getSum() {
		return this.sum;
	}

	@Override
	public ManagedSymmetricEss getEss() {
		return this.ess;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ConfigurationAdmin getConfigurationAdmin() {
		return this.cm;
	}
}
