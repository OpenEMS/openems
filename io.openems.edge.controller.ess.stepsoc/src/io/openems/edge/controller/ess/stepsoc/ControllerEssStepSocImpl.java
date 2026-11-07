package io.openems.edge.controller.ess.stepsoc;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.controller.ess.stepsoc.ControllerEssStepSoc.ChannelId.STATE_MACHINE;
import static io.openems.edge.controller.ess.stepsoc.statemachine.StateMachine.State.UNDEFINED;

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
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.stepsoc.statemachine.Context;
import io.openems.edge.controller.ess.stepsoc.statemachine.StateMachine;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.StepSoc", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("ess")
public class ControllerEssStepSocImpl extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, ControllerEssStepSoc {

	private final Logger log = LoggerFactory.getLogger(ControllerEssStepSocImpl.class);
	private final StateMachine stateMachine = new StateMachine(UNDEFINED);

	@Reference(target = "(id=${config.ess_id})") //
	private ManagedSymmetricEss ess;

	@Reference()
	private ComponentManager componentManager;

	private Config config;
	private int lastPausedAtStep;

	public ControllerEssStepSocImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssStepSoc.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		// Store the current State
		setValue(this, STATE_MACHINE, this.stateMachine.getCurrentState());

		// Prepare Context
		final var context = new Context(this, //
				this.ess, //
				this.config.direction(), //
				this.config.power(), //
				this.config.socStep(), //
				this.config.standbyTime(), //
				this.componentManager.getClock()//
		);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);
			this._setRunFailed(false);
		} catch (OpenemsNamedException e) {
			this._setRunFailed(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	public int getPausedAtStep() {
		return this.lastPausedAtStep;
	}

	public void setPausedAtStep(int lastStandby) {
		this.lastPausedAtStep = lastStandby;
	}
}
