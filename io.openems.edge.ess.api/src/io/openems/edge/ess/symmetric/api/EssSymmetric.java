package io.openems.edge.ess.symmetric.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.ess.power.symmetric.SymmetricPower;
import io.openems.edge.ess.symmetric.readonly.api.EssSymmetricReadonly;

@ProviderType
public interface EssSymmetric extends EssSymmetricReadonly {

	public SymmetricPower getPower();
}
