package io.openems.impl.device.system.asymmetrictosymmetricess;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.system.SystemDevice;

@ThingInfo(title = "Asymmetric to Symmetric Ess")
public class AsymmetricToSymmetricEss extends SystemDevice {

	@ChannelInfo(title = "AsymmetricToSymmetricEss", description = "Sets the wrapper nature to use asymmetric ess as symmetric ess.", type = AsymmetricToSymmetricEssNature.class)
	public final ConfigChannel<AsymmetricToSymmetricEssNature> wrapper = new ConfigChannel<>("wrapper", this);

	public AsymmetricToSymmetricEss(Bridge parent) throws OpenemsException {
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
