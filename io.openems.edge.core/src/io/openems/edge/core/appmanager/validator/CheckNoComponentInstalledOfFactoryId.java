package io.openems.edge.core.appmanager.validator;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.edge.common.component.ComponentManager;

@Component(name = CheckNoComponentInstalledOfFactoryId.COMPONENT_NAME)
public class CheckNoComponentInstalledOfFactoryId implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckNoComponentInstalledOfFactorieId";

	private final ComponentManager componentManager;

	private String factorieId;

	@Activate
	public CheckNoComponentInstalledOfFactoryId(@Reference ComponentManager componentManager) {
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
	public String getErrorMessage() {
		return "Components with the FactorieID[" + this.factorieId + "] are installed!" + System.lineSeparator()
				+ "Remove them to be able to install this app.";
	}

}
