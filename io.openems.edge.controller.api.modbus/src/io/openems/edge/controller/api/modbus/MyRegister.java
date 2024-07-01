package io.openems.edge.controller.api.modbus;

import java.util.function.Consumer;

import com.ghgande.j2mod.modbus.procimg.Register;

/**
 * Largely copied from
 * {@link com.ghgande.j2mod.modbus.procimg.SynchronizedAbstractRegister}.
 */
public class MyRegister implements Register {

	private final int index;
	private final Consumer<MyRegister> onSetCallback;
	private byte byte1;
	private byte byte2;

	public MyRegister(int index, byte byte1, byte byte2, Consumer<MyRegister> callback) {
		this.index = index;
		this.byte1 = byte1;
		this.byte2 = byte2;
		this.onSetCallback = callback;
	}

	protected int getIndex() {
		return this.index;
	}

	protected byte getByte1() {
		return this.byte1;
	}

	protected byte getByte2() {
		return this.byte2;
	}

	@Override
	public int getValue() {
		return (this.byte1 & 0xff) << 8 | this.byte2 & 0xff;
	}

	@Override
	public int toUnsignedShort() {
		return this.getValue();
	}

	@Override
	public short toShort() {
		return (short) this.getValue();
	}

	@Override
	public byte[] toBytes() {
		return new byte[] { this.byte1, this.byte2 };
	}

	protected void setValue(byte byte1, byte byte2) {
		this.byte1 = byte1;
		this.byte2 = byte2;
		this.onSetCallback.accept(this);
	}

	@Override
	public void setValue(int v) {
		this.setValue((short) v);
	}

	@Override
	public final synchronized void setValue(short s) {
		this.setValue((byte) (0xff & s >> 8), (byte) (0xff & s));
	}

	@Override
	public void setValue(byte[] bytes) {
		this.setValue(bytes[0], bytes[1]);
	}

}
