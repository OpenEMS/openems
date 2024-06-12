package io.openems.edge.heater.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;

/**
 * Responsible nature to use when managing a Heater device by its operation
 * mode.
 * 
 * @see OperationModeRequest OperationModeRequest for more info.
 */
@ProviderType
public interface ManagedHeaterByOperationMode extends Heater {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * Set operation mode of heater.
	 * <ul>
	 * <li>Interface: Heater
	 * <li>Type: OperationModeRequest
	 * </ul>
	 */
	OPERATION_MODE_REQUEST(Doc.of(OperationModeRequest.values()) //
		.persistencePriority(PersistencePriority.HIGH) //
		.accessMode(AccessMode.READ_WRITE)), //
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
     * Gets the Channel for {@link ChannelId#OPERATION_MODE_REQUEST}.
     *
     * @return the Channel
     */

    public default WriteChannel<OperationModeRequest> getOperationModeRequestChannel() {
	return this.channel(ChannelId.OPERATION_MODE_REQUEST);
    }

    /**
     * Gets the value of the Channel for {@link ChannelId#OPERATION_MODE_REQUEST}.
     *
     * @return the Operation mode request
     */
    public default OperationModeRequest getOperationModeRequest() {
	return this.getOperationModeRequestChannel().value().asEnum();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#OPERATION_MODE_REQUEST} Channel.
     * 
     * @param value the next value
     */
    public default void _setOperationModeRequest(OperationModeRequest value) {
	this.getOperationModeRequestChannel().setNextValue(value);
    }

    /**
     * Sets the next write value on {@link ChannelId#OPERATION_MODE_REQUEST}
     * Channel.
     * 
     * @param value the write value
     * @throws OpenemsNamedException on error
     */
    public default void setOperationModeRequest(OperationModeRequest value) throws OpenemsNamedException {
	this.getOperationModeRequestChannel().setNextWriteValue(value);
    }

}
