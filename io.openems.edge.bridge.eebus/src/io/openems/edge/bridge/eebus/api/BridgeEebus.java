package io.openems.edge.bridge.eebus.api;

import com.google.common.collect.ImmutableList;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.eebus.Config;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

import java.util.List;

import static org.osgi.framework.BundlePermission.REQUIRE;

public interface BridgeEebus extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		INITIALIZE_FAILURE(Doc.of(Level.FAULT)),

		OWN_SKI(Doc.of(OpenemsType.STRING)
				.accessMode(AccessMode.READ_ONLY)),

		LPC_CURRENT_LIMIT(Doc.of(OpenemsType.LONG)
				.unit(Unit.WATT)
				.accessMode(AccessMode.READ_ONLY)
				.persistencePriority(PersistencePriority.HIGH)),

		LPP_CURRENT_LIMIT(Doc.of(OpenemsType.LONG)
				.unit(Unit.WATT)
				.accessMode(AccessMode.READ_ONLY)
				.persistencePriority(PersistencePriority.HIGH)),

		;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	ImmutableList<EebusPeer> getPeers();

	String[] getTrustedSkis();

	EebusUseCaseManager getUseCaseManager();

}
