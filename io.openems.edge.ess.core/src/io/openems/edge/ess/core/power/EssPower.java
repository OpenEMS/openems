package io.openems.edge.ess.core.power;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.SolverStrategy;

public interface EssPower extends OpenemsComponent, EventHandler, Power {

	public static final String SINGLETON_SERVICE_PID = "Ess.Power";
	public static final String SINGLETON_COMPONENT_ID = "_power";

	public static final boolean DEFAULT_SYMMETRIC_MODE = true;
	public static final boolean DEFAULT_DEBUG_MODE = false;
	public static final SolverStrategy DEFAULT_SOLVER_STRATEGY = SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * The duration needed for solving the Power.
		 *
		 * <ul>
		 * <li>Interface: PowerComponent
		 * <li>Type: Integer
		 * <li>Unit: milliseconds
		 * <li>Range: positive
		 * </ul>
		 */
		SOLVE_DURATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS)),
		/**
		 * The eventually used solving strategy.
		 *
		 * <ul>
		 * <li>Interface: PowerComponent
		 * <li>Type: Integer
		 * <li>Unit: milliseconds
		 * <li>Range: positive
		 * </ul>
		 */
		SOLVE_STRATEGY(Doc.of(SolverStrategy.values())),
		/**
		 * Whether the Power problem could be solved.
		 *
		 * <ul>
		 * <li>Interface: PowerComponent
		 * <li>Type: Boolean
		 * </ul>
		 */
		NOT_SOLVED(Doc.of(Level.WARNING)),
		/**
		 * Gets set, when setting static Constraints failed.
		 *
		 * <ul>
		 * <li>Interface: PowerComponent
		 * <li>Type: Boolean
		 * </ul>
		 */
		STATIC_CONSTRAINTS_FAILED(Doc.of(Level.FAULT));

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
	 * Gets the Channel for {@link ChannelId#NOT_SOLVED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getNotSolvedChannel() {
		return this.channel(ChannelId.NOT_SOLVED);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#NOT_SOLVED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setNotSolved(boolean value) {
		this.getNotSolvedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SOLVE_DURATION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSolveDurationChannel() {
		return this.channel(ChannelId.SOLVE_DURATION);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SOLVE_DURATION}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSolveDuration(int value) {
		this.getSolveDurationChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SOLVE_STRATEGY}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getSolveStrategyChannel() {
		return this.channel(ChannelId.SOLVE_STRATEGY);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SOLVE_STRATEGY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSolveStrategy(SolverStrategy value) {
		this.getSolveStrategyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#STATIC_CONSTRAINTS_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getStaticConstraintsFailedChannel() {
		return this.channel(ChannelId.STATIC_CONSTRAINTS_FAILED);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#STATIC_CONSTRAINTS_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStaticConstraintsFailed(boolean value) {
		this.getStaticConstraintsFailedChannel().setNextValue(value);
	}

}
