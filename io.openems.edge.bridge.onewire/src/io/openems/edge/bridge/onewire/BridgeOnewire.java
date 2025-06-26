package io.openems.edge.bridge.onewire;

import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import com.dalsemi.onewire.adapter.DSPortAdapter;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface BridgeOnewire extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_SELECT_PORT_FAULT(Doc.of(Level.FAULT));

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
	 * Gets the Channel for {@link ChannelId#UNABLE_TO_SELECT_PORT_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getUnableToSelectPortFaultChannel() {
		return this.channel(ChannelId.UNABLE_TO_SELECT_PORT_FAULT);
	}

	/**
	 * Gets the Unable-To-Select-Port Fault State. See
	 * {@link ChannelId#UNABLE_TO_SELECT_PORT_FAULT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getUnableToSelectPortFault() {
		return this.getUnableToSelectPortFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#UNABLE_TO_SELECT_PORT_FAULT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setUnableToSelectPortFault(boolean value) {
		this.getUnableToSelectPortFaultChannel().setNextValue(value);
	}

	/**
	 * Add a Task.
	 *
	 * <p>
	 * Tasks are executed sequentially in a separate thread.
	 *
	 * @param task the task
	 */
	public void addTask(Consumer<DSPortAdapter> task);

	/**
	 * Removes a Task.
	 *
	 * @param task the task
	 */
	public void removeTask(Consumer<DSPortAdapter> task);

}
