package io.openems.edge.controller.ess.syncactivepower;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerSyncActivePower extends Controller, OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	DEBUG_SET_ACTIVE_POWER_BEFORE_PID(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH) //
		.accessMode(AccessMode.READ_ONLY));

	private final Doc doc;

	private ChannelId(Doc doc) {
	    this.doc = doc;
	}

	@Override
	public Doc doc() {
	    return this.doc;
	}
    }

    public default IntegerReadChannel getDebugSetActivePowerBeforePidChannel() {
	return this.channel(ChannelId.DEBUG_SET_ACTIVE_POWER_BEFORE_PID);
    }

    public default Value<Integer> getDebugSetActivePowerBeforePid() {
	return this.getDebugSetActivePowerBeforePidChannel().value();
    }

    /**
     * set debugSetActivePowerBeforePid.
     * @param value the value to set
     */
    public default void _setDebugSetActivePowerBeforePid(Integer value) {
	this.getDebugSetActivePowerBeforePidChannel().setNextValue(value);
    }

    /**
     * set debugSetActivePowerBeforePid.
     * @param value the value to set
     */
    public default void _setDebugSetActivePowerBeforePid(int value) {
	this.getDebugSetActivePowerBeforePidChannel().setNextValue(value);
    }

}
