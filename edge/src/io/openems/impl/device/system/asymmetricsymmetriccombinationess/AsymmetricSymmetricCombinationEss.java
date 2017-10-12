package io.openems.impl.device.system.asymmetricsymmetriccombinationess;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.system.SystemDevice;

@ThingInfo(title = "Ess Asymmetric-Symmetric-Combination")
public class AsymmetricSymmetricCombinationEss extends SystemDevice {

	@ChannelInfo(title = "AsymmetricSymmetricCombinationEss", description = "Sets the wrapper nature to use asymmetric and symmetric controller together.", type = AsymmetricSymmetricCombinationEssNature.class)
	public final ConfigChannel<AsymmetricSymmetricCombinationEssNature> wrapper = new ConfigChannel<>("wrapper", this);

	public AsymmetricSymmetricCombinationEss(Bridge parent) throws OpenemsException {
		super(parent);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (wrapper.valueOptional().isPresent()) {
			natures.add(wrapper.valueOptional().get());
		}
		return natures;
	}

}
