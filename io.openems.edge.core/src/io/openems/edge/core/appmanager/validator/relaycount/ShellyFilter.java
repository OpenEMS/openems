package io.openems.edge.core.appmanager.validator.relaycount;

import static io.openems.edge.app.common.props.RelayProps.shellyFilter;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.edge.app.common.props.RelayProps;

@Component(//
		name = ShellyFilter.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class ShellyFilter implements CheckRelayCountFilter {

	public static final String COMPONENT_NAME = "CheckRelayCount.Filter.Shelly";

	@Override
	public RelayProps.RelayContactFilter apply() {
		return shellyFilter();
	}
}
