package io.openems.edge.meter.tibber.pulse.smlparser.asn1;

import java.io.PrintStream;

import java.math.BigInteger;

import org.bouncycastle.util.Arrays;

import io.openems.edge.meter.tibber.pulse.smlparser.ByteUtil;

/**
 * Parses a BER formated message into ASN.1 tokens. License: AGPLv3
 * 
 * @author mwyraz
 *
 */

// CHECKSTYLE:OFF
public class ASN1BERTokenizer {
	// CHECKSTYLE:ON

	public enum Type {
		BEGIN_OF_FILE, END_OF_FILE, END_OF_MESSAGE, UNKNOWN {
			@Override
			public String describe(byte typeValue, int dataLength, Object object) {
				return name() + "(0x" + ByteUtil.toBits(typeValue) + "?, length=" + dataLength + ")";
			}
		},
		NULL, LIST {
			@Override
			public String describe(byte typeValue, int dataLength, Object object) {
				return name() + " length=" + dataLength;
			}
		},
		OCTET_STRING {
			@Override
			public String describe(byte typeValue, int dataLength, Object object) {
				return name() + " length=" + dataLength + ": 0x" + ByteUtil.toHex((byte[]) object);
			}
		},
		UNSIGNED {
			@Override
			public String describe(byte typeValue, int dataLength, Object object) {
				return name() + "_" + (8 * dataLength) + ": " + object;
			}
		},
		SIGNED {
			@Override
			public String describe(byte typeValue, int dataLength, Object object) {
				return name() + "_" + (8 * dataLength) + ": " + object;
			}
		},;

		/**
		 * Provides a description for a given type of data based on its value, length,
		 * and an associated object. This method is a placeholder meant to be overridden
		 * by subclasses. In its base form, it simply returns the name of the enum that
		 * implements this method. The purpose is to allow for a more descriptive and
		 * context-specific representation of data types, where the type value, length
		 * of the data, and the actual object can influence the description provided.
		 * 
		 * @param typeValue  the value representing the type of data
		 * @param dataLength the length of the data in bytes
		 * @param object     the object associated with the data, which could influence
		 *                   the description
		 * @return a string describing the data type, defaulting to the name of the enum
		 *         instance calling this method
		 */
		public String describe(byte typeValue, int dataLength, Object object) {
			return name();
		}
	}

	protected final byte[] message;
	protected int offset;

	protected Type type = Type.BEGIN_OF_FILE;
	protected byte typeValue = 0;
	protected int dataLength;
	protected Object object = null;

	public ASN1BERTokenizer(byte[] message) {
		this.message = message;
		this.offset = 0;
	}

	public Type getType() {
		return this.type;
	}

	public int getDataLength() {
		return this.dataLength;
	}

	public Object getObject() {
		return this.object;
	}

