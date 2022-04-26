package io.openems.edge.core.appmanager.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.core.appmanager.ComponentUtil;

@Component(name = CheckRelayCount.COMPONENT_NAME)
public class CheckRelayCount implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckRelayCount";

	private final ComponentUtil openemsAppUtil;
	private String io;
	private int count;

	@Activate
	public CheckRelayCount(@Reference ComponentUtil openemsAppUtil) {
		this.openemsAppUtil = openemsAppUtil;
	}

	private void init(String io, int count) {
		this.io = io;
		this.count = count;
	}

	@Override
	public void setProperties(Map<String, ?> properties) {
		var io = (String) properties.get("io");
		var count = (int) properties.get("count");
		this.init(io, count);
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
