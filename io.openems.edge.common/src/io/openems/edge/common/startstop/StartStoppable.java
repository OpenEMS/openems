package io.openems.edge.common.startstop;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Declares an OpenEMS Component as being able to get started and stopped.
 * 
 * <p>
 * 
 * A device or service inside OpenEMS that implements this OpenEMS Nature can be
 * started or stopped.
 * 
 * <p>
 * 
 * Implementing this Nature also requires the Component to have a configuration
 * property "startStop" of type {@link StartStopConfig} that overrides the logic
 * of the {@link StartStoppable#setStartStop(StartStop)} method:
 * 
 * <pre>
 * 	&#64;AttributeDefinition(name = "Start/stop behaviour?", description = "Should this Component be forced to start or stopp?")
 *	StartStopConfig startStop() default StartStopConfig.AUTO;
 * </pre>
 * 
 * <ul>
 * <li>if config is {@link StartStopConfig#START} -> always start
 * <li>if config is {@link StartStopConfig#STOP} -> always stop
 * <li>if config is {@link StartStopConfig#AUTO} -> start
 * {@link StartStop#UNDEFINED} and wait for a call to
 * {@link #setStartStop(StartStop)}
 * </ul>
 */
public interface StartStoppable extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Start/Stop
		 * 
		 * <ul>
		 * <li>Interface: StartStoppable
		 * <li>Type: {@link StartStop}
		 * <li>Range: 0=Undefined, 1=Start, 2=Stop
		 * </ul>
		 */
		START_STOP(Doc.of(StartStop.values()));

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
	 * Gets the Channel for {@link ChannelId#START_STOP}.
	 * 
	 * @return the Channel
	 */
	public default Channel<StartStop> getStartStopChannel() {
		return this.channel(ChannelId.START_STOP);
	}

	/**
	 * Gets the current {@link StartStop} state of the {@link StartStoppable}
	 * Component. See {@link ChannelId#START_STOP}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default StartStop getStartStop() {
		return this.getStartStopChannel().value().asEnum();
	}

	/**
	 * Starts or stops the device or service represented by this OpenEMS Component.
	 * 
	 * @param value target {@link StartStop} state
	 * @throws OpenemsNamedException on error
	 */
	public default void setStartStop(StartStop value) throws OpenemsNamedException {
		this.getStartStopChannel().setNextValue(value);
		this._setStartStop(value);
	}

	/**
	 * Internal method to handle the start/stop command..
	 * 
	 * @param value target {@link StartStop} state
	 * @throws OpenemsNamedException on error
	 */
	public void _setStartStop(StartStop value) throws OpenemsNamedException;
}
