package io.openems.edge.core.appmanager.validator;

import java.util.ArrayList;
import java.util.List;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.core.appmanager.ComponentUtil;

public class CheckRelayCount implements Checkable {

	private final ComponentUtil openemsAppUtil;
	private final String io;
	private final int count;

	public CheckRelayCount(ComponentUtil openemsAppUtil, int count) {
		this(openemsAppUtil, null, count);
	}

	public CheckRelayCount(ComponentUtil openemsAppUtil, String io, int count) {
		this.openemsAppUtil = openemsAppUtil;
		this.io = io;
		this.count = count;
	}

	@Override
	public boolean check() {
		try {
			List<String> availableRelays;
			if (this.io != null) {
				availableRelays = this.openemsAppUtil.getAvailableRelays(this.io);
			} else {
				var relay = this.openemsAppUtil.getAvailableRelays().stream().filter(t -> t.relays.size() >= this.count)
						.findFirst().orElse(null);
				availableRelays = relay != null ? relay.relays : new ArrayList<>();
			}
			if (this.count <= availableRelays.size()) {
				return true;
			}
		} catch (OpenemsNamedException e) {
			// io not found so there are none available
		}
		return false;
	}

	@Override
	public String getErrorMessage() {
		// TODO translation
		return "There are not enough Relay ports available!";
	}

}
