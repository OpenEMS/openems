package io.openems.edge.core.appmanager.validator;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.OpenemsApp;

@Component(//
		name = CheckRelayCount.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckRelayCount extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckRelayCount";

	private final ComponentUtil openemsAppUtil;
	private final OpenemsApp relayApp;

	private String io;
	private int count;

	private int availableRelays;

	@Activate
	public CheckRelayCount(//
			@Reference ComponentUtil openemsAppUtil, //
			ComponentContext componentContext, //
			@Reference(target = "(" + OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME
					+ "=App.Hardware.KMtronic8Channel)") OpenemsApp relayApp //
	) {
		super(componentContext);
		this.openemsAppUtil = openemsAppUtil;
		this.relayApp = relayApp;
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
			int availableRelays;
			if (this.io != null) {
				availableRelays = this.openemsAppUtil.getAvailableRelays(this.io).size();
			} else {
				availableRelays = this.openemsAppUtil.getAvailableRelays().stream() //
						.mapToInt(t -> t.relays.size()) //
						.max() //
						.orElse(0);
			}
			this.availableRelays = availableRelays;
			if (this.count <= availableRelays) {
				return true;
			}
		} catch (OpenemsNamedException e) {
			// io not found so there are none available
			this.availableRelays = 0;
		}
		return false;
	}

	@Override
	public String getErrorMessage(Language language) {
		final var messageBuilder = new StringBuilder(//
				AbstractCheckable.getTranslation(language, //
						"Validator.Checkable.CheckRelayCount.Message", //
						this.count, this.availableRelays) //
		);
		
		// message to install additional relay
		if (this.relayApp != null) {
			messageBuilder.append(//
					AbstractCheckable.getTranslation(language, //
							"Validator.Checkable.CheckRelayCount.Message.AdditionalRelay", //
							this.relayApp.getAppDescriptor().getWebsiteUrl(), //
							this.relayApp.getName(language)) //
			);
		}
		return messageBuilder.toString();
	}

}
