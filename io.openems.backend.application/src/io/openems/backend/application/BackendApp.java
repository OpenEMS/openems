package io.openems.backend.application;

import java.io.IOException;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;

import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.timedata.api.TimedataService;
import io.openems.common.exceptions.OpenemsException;

@Component()
public class BackendApp {

	@Reference
	MetadataService metadataService;

	@Reference
	TimedataService timedataService;

	@Reference
	LogService log;

	@Reference
	ConfigurationAdmin admin;

	@Activate
	void activate() {
		// log.info(arg0);
		log.log(LogService.LOG_INFO, "Activate BackendAppX");
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
