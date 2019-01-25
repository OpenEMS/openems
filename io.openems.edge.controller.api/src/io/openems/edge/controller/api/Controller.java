package io.openems.edge.controller.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Controller extends OpenemsComponent {

	/**
	 * Executes the Controller logic.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void run() throws OpenemsNamedException;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		RUN_FAILED(new Doc().level(Level.FAULT).text("Running the Controller failed"));

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
	 * Gets the "RunFailed" State-Channel.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getRunFailed() {
		return this.channel(ChannelId.RUN_FAILED);
	}

}
