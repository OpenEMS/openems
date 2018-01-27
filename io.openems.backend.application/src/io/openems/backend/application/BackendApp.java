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

	// TODO: use setter to not kill BackendApp all the time...
	@Reference
	MetadataService metadataService;
	
	@Reference
	TimedataService timedataService;

	@Activate
	void activate() {
		System.out.println("Activate BackendApp");
		try {
			this.metadataService.getInfoWithSession("8635d53109cafc9d51de443c7d2bc4e980ba1b5d");
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

	@Deactivate
	void deactivate() {
		System.out.println("Deactivate");
	}

}
