package io.openems.edge.controller.symmetric.thresholdpeakshaver;

import static io.openems.common.channel.PersistencePriority.HIGH;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;



public interface ControllerEssThresholdPeakshaver extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine").persistencePriority(HIGH)), //
		
		PEAK_SHAVING_STATE_MACHINE(Doc.of(PeakshavingState.values()) //
				.text("Current State of Peakshaving").persistencePriority(HIGH)), //
		
		CALCULATED_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)),
		PEAK_SHAVED_GRID_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT).persistencePriority(HIGH)),
		GRID_POWER_WITHOUT_PEAK_SHAVING(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)),;		
		
		
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	// ----------------------------------------
	// PeakShavingStateMachine Channel
	// ----------------------------------------

	/**
	 * Gets the Channel for {@link ChannelId#PEAK_SHAVING_STATE_MACHINE}.
	 *
	 * @return the Channel
	 */
	public default Channel<PeakshavingState> getPeakShavingStateMachineChannel() {
		return this.channel(ChannelId.PEAK_SHAVING_STATE_MACHINE);
	}

	/**
	 * Gets current state of the {@link PeakshavingState}. See
	 * {@link ChannelId#PEAK_SHAVING_STATE_MACHINE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default PeakshavingState getPeakShavingStateMachine() {
		return this.getPeakShavingStateMachineChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PEAK_SHAVING_STATE_MACHINE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPeakShavingStateMachine(PeakshavingState value) {
		this.getPeakShavingStateMachineChannel().setNextValue(value);
	}	

	// ----------------------------------------
	// StateMachine Channel
	// ----------------------------------------

	/**
	 * Gets the Channel for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel
	 */
	public default Channel<State> getStateMachineChannel() {
		return this.channel(ChannelId.STATE_MACHINE);
	}

	/**
	 * Gets current state of the {@link StateMachine}. See
	 * {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default State getStateMachine() {
		return this.getStateMachineChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#STATE_MACHINE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStateMachine(State value) {
		this.getStateMachineChannel().setNextValue(value);
	}

	// ----------------------------------------
	// CalculatedPower Channel
	// ----------------------------------------

	/**
	 * Gets the Channel for {@link ChannelId#CALCULATED_POWER}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getCalculatedPowerChannel() {
		return this.channel(ChannelId.CALCULATED_POWER);
	}

	/**
	 * Gets the current value of Calculated Power. See {@link ChannelId#CALCULATED_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getCalculatedPower() {
		return this.getCalculatedPowerChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CALCULATED_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCalculatedPower(Integer value) {
		this.getCalculatedPowerChannel().setNextValue(value);
	}

	// ----------------------------------------
	// PeakShavedGridPower Channel
	// ----------------------------------------

	/**
	 * Gets the Channel for {@link ChannelId#PEAK_SHAVED_GRID_POWER}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPeakShavedGridPowerChannel() {
		return this.channel(ChannelId.PEAK_SHAVED_GRID_POWER);
	}

	/**
	 * Gets the current value of Peak Shaved Grid Power. See {@link ChannelId#PEAK_SHAVED_GRID_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPeakShavedGridPower() {
		return this.getPeakShavedGridPowerChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PEAK_SHAVED_GRID_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPeakShavedGridPower(Integer value) {
		this.getPeakShavedGridPowerChannel().setNextValue(value);
	}

	// ----------------------------------------
	// GridPowerWithoutPeakShaving Channel
	// ----------------------------------------

	/**
	 * Gets the Channel for {@link ChannelId#GRID_POWER_WITHOUT_PEAK_SHAVING}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getGridPowerWithoutPeakShavingChannel() {
		return this.channel(ChannelId.GRID_POWER_WITHOUT_PEAK_SHAVING);
	}

	/**
	 * Gets the current value of Grid Power Without Peak Shaving. See {@link ChannelId#GRID_POWER_WITHOUT_PEAK_SHAVING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getGridPowerWithoutPeakShaving() {
		return this.getGridPowerWithoutPeakShavingChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_POWER_WITHOUT_PEAK_SHAVING}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridPowerWithoutPeakShaving(Integer value) {
		this.getGridPowerWithoutPeakShavingChannel().setNextValue(value);
	}

}
