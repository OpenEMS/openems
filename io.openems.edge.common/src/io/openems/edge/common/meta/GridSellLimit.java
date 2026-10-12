package io.openems.edge.common.meta;

public record GridSellLimit(//
		Hard hard) {

	public record Hard(//
			/** Power in [W] */
			int power, //
			/** Power in [W] with safety buffer */
			int powerWithBuffer) {
	}
}
