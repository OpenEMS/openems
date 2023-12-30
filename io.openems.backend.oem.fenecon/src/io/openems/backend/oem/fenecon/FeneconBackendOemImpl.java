package io.openems.backend.oem.fenecon;

import org.osgi.service.component.annotations.Component;

import io.openems.common.oem.OpenemsBackendOem;

@Component
public class FeneconBackendOemImpl implements OpenemsBackendOem {

	@Override
	public String getInfluxdbTag() {
		return "fems";
	}

}
