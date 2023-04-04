package io.openems.edge.core.appmanager.validator;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.session.Language;
import io.openems.edge.common.component.ComponentManager;

@Component(//
		name = CheckNoComponentInstalledOfFactoryId.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckNoComponentInstalledOfFactoryId extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckNoComponentInstalledOfFactorieId";

	private final ComponentManager componentManager;

	private String factorieId;

	@Activate
	public CheckNoComponentInstalledOfFactoryId(@Reference ComponentManager componentManager,
			ComponentContext componentContext) {
		super(componentContext);
		this.componentManager = componentManager;
	}

	@Override
	public void setProperties(Map<String, ?> properties) {
		this.factorieId = (String) properties.get("factorieId");
	}

	@Override
	public boolean check() {
		return this.componentManager.getEdgeConfig().getComponentIdsByFactory(this.factorieId).isEmpty();
	}

	@Override
	public String getErrorMessage(Language language) {
		return AbstractCheckable.getTranslation(language,
				"Validator.Checkable.CheckNoComponentInstalledOfFactorieId.Message", this.factorieId);
	}

}
