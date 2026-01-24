package io.openems.edge.core.appmanager.validator;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.OpenemsConstants;
import io.openems.common.session.Language;

@Component(//
		name = CheckCommercial50Gen3.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckCommercial50Gen3 extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckCommercial50Gen3";

	private final Checkable checkAppsNotInstalled;

	@Activate
	public CheckCommercial50Gen3(//
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
				"App.FENECON.Commercial.50.Gen3" //
		).properties());

		return !this.checkAppsNotInstalled.check();
	}

	@Override
	public String getErrorMessage(Language language) {
		return AbstractCheckable.getTranslation(language, "Validator.Checkable.CheckCommercial50Gen3.Message");
	}

	@Override
	public String getInvertedErrorMessage(Language language) {
		return AbstractCheckable.getTranslation(language, "Validator.Checkable.CheckCommercial50Gen3.Message.Inverted");
	}

}
