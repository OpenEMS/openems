package io.openems.edge.heater.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * A generalized interface for smart grid operation of a heat pump.
 * Contains the most important functions shared by all smart grid ready heat pumps, allowing a vendor 
 * agnostic implementation. Vendor specific interfaces should extend this interface.
 *
 */

public interface HeatpumpSmartGridGeneralizedChannel extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {


        /**
         * Smart Grid state of the heat pump.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 4
         *      <li> State 0: Off
         *      <li> State 1: Smart Grid Low
         *      <li> State 2: Standard
         *      <li> State 3: Smart Grid High
         * </ul>
         */

        SMART_GRID_STATE(Doc.of(SmartGridState.values()).accessMode(AccessMode.READ_WRITE)),


        /**
         * Signals if the heat pump is currently running (true) or not (false).
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */

        RUNNING(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),


        /**
         * Signals an error (true) or no error occuring (false).
         * <ul>
         * 		 <li>Type: boolean
         * </ul>
         */

        ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Signals a warning (true) or no warning occuring (false).
         * <ul>
         * 		 <li>Type: boolean
         * </ul>
         */

        WARNING(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Signals that the chp is ready for operation (true) or not (false).
         * <ul>
         * 		 <li>Type: boolean
         * </ul>
         */

        READY(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
    /**
     * Gets the Channel for {@link ChannelId#SMART_GRID_STATE}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getSmartGridStateChannel() {
        return this.channel(ChannelId.SMART_GRID_STATE);
    }
    
    /**
     * Smart Grid state of the heat pump.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 4
     *      <li> State 0: Off
     *      <li> State 1: Smart Grid Low
     *      <li> State 2: Standard
     *      <li> State 3: Smart Grid High
     * </ul>
	 * See {@link ChannelId#SMART_GRID_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSmartGridState() {
		return this.getSmartGridStateChannel().value();
	}
	
	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SMART_GRID_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSmartGridState(Integer value) {
		this.getSmartGridStateChannel().setNextValue(value);
	}
	
	/**
	 * Turn the CHP on (true) or off (false). See {@link ChannelId#SMART_GRID_STATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSmartGridState(Integer value) throws OpenemsNamedException {
		this.getSmartGridStateChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#RUNNING}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getRunningChannel(){
        return this.channel(ChannelId.RUNNING);
    }
	
    /**
     * Check if the heat pump is running. True means running, false means not running.
	 * See {@link ChannelId#RUNNING}.
     *
	 * @return the Channel {@link Value}
     */
    public default Value<Boolean> getRunning() {
		return this.getRunningChannel().value();
	}

    /**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RUNNING}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRunning(Boolean value) {
		this.getRunningChannel().setNextValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#ERROR}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getErrorChannel(){
        return this.channel(ChannelId.ERROR);
    }
	
    /**
     * Check if an error occurred. False for no error.
	 * See {@link ChannelId#ERROR}.
     *
	 * @return the Channel {@link Value}
     */
    public default Value<Boolean> getError() {
		return this.getErrorChannel().value();
	}

    /**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ERROR}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setError(Boolean value) {
		this.getErrorChannel().setNextValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#WARNING}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getWarningChannel(){
        return this.channel(ChannelId.WARNING);
    }
	
    /**
     * Check if a warning occurred. False for no warning.
	 * See {@link ChannelId#WARNING}.
     *
	 * @return the Channel {@link Value}
     */
    public default Value<Boolean> getWarning() {
		return this.getWarningChannel().value();
	}

    /**
	 * Internal method to set the 'nextValue' on {@link ChannelId#WARNING}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWarning(Boolean value) {
		this.getWarningChannel().setNextValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#READY}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getReadyChannel(){
        return this.channel(ChannelId.READY);
    }
	
    /**
     * Check if the heat pump is ready for operation. True means ready.
	 * See {@link ChannelId#READY}.
     *
	 * @return the Channel {@link Value}
     */
    public default Value<Boolean> getReady() {
		return this.getReadyChannel().value();
	}

    /**
	 * Internal method to set the 'nextValue' on {@link ChannelId#READY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setReady(Boolean value) {
		this.getReadyChannel().setNextValue(value);
	}

}
