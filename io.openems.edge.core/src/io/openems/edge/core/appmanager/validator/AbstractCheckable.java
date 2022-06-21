package io.openems.edge.core.appmanager.validator;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

public abstract class AbstractCheckable implements Checkable {

	private final ComponentContext componentContext;

	public AbstractCheckable(ComponentContext componentContext) {
		this.componentContext = componentContext;
	}

	@Override
	public String getComponentName() {
		return this.componentContext.getProperties().get(ComponentConstants.COMPONENT_NAME).toString();
	}

}
