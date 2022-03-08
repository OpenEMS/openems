package io.openems.edge.scheduler.api;

import java.util.LinkedHashSet;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Scheduler extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CONTROLLER_IS_MISSING(Doc.of(Level.INFO) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("A configured Controller is missing"));

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
	 * Gets the Channel for {@link ChannelId#CONTROLLER_IS_MISSING}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getControllerIsMissingChannel() {
		return this.channel(ChannelId.CONTROLLER_IS_MISSING);
	}

	/**
	 * Gets the Run-Failed State. See {@link ChannelId#CONTROLLER_IS_MISSING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getControllerIsMissing() {
		return this.getControllerIsMissingChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONTROLLER_IS_MISSING} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setControllerIsMissing(boolean value) {
		this.getControllerIsMissingChannel().setNextValue(value);
	}

	/**
	 * Returns Component-IDs of Controllers ordered by their current execution
	 * priority.
	 *
	 * <p>
	 * This method is called once every Cycle, i.e. once per second. The
	 * {@link LinkedHashSet} is used, as it preserves insertion order
	 *
	 * @return a ordered set of Component-IDs of Controllers
	 */
	public LinkedHashSet<String> getControllers();

}
