package io.openems.backend.application;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.openems.backend.metadata.api.MetadataService;
import io.openems.common.OpenemsException;

@Component()
public class BackendApp {

	@Reference
	MetadataService metadataService;

	@Activate
	void activate() {
		System.out.println("Activate");
		System.out.println("Meta: " + this.metadataService);
		try {
			this.metadataService.getInfoWithSession();
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

	@Deactivate
	void deactivate() {
		System.out.println("Deactivate");
	}

}
