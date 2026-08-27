package io.openems.edge.core.appmanager.validator.relaycount;

import static io.openems.edge.app.common.props.RelayProps.gpioFilter;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.edge.app.common.props.RelayProps;

@Component(//
		name = GpioFilter.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class GpioFilter implements CheckRelayCountFilter {

	public static final String COMPONENT_NAME = "CheckRelayCount.Filter.Gpio";

	@Override
	public RelayProps.RelayContactFilter apply() {
		return gpioFilter();
	}
}
