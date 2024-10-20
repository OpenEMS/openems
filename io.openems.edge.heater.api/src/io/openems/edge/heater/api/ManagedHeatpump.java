package io.openems.edge.heater.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;

/**
 * Responsible nature to use when managing a Heatpump by SG Ready State.
 */
@ProviderType
public interface ManagedHeatpump extends Heatpump, ManagedHeaterByOperationMode {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * SG ready operation mode of the heatpump.
	 * 
	 * <ul>
	 * <li>Interface: ManagedHeatpump
	 * <li>Type: SgReady
	 * </ul>
	 */
	SG_READY_STATE(Doc.of(SgReady.values()) //
		.persistencePriority(PersistencePriority.HIGH) //
		.accessMode(AccessMode.READ_ONLY)), //
	;

	private final Doc doc;

	ChannelId(Doc doc) {
	    this.doc = doc;
	}

	public Doc doc() {
	    return this.doc;
	}

    }

    /**
     * Gets the Channel for {@link ChannelId#SG_READY_STATE}.
     *
     * @return the Channel
     */
    public default Channel<SgReady> getSgReadyStateChannel() {
	return this.channel(ChannelId.SG_READY_STATE);
    }

    /**
     * Gets the SgReady state of the heatpump. See {@link ChannelId#SG_READY_STATE}.
     *
     * @return the Channel {@link Value}
     */
    public default SgReady getSgReadyState() {
	return this.getSgReadyStateChannel().value().asEnum();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#SG_READY_STATE}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setSgReadyState(SgReady value) {
	this.getSgReadyStateChannel().setNextValue(value);
    }

}
