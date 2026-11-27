package io.openems.edge.core.appmanager.validator;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;

@Component(//
		name = CheckIndustrialXl.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckIndustrialXl extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckIndustrialXl";

	private final Checkable checkAppsNotInstalled;

	@Activate
	public CheckIndustrialXl(//
			ComponentContext componentContext, //
			@Reference(target = "(" + OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME + "="
					+ CheckAppsNotInstalled.COMPONENT_NAME + ")") Checkable checkAppsNotInstalled //
	) {
		super(componentContext);
		this.checkAppsNotInstalled = checkAppsNotInstalled;
	}

	@Override
	public boolean check() {
		this.checkAppsNotInstalled.setProperties(Checkables.checkAppsNotInstalled(//
				"App.FENECON.Industrial.Xl.IXL010" //
		).properties());

		return !this.checkAppsNotInstalled.check();
	}

	@Override
	public OpenemsNamedException getValidationError(Language language) {
		return OpenemsError.EDGE_APP_VALIDATION_CHECK_INDUSTRIAL_XL.exception(language);
	}

	@Override
	public OpenemsNamedException getInvertedValidationError(Language language) {
		return OpenemsError.EDGE_APP_VALIDATION_CHECK_INDUSTRIAL_XL_INVERTED.exception(language);
	}
}
