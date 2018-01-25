package io.openems.backend.metadata.odoo;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.backend.metadata.api.MetadataService;
import io.openems.common.OpenemsException;

import org.osgi.service.metatype.annotations.Designate;

@Designate( ocd=OdooImpl.Config.class, factory=true)
@Component(name="io.openems.backend.metadata.odoo")
public class OdooImpl implements MetadataService {

	@ObjectClassDefinition
	@interface Config {
		String name() default "World";
	}

	private String name;

	@Activate
	void activate(Config config) {
		this.name = config.name();
	}

	@Deactivate
	void deactivate() {
	}

	@Override
	public void getInfoWithSession() throws OpenemsException {
		System.out.println("Just saying ");		
	}

}
