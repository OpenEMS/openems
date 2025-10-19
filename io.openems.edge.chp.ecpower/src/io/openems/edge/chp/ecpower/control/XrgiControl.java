package io.openems.edge.chp.ecpower.control;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;

public interface XrgiControl extends ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		
		CHP_PREPARING(Doc.of(OpenemsType.INTEGER) // use of Integer for future implementation of additional preparation methods
				.accessMode(AccessMode.READ_WRITE)
				.text("Prepare CHP for operation")),	
		
		POWER_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)),

		REGULATION_STEPS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)),

		ACTIVE_REGULATION_STEP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)),
		
		


		/**
		 * Active Power Target.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#WATT}
		 * <li>Range: positive values
		 * </ul>
		 */
		ACTIVE_POWER_TARGET(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE).persistencePriority(PersistencePriority.HIGH)), //
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
	


	//
	public default void _setActivePowerTarget(Integer value) {
		this.getActivePowerTargetChannel().setNextValue(value);
	}

	public default void _setActivePowerTarget(int value) {
		this.getActivePowerTargetChannel().setNextValue(value);
	}

	public default Value<Integer> geActivePowerTarget() {
		return this.getActivePowerTargetChannel().value();
	}

	public default IntegerReadChannel getActivePowerTargetChannel() {
		return this.channel(ChannelId.ACTIVE_POWER_TARGET);
	}

	//
	public default void _setPowerPercent(Integer value) throws OpenemsNamedException {
		this.getSetPowerPercentChannel().setNextWriteValue(value);
	}

	public default void _setPowerPercent(int value) throws OpenemsNamedException {
		this.getSetPowerPercentChannel().setNextWriteValue(value);
	}

	public default Value<Integer> getPowerPercent() {
		return this.getSetPowerPercentChannel().value();
	}

	public default IntegerReadChannel getPowerPercentChannel() {
		return this.channel(ChannelId.POWER_PERCENT);
	}

	public default IntegerWriteChannel getSetPowerPercentChannel() {
		return this.channel(ChannelId.POWER_PERCENT);
	}
	
	
	//
	public default void _setActiveRegulationStep(Integer value) {
		this.getActiveRegulationStepChannel().setNextValue(value);
	}

	public default void _setActiveRegulationStep(int value) {
		this.getActiveRegulationStepChannel().setNextValue(value);
	}

	public default Value<Integer> getActiveRegulationStep() {
		return this.getActiveRegulationStepChannel().value();
	}

	public default IntegerReadChannel getActiveRegulationStepChannel() {
		return this.channel(ChannelId.ACTIVE_REGULATION_STEP);
	}

	//
	public default void _setRegulationSteps(Integer value) {
		this.getSetRegulationStepsChannel().setNextValue(value);
	}

	public default void _setRegulationSteps(int value) {
		this.getSetRegulationStepsChannel().setNextValue(value);
	}

	public default Value<Integer> getRegulationSteps() {
		return this.getRegulationStepsChannel().value();
	}

	public default IntegerReadChannel getRegulationStepsChannel() {
		return this.channel(ChannelId.REGULATION_STEPS);
	}

	public default IntegerWriteChannel getSetRegulationStepsChannel() {
		return this.channel(ChannelId.REGULATION_STEPS);
	}


	//
	/**
	 * Gets the Channel for {@link ChannelId#CHP_PREPARING}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getChpPreparationChannel() {
		return this.channel(ChannelId.CHP_PREPARING);
	}

	/**
	 * Gets the Active Power Limit in [W]. See {@link ChannelId#CHP_PREPARING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChpPreparation() {
		return this.getChpPreparationChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHP_PREPARING} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChpPreparation(Integer value) {
		this.getChpPreparationChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHP_PREPARING} Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException 
	 */
	public default void setChpPreparation(int value) throws OpenemsNamedException {
		this.getChpPreparationChannel().setNextWriteValue(value);
	}	
	
	
	void applyPower(int activePowerTarget);

	void applyPower(Integer activePowerTarget);

	void applyPreparation(Boolean activate);

}