	/**
	 * Decodes an unsigned number from a byte array starting at a given offset with
	 * a specified length. This method handles the conversion of bytes to an
	 * unsigned number with variable lengths up to 7 bytes. For lengths of 1 to 7
	 * bytes, it constructs the number by combining the bytes appropriately,
	 * considering the most significant bit and handling negative numbers by adding
	 * a leading zero byte for BigInteger conversion, ensuring the number is treated
	 * as positive.
	 * 
	 * @param data   the byte array containing the number to decode
	 * @param offset the position in the array from which to start decoding
	 * @param length the number of bytes to use for decoding the number
	 * @return the decoded number as a {@link Number}. For numbers up to 7 bytes, it
	 *         returns a {@link Long}. For numbers with more than 7 bytes, it
	 *         returns a {@link BigInteger} to handle the larger values.
	 * @throws IllegalArgumentException if the length is invalid or the data array
	 *                                  does not contain enough bytes to decode the
	 *                                  number from the given offset.
	 */
	public static Number decodeUnsigned(byte[] data, int offset, int length) {

		if (length == 1) {
			return data[offset] & 0xFF;
		}

		if (length == 2) {
			return (data[offset++] & 0xFF) << 8 | data[offset++] & 0xFF;
		}

		if (length == 3) {
			return (data[offset++] & 0xFF) << 16 | (data[offset++] & 0xFF) << 8 | data[offset++] & 0xFF;
		}

		if (length == 4) {
			return ((long) data[offset++] & 0xFF) << 24 | (data[offset++] & 0xFF) << 16 | (data[offset++] & 0xFF) << 8
					| data[offset++] & 0xFF;
		}

		if (length == 5) {
			return ((long) data[offset++] & 0xFF) << 32 | ((long) data[offset++] & 0xFF) << 24
					| (data[offset++] & 0xFF) << 16 | (data[offset++] & 0xFF) << 8 | data[offset++] & 0xFF;
		}

		if (length == 6) {
			return ((long) data[offset++] & 0xFF) << 40 | ((long) data[offset++] & 0xFF) << 32
					| ((long) data[offset++] & 0xFF) << 24 | (data[offset++] & 0xFF) << 16
					| (data[offset++] & 0xFF) << 8 | data[offset++] & 0xFF;
		}

		if (length == 7) {
			return ((long) data[offset++] & 0xFF) << 48 | ((long) data[offset++] & 0xFF) << 40
					| ((long) data[offset++] & 0xFF) << 32 | ((long) data[offset++] & 0xFF) << 24
					| (data[offset++] & 0xFF) << 16 | (data[offset++] & 0xFF) << 8 | data[offset++] & 0xFF;
		}

		if ((data[offset] & 0x80) != 0) { // most significant bit set -> negative number
			// add a "00" byte so that BigInteger sees a larger positive number
			byte[] bytes = new byte[length + 1];
			bytes[0] = 0;
			System.arraycopy(data, offset, bytes, 1, length);
			return new BigInteger(bytes);
		}

		return new BigInteger(data, offset, length);
	}

	/**
	 * Decodes a signed number from a byte array starting at a given offset and with
	 * a specified length.
	 *
	 * <p>
	 * This method supports decoding signed integers of various byte lengths into
	 * the appropriate Java {@link Number} type. For single-byte data, it directly
	 * returns the byte value. For data lengths up to 4 bytes, it returns an
	 * {@link Integer}; for lengths up to 8 bytes, a {@link Long} is returned. For
	 * longer data, it returns a {@link BigInteger}.
	 * </p>
	 *
	 * @param data   The byte array containing the number to decode.
	 * @param offset The position in the array where the number starts.
	 * @param length The number of bytes that make up the number.
	 * @return The decoded number, appropriately typed as a {@link Byte},
	 *         {@link Integer}, {@link Long}, or {@link BigInteger} depending on the
	 *         byte length.
	 */
	public static Number decodeSigned(byte[] data, int offset, int length) {

		if (length == 1) {
			return data[offset];
		}

		if (length > 1 && length <= 4) {
			return new BigInteger(data, offset, length).intValue();
		}

		if (length > 4 && length <= 8) {
			return new BigInteger(data, offset, length).longValue();
		}

		return new BigInteger(data, offset, length);
	}

	/**
	 * Checks if more data is available in the message stream.
	 *
	 * <p>
	 * Determines whether the current offset is less than the length of the message,
	 * indicating that additional data is available for reading.
	 * </p>
	 *
	 * @return true if more data is available, false otherwise.
	 */
	public boolean hasMoreData() {
		return this.offset < this.message.length;
	}

	/**
	 * Reads a list of elements from the message stream and validates its size.
	 *
	 * <p>
	 * Reads a list structure from the message, expecting a specific number of
	 * elements. If the actual number of elements does not match the expected size
	 * and the list is not marked as optional, an exception is thrown.
	 * </p>
	 *
	 * @param expectedSize The expected number of elements in the list.
	 * @param optional     Indicates whether the list is optional.
	 * @return The size of the list if present, or null if the list is optional and
	 *         not present.
	 */
	public Integer readListOfElements(int expectedSize, boolean optional) {
		this.readNext(Type.LIST, expectedSize, optional);
		if (this.type == Type.NULL) {
			return null;
		}
		return this.dataLength;
	}

	/**
	 * Reads the end-of-message marker from the message stream.
	 *
	 * <p>
	 * Expects an end-of-message marker at the current position in the message
	 * stream. If not present and not optional, throws an exception.
	 * </p>
	 *
	 * @param optional Indicates whether the end-of-message marker is optional.
	 */
	public void readEndOfMessage(boolean optional) {
		this.readNext(Type.END_OF_MESSAGE, 0, optional);
	}

