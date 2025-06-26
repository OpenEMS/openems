package io.openems.oem.openems;

import org.osgi.service.component.annotations.Component;

import io.openems.common.oem.DummyOpenemsBackendOem;
import io.openems.common.oem.OpenemsBackendOem;

@Component
public class OpenemsBackendOemImpl extends DummyOpenemsBackendOem implements OpenemsBackendOem {

}
