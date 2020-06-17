package io.openems.edge.scheduler.api;

import java.util.LinkedHashSet;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@ProviderType
public interface Scheduler extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		RUN_FAILED(Doc.of(Level.FAULT).text("Running the Scheduler failed"));

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
	 * Gets the Channel for {@link ChannelId#RUN_FAILED}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getRunFailedChannel() {
		return this.channel(ChannelId.RUN_FAILED);
	}

	/**
	 * Gets the Run-Failed State. See {@link ChannelId#RUN_FAILED}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRunFailed() {
		return this.getRunFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RUN_FAILED}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setRunFailed(boolean value) {
		this.getRunFailedChannel().setNextValue(value);
	}

	/**
	 * Returns Controllers ordered by their current execution priority.
	 * 
	 * <p>
	 * This method is called once every Cycle, i.e. once per second. The
	 * {@link LinkedHashSet} is used, as it preserves insertion order
	 * 
	 * @return a ordered set of Controllers
	 * @throws OpenemsNamedException on error
	 */
	public LinkedHashSet<Controller> getControllers() throws OpenemsNamedException;

}
