package io.openems.edge.common.startstop;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
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
	 * Gets the Start/Stop-Channel. Referrs to {@link ChannelId#START_STOP}. Values
	 * are of type {@link StartStop}.
	 * 
	 * @return the {@link EnumReadChannel}
	 */
	public default EnumReadChannel getStartStopChannel() {
		return this.channel(ChannelId.START_STOP);
	}

	/**
	 * Gets the current {@link StartStop} state of the {@link StartStoppable}
	 * Component.
	 * 
	 * @return the current state
	 */
	public default StartStop getStartStop() {
		return this.getStartStopChannel().value().asEnum();
	}

	/**
	 * Starts or stops the device or service represented by this OpenEMS Component.
	 * 
	 * @param value target {@link StartStop} state
	 */
	public void setStartStop(StartStop value);
}
