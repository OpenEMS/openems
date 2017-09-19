package io.openems.impl.device.system.metercluster;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.system.SystemDevice;

@ThingInfo(title = "Meter Cluster")
public class MeterCluster extends SystemDevice {

	@ChannelInfo(title = "EssCluster", description = "Sets the cluster nature.", type = MeterClusterNature.class)
	public final ConfigChannel<MeterClusterNature> cluster = new ConfigChannel<>("cluster", this);

	public MeterCluster(Bridge parent) throws OpenemsException {
		super(parent);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (cluster.valueOptional().isPresent()) {
			natures.add(cluster.valueOptional().get());
		}
		return natures;
	}

}
