package io.openems.edge.ess.generic.common;

import static io.openems.edge.ess.generic.common.RuntimeChannels.ChannelId.CUMULATED_TIME_FAULT_STATE;
import static io.openems.edge.ess.generic.common.RuntimeChannels.ChannelId.CUMULATED_TIME_INFO_STATE;
import static io.openems.edge.ess.generic.common.RuntimeChannels.ChannelId.CUMULATED_TIME_OK_STATE;
import static io.openems.edge.ess.generic.common.RuntimeChannels.ChannelId.CUMULATED_TIME_WARNING_STATE;

import io.openems.common.channel.Level;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

/**
 * Class which provides logic for {@link RuntimeChannels} interface. Adds a
 * {@link CalculateActiveTime} variable for each {@link Level} of the component.
 */
public class RuntimeChannelsProvider {

	private final CalculateActiveTime calculateOkStateTime;
	private final CalculateActiveTime calculateInfoStateTime;
	private final CalculateActiveTime calculateWarningStateTime;
	private final CalculateActiveTime calculateFaultStateTime;

	public RuntimeChannelsProvider(TimedataProvider timedataProvider) {
		this.calculateOkStateTime = new CalculateActiveTime(timedataProvider, CUMULATED_TIME_OK_STATE);
		this.calculateInfoStateTime = new CalculateActiveTime(timedataProvider, CUMULATED_TIME_INFO_STATE);
		this.calculateWarningStateTime = new CalculateActiveTime(timedataProvider, CUMULATED_TIME_WARNING_STATE);
		this.calculateFaultStateTime = new CalculateActiveTime(timedataProvider, CUMULATED_TIME_FAULT_STATE);
	}

	/**
	 * Accumulates time spent in a certain component {@link Level}.
	 * 
	 * @param state the current state of the component.
	 */
	public void updateStateTime(Level state) {
		this.calculateOkStateTime.update(state == Level.OK);
		this.calculateInfoStateTime.update(state == Level.INFO);
		this.calculateWarningStateTime.update(state == Level.WARNING);
		this.calculateFaultStateTime.update(state == Level.FAULT);
	}
}
