package io.openems.edge.core.appmanager.validator.relaycount;

import static io.openems.edge.app.common.props.RelayProps.feneconHomeFilter;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.OpenemsConstants;
import io.openems.common.session.Language;
import io.openems.edge.app.common.props.RelayProps.RelayContactFilter;
import io.openems.edge.core.appmanager.validator.CheckHome;
import io.openems.edge.core.appmanager.validator.Checkable;

@Component(//
		name = HomeFilter.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class HomeFilter implements CheckRelayCountFilter {

	public static final String COMPONENT_NAME = "CheckRelayCount.Filter.Home";

	private final Checkable checkIsHome;

	private boolean onlyHighVoltageRelays;

	@Activate
	public HomeFilter(@Reference(target = "(" + OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME + "="
			+ CheckHome.COMPONENT_NAME + ")") Checkable checkIsHome) {
		super();
		this.checkIsHome = checkIsHome;
	}

	@Override
	public void setProperties(Map<String, ?> parameters) {
		this.onlyHighVoltageRelays = parameters.containsKey("onlyHighVoltageRelays")
				? (boolean) parameters.get("onlyHighVoltageRelays")
				: false;
	}

	@Override
	public RelayContactFilter apply() {
		return feneconHomeFilter(Language.DEFAULT, this.checkIsHome.check(), this.onlyHighVoltageRelays);
	}

}