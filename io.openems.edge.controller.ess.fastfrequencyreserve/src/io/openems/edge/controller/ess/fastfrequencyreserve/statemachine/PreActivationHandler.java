package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.function.BiConsumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class PreActivationHandler extends StateHandler<State, Context> {

	private BiConsumer<String, Long> logTime = (desc, now) -> System.out.println(desc + Instant.ofEpochSecond(now));

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		ManagedSymmetricEss e = context.ess;
		var minPowerEss = e.getPower().getMinPower(context.ess, Phase.ALL, Pwr.ACTIVE);
		ComponentManager componentManager = context.componentManager;

		context.setCycleStart(LocalDateTime.now(componentManager.getClock()));
		System.out.println(" Started the cycle at : " + context.getCycleStart());

		if (this.isActivationTime(context.componentManager, context.startTimestamp, context.duration)) {
			return State.ACTIVATION_TIME;
		} else {
			// Charge with 18 % of min Power of ess
			context.setPowerandPrint(State.PRE_ACTIVATIOM_STATE, (int) (minPowerEss * 0.18), e, componentManager);
			return State.PRE_ACTIVATIOM_STATE;
		}
	}

	private boolean isActivationTime(ComponentManager cm, long startTimestamp, int duration) {
		var now = ZonedDateTime.now(cm.getClock()).toEpochSecond();

		logTime.accept("now : ", now);
		logTime.accept("How long : ", startTimestamp + duration);
		if (now >= startTimestamp && now <= startTimestamp + duration) {
			return true;
		} else {
			return false;
		}
	}

}
