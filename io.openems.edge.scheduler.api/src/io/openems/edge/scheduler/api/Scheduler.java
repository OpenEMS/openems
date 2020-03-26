package io.openems.edge.scheduler.api;

import java.util.LinkedHashSet;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
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

	/**
	 * Gets the "RunFailed" State-Channel.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getRunFailed() {
		return this.channel(ChannelId.RUN_FAILED);
	}
}
