package io.openems.edge.core.appmanager.validator;

import java.util.TreeMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.OpenemsConstants;
import io.openems.common.session.Language;
import io.openems.edge.common.component.ComponentManager;

@Component(//
		name = CheckHome.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckHome extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckHome";

	private final ComponentManager componentManager;
	private final Checkable checkAppsNotInstalled;

	@Activate
	public CheckHome(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference(target = "(" + OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME + "="
					+ CheckAppsNotInstalled.COMPONENT_NAME + ")") Checkable checkAppsNotInstalled) {
		super(componentContext);
		this.componentManager = componentManager;
		this.checkAppsNotInstalled = checkAppsNotInstalled;
	}

	@Override
	public boolean check() {
		var batteries = this.componentManager.getEdgeConfig().getComponentsByFactory("Battery.Fenecon.Home");
		this.checkAppsNotInstalled.setProperties(new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
				.put("appIds", new String[] { "App.FENECON.Home" }) //
				.build());
		// TODO remove check for batteries
		// not every home has the home app installed but if a batterie of an home is
		// installed its probably a home and so the app can be used.
		// later there should only be checked if the home app is installed because if
		// the configuration is wrong there may be no home battery installed and so the
		// app wouldn't be available even though it is a home
		return !batteries.isEmpty() || !this.checkAppsNotInstalled.check();
	}

	@Override
	public String getErrorMessage(Language language) {
		return AbstractCheckable.getTranslation(language, "Validator.Checkable.CheckHome.Message");
	}

}
