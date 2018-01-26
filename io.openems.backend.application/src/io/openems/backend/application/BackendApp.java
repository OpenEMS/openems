package io.openems.backend.application;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.timedata.api.TimedataService;
import io.openems.common.exceptions.OpenemsException;

@Component()
public class BackendApp {

	@Reference
	MetadataService metadataService;
	
	@Reference
	TimedataService timedataService;

	@Activate
	void activate() {
		System.out.println("Activate BackendApp");
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
