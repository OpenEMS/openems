package io.openems.edge.core.appmanager.validator;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.evcs.api.Evcs;

@Component(//
		name = CheckEvcsNotInstalled.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckEvcsNotInstalled implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckEvcsNotInstalled";

	private final ComponentManager componentManager;

	@Activate
	public CheckEvcsNotInstalled(@Reference ComponentManager componentManager) {
		this.componentManager = componentManager;
	}

	@Override
	public String getComponentName() {
		return COMPONENT_NAME;
	}

	@Override
	public boolean check() {
		return this.componentManager.getEnabledComponentsOfType(Evcs.class).isEmpty();
	}

	@Override
	public OpenemsNamedException getValidationError(Language language) {
		return OpenemsError.EDGE_APP_VALIDATION_CHECK_EVCS_NOT_INSTALLED.exception(language);
	}

	@Override
	public OpenemsNamedException getInvertedValidationError(Language language) {
		return OpenemsError.EDGE_APP_VALIDATION_CHECK_EVCS_NOT_INSTALLED_INVERTED.exception(language);
	}
}
