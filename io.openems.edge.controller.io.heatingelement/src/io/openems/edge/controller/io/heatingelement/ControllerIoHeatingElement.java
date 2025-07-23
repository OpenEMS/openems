package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Status;

public interface ControllerIoHeatingElement extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		LEVEL(Doc.of(Level.values()) //
				.text("Current Level") //
				.persistencePriority(HIGH)),
		AWAITING_HYSTERESIS(Doc.of(INTEGER)), //
		PHASE1_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		PHASE2_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		PHASE3_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		/*
		 * LEVELx_TIME was used for old history view. It is left for the analysis of the
		 * forced duration on a day.
		 */
		LEVEL1_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		LEVEL2_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		LEVEL3_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //

		/*
		 * Total active Time of each Level.
		 */
		LEVEL1_CUMULATED_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		LEVEL2_CUMULATED_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		LEVEL3_CUMULATED_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		TOTAL_PHASE_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		FORCE_START_AT_SECONDS_OF_DAY(Doc.of(INTEGER)//
				.unit(SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)),
		STATUS(Doc.of(Status.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //

		PHASE1_AVG_POWER(Doc.of(INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		PHASE2_AVG_POWER(Doc.of(INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		PHASE3_AVG_POWER(Doc.of(INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		WAITING_FOR_CALIBRATION(Doc.of(OpenemsType.BOOLEAN)),

		SESSION_ENERGY(Doc.of(LONG) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#SESSION_ENERGY}.
	 * 
	 * @return the Channel
	 */
	public default LongReadChannel getSessionEnergyChannel() {
		return this.channel(ChannelId.SESSION_ENERGY);
	}

	/**
	 * Gets the session energy for the mode Energy function. See
	 * {@link ChannelId#SESSION_ENERGY}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getSessionEnergy() {
		return this.getSessionEnergyChannel().value();
	}

	/**
	 * Sets the next value on {@link ChannelId#SESSION_ENERGY} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setSessionEnergy(double value) {
		this.channel(ChannelId.SESSION_ENERGY).setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WAITING_FOR_CALIBRATION}.
	 * 
	 * @return the Channel
	 */
	public default BooleanReadChannel getWaitingForCalibrationChannel() {
		return this.channel(ChannelId.WAITING_FOR_CALIBRATION);
	}

	/**
	 * Gets the boolean value if it should still calibrate or not. See
	 * {@link ChannelId#WAITING_FOR_CALIBRATION}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getWaitingForCalibration() {
		return this.getWaitingForCalibrationChannel().value();
	}

	/**
	 * Sets the next value on {@link ChannelId#WAITING_FOR_CALIBRATION} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setWaitingForCalibration(boolean value) {
		this.channel(ChannelId.WAITING_FOR_CALIBRATION).setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#LEVEL}.
	 * 
	 * @return the Channel
	 */
	public default Channel<Level> getLevelChannel() {
		return this.channel(ChannelId.LEVEL);
	}

	/**
	 * Gets the Level of the heating element. See {@link ChannelId#LEVEL}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Level> getLevel() {
		return this.getLevelChannel().value();
	}

	/**
	 * Sets the next value on {@link ChannelId#LEVEL}.
	 * 
	 * @param value the next value
	 */
	public default void _setLevel(Level value) {
		this.channel(ChannelId.LEVEL).setNextValue(value);
	}

	/**
	 * Sets the next value on {@link ChannelId#FORCE_START_AT_SECONDS_OF_DAY}.
	 * 
	 * @param value the next value
	 */
	public default void _setForceStartAtSecondsOfDay(Integer value) {
		this.channel(ChannelId.FORCE_START_AT_SECONDS_OF_DAY).setNextValue(value);
	}

	/**
	 * Sets the next value on {@link ChannelId#AWAITING_HYSTERESIS}.
	 * 
	 * @param value the next value
	 */
	public default void _setAwaitingHysteresis(boolean value) {
		this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(value);
	}

	/**
	 * Sets the next value on {@link ChannelId#STATUS}.
	 * 
	 * @param value the next value
	 */
	public default void _setStatus(Status value) {
		this.channel(ChannelId.STATUS).setNextValue(value);
	}

	/**
	 * Sets the next value on {@link ChannelId#PHASE1_TIME}.
	 * 
	 * @param value the next value
	 */
	public default void _setPhase1Time(int value) {
		this.channel(ChannelId.PHASE1_TIME).setNextValue(value);
	}

	/**
	 * Sets the next value on {@link ChannelId#PHASE2_TIME}.
	 * 
	 * @param value the next value
	 */
	public default void _setPhase2Time(int value) {
		this.channel(ChannelId.PHASE2_TIME).setNextValue(value);
	}

	/**
	 * Sets the next value on {@link ChannelId#PHASE3_TIME}.
	 * 
	 * @param value the next value
	 */
	public default void _setPhase3Time(int value) {
		this.channel(ChannelId.PHASE3_TIME).setNextValue(value);
	}

	/**
	 * Sets the next value on {@link ChannelId#TOTAL_PHASE_TIME}.
	 * 
	 * @param value the next value
	 */
	public default void _setTotalPhaseTime(int value) {
		this.channel(ChannelId.TOTAL_PHASE_TIME).setNextValue(value);
	}

	/**
	 * Sets the next value on {@link ChannelId#LEVEL1_TIME}.
	 * 
	 * @param value the next value
	 */
	public default void _setLevel1Time(int value) {
		this.channel(ChannelId.LEVEL1_TIME).setNextValue(value);
	}

	/**
	 * Sets the next value on {@link ChannelId#LEVEL2_TIME}.
	 * 
	 * @param value the next value
	 */
	public default void _setLevel2Time(int value) {
		this.channel(ChannelId.LEVEL2_TIME).setNextValue(value);
	}

	/**
	 * Sets the next value on {@link ChannelId#LEVEL3_TIME}.
	 * 
	 * @param value the next value
	 */
	public default void _setLevel3Time(int value) {
		this.channel(ChannelId.LEVEL3_TIME).setNextValue(value);
	}

}