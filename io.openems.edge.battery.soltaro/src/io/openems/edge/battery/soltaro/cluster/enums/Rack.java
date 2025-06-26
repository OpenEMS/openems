package io.openems.edge.battery.soltaro.cluster.enums;

import io.openems.edge.battery.soltaro.cluster.SoltaroCluster;
import io.openems.edge.battery.soltaro.cluster.versionb.BatterySoltaroClusterVersionB;

/**
 * Helper enum to wrap information about racks.
 */
public enum Rack {
	RACK_1(1, 0x2000, SoltaroCluster.ChannelId.RACK_1_USAGE,
			BatterySoltaroClusterVersionB.ChannelId.RACK_1_POSITIVE_CONTACTOR,
			SoltaroCluster.ChannelId.SUB_MASTER_1_COMMUNICATION_FAILURE), //
	RACK_2(2, 0x3000, SoltaroCluster.ChannelId.RACK_2_USAGE,
			BatterySoltaroClusterVersionB.ChannelId.RACK_2_POSITIVE_CONTACTOR,
			SoltaroCluster.ChannelId.SUB_MASTER_2_COMMUNICATION_FAILURE), //
	RACK_3(3, 0x4000, SoltaroCluster.ChannelId.RACK_3_USAGE,
			BatterySoltaroClusterVersionB.ChannelId.RACK_3_POSITIVE_CONTACTOR,
			SoltaroCluster.ChannelId.SUB_MASTER_3_COMMUNICATION_FAILURE), //
	RACK_4(4, 0x5000, SoltaroCluster.ChannelId.RACK_4_USAGE,
			BatterySoltaroClusterVersionB.ChannelId.RACK_4_POSITIVE_CONTACTOR,
			SoltaroCluster.ChannelId.SUB_MASTER_4_COMMUNICATION_FAILURE), //
	RACK_5(5, 0x6000, SoltaroCluster.ChannelId.RACK_5_USAGE,
			BatterySoltaroClusterVersionB.ChannelId.RACK_5_POSITIVE_CONTACTOR,
			SoltaroCluster.ChannelId.SUB_MASTER_5_COMMUNICATION_FAILURE);

	/**
	 * Get the {@link Rack} for the given ID.
	 *
	 * @param rackId from 1 to 5
	 * @return the {@link Rack}
	 */
	public static Rack getRack(int rackId) {
		for (Rack rack : Rack.values()) {
			if (rackId == rack.id) {
				return rack;
			}
		}
		throw new IllegalArgumentException("Rack with ID [" + rackId + "] is not available!");
	}

	public final int id;
	public final int offset;
	public final SoltaroCluster.ChannelId usageChannelId;
	// NOTE: this is only used with Version B
	public final BatterySoltaroClusterVersionB.ChannelId positiveContactorChannelId;
	public final SoltaroCluster.ChannelId subMasterCommunicationAlarmChannelId;

	private Rack(//
			int id, //
			int addressOffset, //
			SoltaroCluster.ChannelId usageChannelId, //
			BatterySoltaroClusterVersionB.ChannelId positiveContactorChannelId, //
			SoltaroCluster.ChannelId subMasterCommunicationAlarmChannelId //
	) {
		this.id = id;
		this.offset = addressOffset;
		this.usageChannelId = usageChannelId;
		this.subMasterCommunicationAlarmChannelId = subMasterCommunicationAlarmChannelId;
		this.positiveContactorChannelId = positiveContactorChannelId;
	}

	/**
	 * Gets the Channel-ID Prefix for this Rack in the form "RACK_X_".
	 *
	 * @return the prefix
	 */
	public String getChannelIdPrefix() {
		return "RACK_" + this.id + "_";
	}

	/**
	 * Gets the Channel-Doc Text Prefix for this Rack in the form "Rack X ".
	 *
	 * @return the prefix
	 */
	public String getChannelDocTextPrefix() {
		return "Rack " + this.id + " ";
	}

}