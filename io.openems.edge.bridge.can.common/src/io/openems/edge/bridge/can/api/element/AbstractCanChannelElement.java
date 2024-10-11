package io.openems.edge.bridge.can.api.element;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.can.AbstractCanBridge;
import io.openems.edge.bridge.can.api.CanIoException;
import io.openems.edge.bridge.can.api.data.CanFrame;
import io.openems.edge.bridge.can.api.data.CanFrameImpl;
import io.openems.edge.common.type.TypeUtils;

/**
 * Represents a channel object and holding a reference to the appropriate
 * {@link CanFrame}.
 *
 * @param <T> the target OpenemsType
 */
public abstract class AbstractCanChannelElement<E, C> implements CanChannelElement<C> {

	protected final Logger log = LoggerFactory.getLogger(AbstractCanChannelElement.class);

	protected final List<Consumer<Optional<C>>> onSetNextWriteCallbacks = new ArrayList<>();
	private final List<Consumer<C>> onUpdateCallbacks = new CopyOnWriteArrayList<>();
	private final OpenemsType type;
	protected int bitposition;
	protected int bitlen;
	protected int offset;
	protected int min;
	protected int max;

	protected CanFrame canFrame;

	protected boolean invalid;

	public AbstractCanChannelElement(OpenemsType type) {
		this(type, 0, 0, 0, 0, 0);
	}

	/**
	 * Objects describes the behaviour and the position of an unsigned word value
	 * within an 8 byte CAN frame.
	 *
	 * @param type     the {@link OpenemsType}
	 * @param position 0-63 the bit position within the 8 bytes of a CAN frame
	 * @param len      0-15 the number which belongs to the value
	 * @param offset   value to add to the received unsigned word
	 * @param min      min possible value
	 * @param max      max possible value
	 */
	public AbstractCanChannelElement(OpenemsType type, int position, int len, int offset, int min, int max) {
		this.type = type;
		this.bitposition = position;
		this.bitlen = len;
		this.offset = offset;
		this.min = min;
		this.max = max;
		this.invalid = true;
	}

	/**
	 * Gets itself.
	 *
	 * @return itself
	 */
	public abstract E self();

	@Override
	public boolean isInvalid() {
		return this.invalid;
	}

	@Override
	public OpenemsType getType() {
		return this.type;
	}

	public void setCanFrame(CanFrameImpl canFrameImpl) {
		this.canFrame = canFrameImpl;
	}

	public CanFrame getCanFrame() {
		return this.canFrame;
	}

	@Override
	public void deactivate() {
		this.onUpdateCallbacks.clear();
	}

	@Override
	public final void onSetNextWrite(Consumer<Optional<C>> callback) {
		this.onSetNextWriteCallbacks.add(callback);
	}

	/**
	 * The onUpdateCallback is called on reception of a new value.
	 *
	 * <p>
	 * Be aware, that this is the original, untouched value.
	 * ChannelToElementConverters are not applied here yet!
	 *
	 * @param onUpdateCallback the Callback
	 * @return myself
	 */
	public AbstractCanChannelElement<E, C> onUpdateCallback(Consumer<C> onUpdateCallback) {
		this.onUpdateCallbacks.add(onUpdateCallback);
		return this;
	}

	protected void setValue(C value) {
		if (this.isDebug) {
			this.log.info("Element [" + this + "] set value to [" + value + "].");
		}
		if (value != null) {
			this.invalid = false;
		} else {
			this.invalid = true;
		}
		for (Consumer<C> callback : this.onUpdateCallbacks) {
			callback.accept(value);
		}
	}

	/**
	 * Sets a value that should be written to the CAN device. <br/>
	 * <br/>
	 *
	 * <p>
	 * NOTE: method is called, when someone writes a value to this channel
	 *
	 * @param valueOpt the Optional value
	 * @throws OpenemsException on error
	 */
	public void setNextWriteValue(Optional<?> valueOpt) throws OpenemsException {
		if (valueOpt.isPresent()) {
			this._setNextWriteValue(Optional.of(TypeUtils.getAsType(this.getType(), valueOpt.get())));
		} else {
			this._setNextWriteValue(Optional.empty());
		}
	}

	@Override
	public boolean invalidate(AbstractCanBridge bridge) {
		this.setValue(null);
		return true;
	}

	@Override
	public void onCanFrameSuccessfullySend() throws OpenemsException {
		; // NOTE: unused here
	}

	/**
	 * Updates the Channel value when a CAN frame is received.
	 *
	 * @param data the reveived data
	 */
	public abstract void doElementSetInput(byte[] data) throws CanIoException;

	protected Integer fromSignedByteBuffer16(ByteBuffer buff) {
		return (int) buff.getShort(0);
	}

	protected Integer fromByteBuffer16(ByteBuffer buff) {
		return Short.toUnsignedInt(buff.getShort(0));
	}

	protected ByteBuffer toByteBuffer16(ByteBuffer buff, Integer value) {
		return buff.putShort(value.shortValue());
	}

	protected Integer fromByteBuffer32(ByteBuffer buff) {
		return buff.getInt(0);
	}

	protected ByteBuffer toByteBuffer32(ByteBuffer buff, Integer value) {
		return buff.putInt(value.intValue());
	}

	protected byte getBitmask(int bitidx) {
		// relevant bits go from bitidx to (bitidx+bitlen), create a special mask
		byte bitpattern = 0;
		if (this.bitlen == 4) {
			bitpattern = 0x0f;
		} else {
			for (var i = 0; i < this.bitlen; i++) {
				bitpattern |= 1 << i;
			}
		}
		return (byte) (bitpattern << bitidx);
	}

	/*
	 * Enable Debug mode for this Element. Activates verbose logging.
	 */
	private boolean isDebug = false;

	/**
	 * Activates the debug mode for the element.
	 *
	 * @return itself
	 */
	public AbstractCanChannelElement<E, C> debug() {
		this.isDebug = true;
		return this;
	}

	protected boolean isDebug() {
		return this.isDebug;
	}

	/**
	 * Gets the name of the element.
	 *
	 * @return the name
	 */
	public abstract String getName();

	@Override
	public String toString() {
		var byteidx = this.bitposition / 8;
		var bitidx = this.bitposition % 8;

		return this.getName() + " masked at byte " + byteidx + " bit " + bitidx + " for " + this.bitlen + " bits in "
				+ this.canFrame;
	}

	/**
	 * Asks if the element has an own template format.
	 *
	 * @return true, if it has
	 */
	public boolean hasOwnCanTemplateFormat() {
		return false;
	}

	/**
	 * Gets the template data.
	 *
	 * @return the template data
	 * @throws OpenemsException on error
	 */
	public byte[] getOwnCanTemplateData() throws OpenemsException {
		return null;
	}

	@Override
	public boolean sendChunkOfFrames() {
		return false;
	}

}
