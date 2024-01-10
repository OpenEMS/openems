package io.openems.backend.oem.fenecon;

import org.osgi.service.component.annotations.Component;

import io.openems.common.oem.OpenemsBackendOem;

@Component
public class FeneconBackendOemImpl implements OpenemsBackendOem {

	@Override
	public String getAppCenterMasterKey() {
		return "8fyk-Gma9-EUO3-j3gi";
	}

	@Override
	public String getInfluxdbTag() {
		return "fems";
	}

}
