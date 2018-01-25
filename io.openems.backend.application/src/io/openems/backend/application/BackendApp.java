package io.openems.backend.application;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component()
public class BackendApp {

	@Activate
	void activate() {
		System.out.println("Activate");
	}

	@Deactivate
	void deactivate() {
		System.out.println("Deactivate");
	}

}
