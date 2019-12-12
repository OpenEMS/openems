package io.openems.edge.bridge.onewire;

import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import com.dalsemi.onewire.adapter.DSPortAdapter;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface BridgeOnewire extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_SELECT_PORT(Doc.of(Level.FAULT));

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

	public default StateChannel getUnableToSelectPortChannel() {
		return this.channel(BridgeOnewire.ChannelId.UNABLE_TO_SELECT_PORT);
	}

	public default void setUnableToSelectPort(boolean value) {
		this.getUnableToSelectPortChannel().setNextValue(value);
	}

}