	/**
	 * Reads an octet string from the message stream.
	 *
	 * <p>
	 * Reads and returns an octet string. The length of the string is determined
	 * dynamically from the stream.
	 * </p>
	 *
	 * @param optional Indicates whether the octet string is optional.
	 * @return The read octet string as a byte array.
	 */
	public byte[] readOctetString(boolean optional) {
		return (byte[]) this.readNext(Type.OCTET_STRING, -1, optional);
	}

	/**
	 * Reads an unsigned 8-bit integer from the message stream.
	 *
	 * <p>
	 * Reads and returns a single byte as an unsigned 8-bit integer.
	 * </p>
	 *
	 * @param optional Indicates whether the unsigned 8-bit integer is optional.
	 * @return The read unsigned 8-bit integer as an Integer.
	 */
	public Integer readUnsigned8(boolean optional) {
		return (Integer) this.readNext(Type.UNSIGNED, 1, optional);
	}

	/**
	 * Reads an unsigned 16-bit integer from the message stream.
	 *
	 * <p>
	 * Reads two bytes and returns them as an unsigned 16-bit integer.
	 * </p>
	 *
	 * @param optional Indicates whether the unsigned 16-bit integer is optional.
	 * @return The read unsigned 16-bit integer as an Integer.
	 */
	public Integer readUnsigned16(boolean optional) {
		return (Integer) this.readNext(Type.UNSIGNED, 2, optional);
	}

	/**
	 * Reads an unsigned 32-bit integer from the message stream.
	 *
	 * <p>
	 * Reads four bytes and returns them as an unsigned 32-bit integer.
	 * </p>
	 *
	 * @param optional Indicates whether the unsigned 32-bit integer is optional.
	 * @return The read unsigned 32-bit integer as a Long.
	 */
	public Long readUnsigned32(boolean optional) {
		return (Long) this.readNext(Type.UNSIGNED, 4, optional);
	}

	/**
	 * Reads a signed 8-bit integer from the message stream.
	 *
	 * <p>
	 * Reads a single byte and interprets it as a signed 8-bit integer.
	 * </p>
	 *
	 * @param optional Indicates whether the signed 8-bit integer is optional.
	 * @return The read signed 8-bit integer as a Byte.
	 */
	public Byte readSigned8(boolean optional) {
		return (Byte) this.readNext(Type.SIGNED, 1, optional);
	}

	public byte[] getMessage() {
		return this.message;
	}

	public int getOffset() {
		return this.offset;
	}

	/**
	 * Validates the currently read object against the expected type and size.
	 *
	 * <p>
	 * Ensures the last read object matches the expected ASN.1 type and, if
	 * specified, the exact size. Allows for optional elements by returning null
	 * without throwing an exception if the object is optional and a NULL type.
	 * </p>
	 *
	 * @param expectedType The expected ASN.1 {@link Type} of the object.
	 * @param expectedSize The expected size of the object in bytes, or -1 if size
	 *                     is not to be validated.
	 * @param optional     Flag indicating if the object is optional within the
	 *                     structure.
	 * @return The validated object, or null if the object is optional and a NULL
	 *         type was read.
	 * @throws RuntimeException if the read object does not match the expected
	 *                          criteria.
	 */
	public Object expect(Type expectedType, int expectedSize, boolean optional) {
		if (optional && this.type == Type.NULL) {
			return null;
		}
		if (this.type != expectedType) {
			throw new RuntimeException("Expected " + expectedType + " but found "
					+ this.type.describe(this.typeValue, this.dataLength, this.object));
		}
		if (expectedSize > -1 && expectedSize != this.dataLength) {
			throw new RuntimeException("Expected " + expectedType + " of length " + expectedSize + " but found "
					+ this.type.describe(this.typeValue, this.dataLength, this.object));
		}
		return this.object;
	}

	/**
	 * Reads and validates the next ASN.1 encoded object from the buffer against the
	 * expected type and size.
	 *
	 * <p>
	 * Combines reading and validation of the next object, ensuring it matches the
	 * predefined schema. Useful for parsing data with strict structural
	 * requirements. Throws an exception for mismatches unless the object is marked
	 * optional.
	 * </p>
	 *
	 * @param expectedType The expected ASN.1 {@link Type} of the object.
	 * @param expectedSize The expected size of the object in bytes.
	 * @param optional     Flag indicating if the object is optional.
	 * @return The validated object or null if optional and not matching.
	 * @throws ASN1ParseException if the object does not match expectations and is
	 *                            not optional.
	 */

