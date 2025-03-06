package io.openems.edge.bridge.can.api.element;

import java.util.Optional;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.can.AbstractCanBridge;

/**
 * Represents a element data object, which <br/>
 * 1. is directly associated to a openems Channel Object (read,write)<br/>
 * 2. is directly associated to a very specific particle within the CAN frame
 * (read,write)
 */
public interface CanChannelElement<T> {

	/**
	 * Asks if the element is invalid.
	 *
	 * @return true, if this element is invalid
	 */
	public boolean isInvalid();

	/**
	 * Gets the type of this Register, e.g. INTEGER, BOOLEAN,..
	 *
	 * @return the OpenemsType
	 */
	public OpenemsType getType();

	/**
	 * Updates the appropriate bits within the 8 data bytes of the connected CAN
	 * frame with the given channel value.
	 *
	 * <p>
	 * NOTE: method is called, when someone writes a value to this channel
	 *
	 * @param valueOpt the channel value
	 * @throws OpenemsException on error
	 */
	public abstract void _setNextWriteValue(Optional<T> valueOpt) throws OpenemsException;

	/**
	 * Add an onSetNextWrite callback. It is called when a 'next write value' was
	 * set.
	 *
	 * @param callback the callback
	 */
	public void onSetNextWrite(Consumer<Optional<T>> callback);

	/**
	 * Invalidates the Channel in case it could not be read from the Can device,
	 * i.e. sets the value to 'UNDEFINED'/null. Applies the
	 * 'invalidateElementsAfterReadErrors' config setting of the bridge.
	 *
	 * @param bridge the {@link AbstractCanBridge}
	 * @return true if Channel was invalidated
	 */
	public boolean invalidate(AbstractCanBridge bridge);

	/**
	 * This is called on deactivate of the CAN-Bridge. It can be used to clear any
	 * references like listeners.
	 */
	public void deactivate();

	/**
	 * Called everytime a CAN frame was successfully send via low level CAN device.
	 *
	 * @throws OpenemsException on error
	 */
	public void onCanFrameSuccessfullySend() throws OpenemsException;

	/**
	 * Asks if a new frame is sent directly after calling
	 * onCanFrameSuccessfullySend().
	 *
	 * @return true, if a frame is sent directly after calling
	 *         onCanFrameSuccessfullySend() and false if sending has finished
	 */
	public boolean sendChunkOfFrames();

}
