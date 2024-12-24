package io.openems.edge.goodwe.charger.mppt.twostring;

import io.openems.edge.goodwe.common.GoodWe;

/**
 * Defines the GoodWe MPPT with two Strings.
 */
public enum MpptPort {

	MPPT_1(GoodWe.ChannelId.MPPT1_P, GoodWe.ChannelId.MPPT1_I, GoodWe.ChannelId.TWO_S_PV1_V), //
	MPPT_2(GoodWe.ChannelId.MPPT2_P, GoodWe.ChannelId.MPPT2_I, GoodWe.ChannelId.TWO_S_PV3_V), //
	MPPT_3(GoodWe.ChannelId.MPPT3_P, GoodWe.ChannelId.MPPT3_I, GoodWe.ChannelId.TWO_S_PV5_V); //

	public final GoodWe.ChannelId mpptPowerChannelId;
	public final GoodWe.ChannelId mpptCurrentChannelId;
	public final GoodWe.ChannelId mpptVoltageChannelId;

	private MpptPort(GoodWe.ChannelId mpptPowerChannelId, GoodWe.ChannelId mpptCurrentChannelId,
			GoodWe.ChannelId mpptVoltageChannelId) {
		this.mpptPowerChannelId = mpptPowerChannelId;
		this.mpptCurrentChannelId = mpptCurrentChannelId;
		this.mpptVoltageChannelId = mpptVoltageChannelId;
	}
}
