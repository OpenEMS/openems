package io.openems.edge.ess.offgrid.api;

import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;

/**
 * Represents a device that can be used to determine the grid status (On-Grid or
 * Off-Grid) and actively switch connection between On-Grid and Off-Grid.
 */
public interface OffGridSwitch extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Main-Contactor connects the inverter to the public grid.
		 *
		 * <ul>
		 * <li>Interface: {@link OffGridSwitch}
		 * <li>Type: Boolean
		 * <li>Range: 0=CONTACTOR TURNED ON, 1=CONTACTOR TURNED OFF
		 * </ul>
		 */
		MAIN_CONTACTOR(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Grounding-Contactor connects the inverter grounding in off-grid to neutral.
		 *
		 * <ul>
		 * <li>Interface: {@link OffGridSwitch}
		 * <li>Type: Boolean
		 * <li>Range: 0=CONTACTOR TURNED ON, 1=CONTACTOR TURNED OFF
		 * </ul>
		 */
		GROUNDING_CONTACTOR(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Grid Mode.
		 *
		 * <ul>
		 * <li>Interface: {@link OffGridSwitch}
		 * <li>Type: {@link GridMode}
		 * </ul>
		 */
		GRID_MODE(Doc.of(GridMode.values())//
				.accessMode(AccessMode.READ_ONLY)),//
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

	/**
	 * Gets the Channel for {@link ChannelId#MAIN_CONTACTOR}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getMainContactorChannel() {
		return this.channel(ChannelId.MAIN_CONTACTOR);
	}

	/**
	 * Gets the Main Contactor relay state. It is Normally-Closed.
	 * {@link ChannelId#MAIN_CONTACTOR}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Optional<Contactor> getMainContactor() {
		var mainContactor = this.getMainContactorChannel().value();
		return mainContactor.asOptional().map(value -> {
			return value ? Contactor.OPEN : Contactor.CLOSE;
		});
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAIN_CONTACTOR}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMainContactor(Boolean value) {
		this.getMainContactorChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GROUNDING_CONTACTOR}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getGroundingContactorChannel() {
		return this.channel(ChannelId.GROUNDING_CONTACTOR);
	}

	/**
	 * Gets the Grounding Contactor relay state. It is Normally-Open.
	 * {@link ChannelId#GROUNDING_CONTACTOR}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Optional<Contactor> getGroundingContactor() {
		var groundingContactor = this.getGroundingContactorChannel().value();
		return groundingContactor.asOptional().map(value -> {
			return value ? Contactor.CLOSE : Contactor.OPEN;
		});
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GROUNDING_CONTACTOR} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGroundingContactor(Boolean value) {
		this.getGroundingContactorChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<GridMode> getGridModeChannel() {
		return this.channel(ChannelId.GRID_MODE);
	}

	/**
	 * {@link ChannelId#GRID_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default GridMode getGridMode() {
		return this.getGridModeChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_MODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridMode(GridMode value) {
		this.getGridModeChannel().setNextValue(value);
	}

	public static enum Contactor {
		/**
		 * <ul>
		 * <li>In Normally-Open: Switch the relay off.
		 * <li>In Normally-Close: Switch the relay on.
		 * </ul>
		 */
		OPEN, //
		/**
		 * <ul>
		 * <li>In Normally-Open: Switch the relay on.
		 * <li>In Normally-Close: Switch the relay off.
		 * </ul>
		 */
		CLOSE;
	}

	/**
	 * Set the Main-Contactor, which connects the inverter to the public grid.
	 *
	 * <ul>
	 * <li>OPEN: disconnect inverter from public grid
	 * <li>CLOSE: connect inverter to public grid
	 * </ul>
	 *
	 * @param operation {@link Contactor} operation
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	public void setMainContactor(Contactor operation) throws IllegalArgumentException, OpenemsNamedException;

	/**
	 * Sets the Grounding-Contactor, which connects the inverter grounding in
	 * off-grid to neutral.
	 *
	 * <ul>
	 * <li>OPEN: disconnects inverter grounding from neutral
	 * <li>CLOSE: connects in inverter grounding to neutral
	 * </ul>
	 *
	 * @param operation {@link Contactor} operation
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	public void setGroundingContactor(Contactor operation) throws IllegalArgumentException, OpenemsNamedException;

}