	public Object readNext(Type expectedType, int expectedSize, boolean optional) {
		this.readNext();
		return this.expect(expectedType, expectedSize, optional);
	}

	/**
	 * Reads and decodes the next ASN.1 encoded object from the message buffer,
	 * updating internal state.
	 *
	 * <p>
	 * This method interprets the next segment of the buffer based on ASN.1 TLV
	 * encoding, adjusting the internal offset and setting the type, length, and
	 * value for various data types including lists, integers, strings, and null
	 * values. It handles end-of-message conditions and supports multi-byte length
	 * fields.
	 * </p>
	 *
	 * @return The {@link Type} of the object read, indicating its ASN.1 data type.
	 * @throws RuntimeException If attempting to read past the end-of-file marker.
	 */
	public Type readNext() {

		this.typeValue = 0;
		this.type = Type.UNKNOWN;
		this.dataLength = 0;
		this.object = null;

		if (!this.hasMoreData()) {
			if (this.type == Type.END_OF_FILE) {
				throw new RuntimeException("Read after END_OF_FILE");
			}
			this.type = Type.END_OF_FILE;
			return this.type;
		}

		byte tlField = this.message[this.offset++];
		this.typeValue = (byte) (tlField & 0b01110000);

		if (tlField == 0x0) {
			this.type = Type.END_OF_MESSAGE;
			return this.type;
		}

		int tlLength = 1;
		int tlAndDataLength = (tlField & 0b1111);
		while ((tlField & 0b10000000) != 0) {
			tlField = this.message[this.offset++];
			tlAndDataLength = (((tlAndDataLength & 0xffffffff) << 4) | (tlField & 0b00001111));
			tlLength++;
		}

		this.dataLength = tlAndDataLength - tlLength;

		switch (this.typeValue) {
		case 0b01110000:
			this.type = Type.LIST;
			this.dataLength = tlAndDataLength; // since length is not in bytes, tlLength is not substracted
			break;
		case 0b01100000:
			this.type = Type.UNSIGNED;
			this.object = decodeUnsigned(this.message, this.offset, this.dataLength);
			this.offset += this.dataLength;
			break;
		case 0b01010000:
			this.type = Type.SIGNED;
			this.object = decodeSigned(this.message, this.offset, this.dataLength);
			this.offset += this.dataLength;
			break;
		case 0b00000000:
			if (this.dataLength == 0) {
				this.type = Type.NULL;
				break;
			}
			this.type = Type.OCTET_STRING;
			// Fall-through to the default case is intentional here to handle the assignment of this.object
		//CHECKSTYLE:OFF
		default:
			// CHECKSTYLE:ON
			this.object = Arrays.copyOfRange(this.message, this.offset, this.offset + this.dataLength);
			this.offset += this.dataLength;
			break;
		}

		return this.type;
	}

	/**
	 * Dumps the current state to the specified PrintStream output, starting from
	 * the beginning until the maximum possible integer value depth. This method is
	 * a convenience wrapper that calls the full dump method with a start depth of 0
	 * and a depth limit set to {@link Integer#MAX_VALUE}, indicating no practical
	 * limit to the depth of the dump. It's useful for printing the entire state or
	 * structure to the provided PrintStream, such as System.out or System.err,
	 * without needing to specify depth parameters.
	 * 
	 * @param out the {@link PrintStream} to which the dump output will be written,
	 *            typically System.out or System.err.
	 */
	public void dump(PrintStream out) {
		this.dump(out, 0, Integer.MAX_VALUE);
	}

	protected void dump(PrintStream out, int depth, int maxElementCount) {
		while ((maxElementCount--) > 0 && this.readNext() != Type.END_OF_FILE) {

			for (int i = 0; i < depth; i++) {
				out.print("  ");
			}
			out.println(this.type.describe(this.typeValue, this.dataLength, this.object));
			if (this.type == Type.LIST) { // nested list
				this.dump(out, depth + 1, this.dataLength);
			}
		}
	}

}
