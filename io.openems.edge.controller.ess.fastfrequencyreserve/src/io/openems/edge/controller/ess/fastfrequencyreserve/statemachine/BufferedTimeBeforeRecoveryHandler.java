package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.EnumUtils;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class BufferedTimeBeforeRecoveryHandler extends StateHandler<State, Context> {

	private static final int ZERO_WATT_POWER = 0;
	private static final int BUFFER_DURATION_THRESHOLD_SECONDS = 15; // [s]
	private static final int RECOVERY_DURATION_THRESHOLD_MINUTES = 4; // [minute]
	private static final double EIGHTEENX_PERCENT_OF_MAX_POWER = 0.18; // [%]

	protected Instant bufferedTimeBeforeRecoveryStartTime = Instant.MIN;

	protected static record BufferedTimeBeforeRecoveryState(SubState subState, Instant lastChange) {
	}

	protected BufferedTimeBeforeRecoveryState bufferedTimeBeforeRecoveryState;

	private static enum SubState {
		HOLD_BUFFERED_TIME_BEFORE_RECOVERY, //
		BUFFERED_TIME_RECOVERY, //
		FINISH_BUFFERED_TIME_BEFORE_RECOVERY
	}

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		context.ess.setActivePowerEquals(ZERO_WATT_POWER);
		final var now = Instant.now(context.clock);
		this.bufferedTimeBeforeRecoveryStartTime = now;
		this.bufferedTimeBeforeRecoveryState = new BufferedTimeBeforeRecoveryState(
				SubState.HOLD_BUFFERED_TIME_BEFORE_RECOVERY, now);

	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var nextSubState = this.getNextSubState(context);

		if (nextSubState != this.bufferedTimeBeforeRecoveryState.subState) {
			this.bufferedTimeBeforeRecoveryState = new BufferedTimeBeforeRecoveryState(nextSubState,
					Instant.now(context.clock));
		}

		if (nextSubState == SubState.FINISH_BUFFERED_TIME_BEFORE_RECOVERY) {
			return State.RECOVERY_TIME;
		}

		return State.BUFFERED_TIME_BEFORE_RECOVERY;
	}

	private SubState getNextSubState(Context context) throws OpenemsNamedException {
		return switch (this.bufferedTimeBeforeRecoveryState.subState) {
		case HOLD_BUFFERED_TIME_BEFORE_RECOVERY -> {
			context.ess.setActivePowerEquals(ZERO_WATT_POWER);
			var bufferedDurationExpiration = this.calculateBufferedDurationExpiration(context);
			if (bufferedDurationExpiration >= BUFFER_DURATION_THRESHOLD_SECONDS) {
				yield SubState.BUFFERED_TIME_RECOVERY;
			}
			yield SubState.HOLD_BUFFERED_TIME_BEFORE_RECOVERY;
		}
		case BUFFERED_TIME_RECOVERY -> {
			var minPowerEss = this.calculateMinPower(context.ess);
			context.ess.setActivePowerEquals(minPowerEss);
			var bufferedRecoveryExpiration = this.calculateBufferedRecoveryExpiration(context);
			if (bufferedRecoveryExpiration >= RECOVERY_DURATION_THRESHOLD_MINUTES) {
				yield SubState.FINISH_BUFFERED_TIME_BEFORE_RECOVERY;
			}
			yield SubState.BUFFERED_TIME_RECOVERY;
		}
		case FINISH_BUFFERED_TIME_BEFORE_RECOVERY -> SubState.FINISH_BUFFERED_TIME_BEFORE_RECOVERY;
		};
	}

	private long calculateBufferedDurationExpiration(Context context) {
		return Duration.between(this.bufferedTimeBeforeRecoveryStartTime, Instant.now(context.clock))//
				.toSeconds();
	}

	private long calculateBufferedRecoveryExpiration(Context context) {
		return Duration//
				.between(context.getCycleStart(), Instant.now(context.clock))//
				.toMinutes();
	}

	private int calculateMinPower(ManagedSymmetricEss ess) {
		return (int) (ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE) * EIGHTEENX_PERCENT_OF_MAX_POWER);
	}

	@Override
	protected String debugLog() {
		return State.BUFFERED_TIME_BEFORE_RECOVERY.asCamelCase() + "-"
				+ EnumUtils.nameAsCamelCase(this.bufferedTimeBeforeRecoveryState.subState());
	}
}
