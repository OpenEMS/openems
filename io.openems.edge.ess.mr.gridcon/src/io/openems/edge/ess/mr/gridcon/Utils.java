package io.openems.edge.ess.mr.gridcon;

import io.openems.edge.ess.mr.gridcon.enums.GridConChannelId;
import io.openems.edge.ess.mr.gridcon.enums.PCSControlWordBitPosition;

public class Utils {
	/**
	 * Checks if bit at requested position is set (true/false) and writes that
	 * boolean value to the target Channel-ID.
	 * 
	 * @param ctrlWord    the source integer value
	 * @param bitPosition the bit position
	 * @param component   the GridconPCS component
	 * @param id          the Channel-ID
	 */
	@Deprecated
	public static void mapBitToChannel(long ctrlWord, PCSControlWordBitPosition bitPosition, GridconPCS component,
			GridConChannelId id) {
		boolean val = ((ctrlWord >> bitPosition.getBitPosition()) & 1) == 1;
		component.channel(id).setNextValue(val);
	}
}
