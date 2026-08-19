package io.openems.edge.core.appmanager.validator.relaycount;

import static io.openems.edge.app.common.props.RelayProps.techbaseCm4Gen3Filter;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.session.Language;
import io.openems.edge.app.common.props.RelayProps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

@Component(//
		name = TechbaseCm4sGen3Filter.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class TechbaseCm4sGen3Filter implements CheckRelayCountFilter {

	public static final String COMPONENT_NAME = "CheckRelayCount.Filter.TechbaseCm4sGen3";

	private boolean onlyHighVoltageRelays;

	private OpenemsAppInstance deviceHardware;

	@Override
	public void setProperties(Map<String, ?> parameters) {
		this.onlyHighVoltageRelays = parameters.containsKey("onlyHighVoltageRelays")
				&& (boolean) parameters.get("onlyHighVoltageRelays");
		this.deviceHardware = parameters.containsKey("deviceHardware")
				? (OpenemsAppInstance) parameters.get("deviceHardware")
				: null;
	}

	@Override
	public RelayProps.RelayContactFilter apply() {
		return techbaseCm4Gen3Filter(Language.DEFAULT, this.onlyHighVoltageRelays, this.deviceHardware);
	}
}
