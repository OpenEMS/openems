package io.openems.edge.goodwe.charger.twostring;

import io.openems.edge.goodwe.common.GoodWe;

/**
 * Defines the PV-Port of a GoodWe Charger Two-String.
 */
@Deprecated
public enum PvPort {

	PV_1(GoodWe.ChannelId.MPPT1_P, GoodWe.ChannelId.MPPT1_I, GoodWe.ChannelId.TWO_S_PV1_I, GoodWe.ChannelId.TWO_S_PV2_I,
			GoodWe.ChannelId.TWO_S_PV1_V),
	PV_2(GoodWe.ChannelId.MPPT1_P, GoodWe.ChannelId.MPPT1_I, GoodWe.ChannelId.TWO_S_PV2_I, GoodWe.ChannelId.TWO_S_PV1_I,
			GoodWe.ChannelId.TWO_S_PV2_V), //
	PV_3(GoodWe.ChannelId.MPPT2_P, GoodWe.ChannelId.MPPT2_I, GoodWe.ChannelId.TWO_S_PV3_I, GoodWe.ChannelId.TWO_S_PV4_I,
			GoodWe.ChannelId.TWO_S_PV3_V), //
	PV_4(GoodWe.ChannelId.MPPT2_P, GoodWe.ChannelId.MPPT2_I, GoodWe.ChannelId.TWO_S_PV4_I, GoodWe.ChannelId.TWO_S_PV5_I,
			GoodWe.ChannelId.TWO_S_PV4_V), //
	PV_5(GoodWe.ChannelId.MPPT3_P, GoodWe.ChannelId.MPPT3_I, GoodWe.ChannelId.TWO_S_PV5_I, GoodWe.ChannelId.TWO_S_PV6_I,
			GoodWe.ChannelId.TWO_S_PV5_V), //
	PV_6(GoodWe.ChannelId.MPPT3_P, GoodWe.ChannelId.MPPT3_I, GoodWe.ChannelId.TWO_S_PV6_I, GoodWe.ChannelId.TWO_S_PV5_I,
			GoodWe.ChannelId.TWO_S_PV6_V); //

	public final GoodWe.ChannelId mpptPowerChannelId;
	public final GoodWe.ChannelId mpptCurrentChannelId;
	public final GoodWe.ChannelId pvCurrentId;
	public final GoodWe.ChannelId relatedPvCurrent;
	public final GoodWe.ChannelId pvVoltageId;

	private PvPort(GoodWe.ChannelId mpptPowerChannelId, GoodWe.ChannelId mpptCurrentChannelId,
			GoodWe.ChannelId pvCurrentId, GoodWe.ChannelId relatedPvCurrent, GoodWe.ChannelId pvVoltageId) {
		this.mpptPowerChannelId = mpptPowerChannelId;
		this.mpptCurrentChannelId = mpptCurrentChannelId;
		this.pvCurrentId = pvCurrentId;
		this.relatedPvCurrent = relatedPvCurrent;
		this.pvVoltageId = pvVoltageId;
	}
}
