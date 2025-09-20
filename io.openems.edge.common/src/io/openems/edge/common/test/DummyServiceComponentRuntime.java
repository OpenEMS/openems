package io.openems.edge.common.test;

import java.util.ArrayList;
import java.util.Collection;

import org.osgi.framework.Bundle;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.promise.Promise;

public class DummyServiceComponentRuntime implements ServiceComponentRuntime {

	@Override
	public Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs(Bundle... bundles) {
		return new ArrayList<>();
	}

	@Override
	public ComponentDescriptionDTO getComponentDescriptionDTO(Bundle bundle, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(ComponentDescriptionDTO description) {
		return new ArrayList<>();
	}

	@Override
	public boolean isComponentEnabled(ComponentDescriptionDTO description) {
		return true;
	}

	@Override
	public Promise<Void> enableComponent(ComponentDescriptionDTO description) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Promise<Void> disableComponent(ComponentDescriptionDTO description) {
		// TODO Auto-generated method stub
		return null;
	}

}
