package io.openems.edge.goodwe.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import io.openems.edge.common.update.ProgressPublisher;

public class UpdateHandler implements AutoCloseable {

	private static final int DEFAULT_TIMEOUT = 3;
	private static final int DEFAULT_INITIAL_BYTES = 32;
	private static final int DEFAULT_BLOCK_BYTES = 512;

	private final SerialPortHandler serialPortHandler;

	public UpdateHandler(String portName, int baudRate) throws Exception {
		this.serialPortHandler = new SerialPortHandler(portName, baudRate);
	}

	@Override
	public void close() {
		this.serialPortHandler.close();
	}

	/**
	 * Closes the serial port.
	 */
	public void closePort() {
		this.serialPortHandler.closeSerialPort();
	}

	/**
	 * Opens the serial port.
	 */
	public void openPort() {
		this.serialPortHandler.openSerialPort();
	}

	/**
	 * Run the update for ARM firmware.
	 *
	 * @param progress the progress
	 * @param fileName File name
	 * @throws Exception on error
	 */
	public void updateArmVersion(ProgressPublisher progress, String fileName) throws Exception {
		this.updateVersion(progress, GoodWeFirmwareVersion.ARM, fileName);
	}

	/**
	 * Run the update for DSP firmware.
	 *
	 * @param progress the progress
	 * @param fileName File name
	 * @throws Exception on error
	 */
	public void updateDspVersion(ProgressPublisher progress, String fileName) throws Exception {
		this.updateVersion(progress, GoodWeFirmwareVersion.DSP, fileName);
	}

	private void updateVersion(ProgressPublisher progress, GoodWeFirmwareVersion fw, String fileName) throws Exception {
		progress.setPercentage(0, String.format("Start updating %s version", fw));
		progress.setPercentage(0, String.format("reading %s file", fw));
		final var bytes = readBinaryFile(fileName);
		progress.setPercentage(5, String.format("finished reading %s file", fw));

		// Slice the first 32 bytes for the initial part
		final var initialPart = Arrays.copyOfRange(bytes, 0, DEFAULT_INITIAL_BYTES);
		final var firmwarePart = Arrays.copyOfRange(bytes, DEFAULT_INITIAL_BYTES, bytes.length);

		progress.setPercentage(5, "start sending initial part");
		this.sendInitialPartWithHeader(fw, initialPart);
		progress.setPercentage(15, "finished sending initial part");

		progress.setPercentage(15, "start sending firmware parts");
		this.sendFirmwarePart(progress.subProgress(15, 99), fw, firmwarePart);
		progress.setPercentage(99, "finished sending firmware parts");

		progress.setPercentage(100, String.format("finished updating %s version", fw));
	}

	private void sendInitialPartWithHeader(GoodWeFirmwareVersion fw, byte[] initialBytes) throws Exception {

		byte[] headerBytes = {
				// HEADER
				(byte) 0xAA, 0x55, (byte) 0x80, 0x7f, 0x02, 0x29,

				// TYPE
				fw.hexCode[0], fw.hexCode[1], //

				// INDEX
				0x00, 0x00, //

				// DATA LENGTH
				0x00, 0x20 // 32 for the initial data
		};

		var dataPackage = joinByteArrays(headerBytes, initialBytes);

		// Calculate and add checksum
		dataPackage = joinByteArrays(dataPackage, calculateChecksum(dataPackage));

		// Send initial package
		this.serialPortHandler.sendBytes(fw.text, dataPackage, DEFAULT_TIMEOUT, true);
	}

	private void sendFirmwarePart(ProgressPublisher progress, GoodWeFirmwareVersion fw, byte[] firmwarePart)
			throws Exception {
		final var fwLength = firmwarePart.length;
		final var blockLength = DEFAULT_BLOCK_BYTES;

		for (final var packageIndex : progress.fori(0, (int) Math.ceil((((double) fwLength) / blockLength)), 1,
				"sending firmware part")) {
			final var i = packageIndex * blockLength;

			// End index of this block
			var endIndex = Math.min((i + blockLength), fwLength);

			// Current block
			var firmwareBlock = Arrays.copyOfRange(firmwarePart, i, endIndex);

			var dataPackage = this.getDataPackageWithHeader(fw, packageIndex, firmwareBlock);

			// Send data package
			this.serialPortHandler.sendBytes(fw.text, dataPackage, DEFAULT_TIMEOUT, true);
		}
	}

	private byte[] getDataPackageWithHeader(GoodWeFirmwareVersion fw, int packageIndex, byte[] firmwareBlock) {

		var dataLength = toHexBytes(firmwareBlock.length); // default 512, last package normally lower
		var hexIndex = toHexBytes(packageIndex + 1);

		// Build byte block (new format)
		byte[] headerBytes = {
				// HEADER
				(byte) 0xAA, 0x55, (byte) 0x80, 0x7f, 0x02, 0x29,

				// TYPE
				fw.hexCode[0], fw.hexCode[1],

				// INDEX
				hexIndex[0], hexIndex[1],

				// DATA LENGTH
				dataLength[0], dataLength[1] };

		/*
		 * Calculate checksum
		 */
		var dataPackage = joinByteArrays(headerBytes, firmwareBlock);

		// Calculate and add checksum
		return joinByteArrays(dataPackage, calculateChecksum(dataPackage));
	}

	/**
	 * Read binary update file.
	 *
	 * @param fileName filename
	 * @return bytes
	 * @throws Exception on read error
	 */
	private static byte[] readBinaryFile(String fileName) throws Exception {
		try {
			return Files.readAllBytes(Paths.get(fileName));
		} catch (IOException e) {
			throw new Exception("Not able to read from file: " + fileName + ".", e);
		}
	}

	/**
	 * Returns the given byte[] as hex encoded string.
	 *
	 * @param data a byte[] array.
	 * @return a hex encoded String.
	 */
	public static String toHex(byte[] data) {
		if (data == null) {
			return null;
		}

		// The offset to start converting from
		final var off = 0;

		// Length the number of bytes to be converted
		final var length = data.length;

		// double size, two bytes (hex range) for one byte
		final var buf = new StringBuilder(data.length * 2);
		for (int i = off; i < length; i++) {
			// don't forget the second hex digit
			if ((data[i] & 0xff) < 0x10) {
				buf.append("0");
			}
			buf.append(Long.toString(data[i] & 0xff, 16));
			if (i < data.length - 1) {
				buf.append(" ");
			}
		}
		return buf.toString().toUpperCase();
	}

	private static byte[] joinByteArrays(byte[] array1, byte[] array2) {
		byte[] result = new byte[array1.length + array2.length];
		System.arraycopy(array1, 0, result, 0, array1.length);
		System.arraycopy(array2, 0, result, array1.length, array2.length);
		return result;
	}

	private static byte[] calculateChecksum(byte[] bytes) {
		int sum = 0;

		for (byte b : bytes) {
			// Convert the byte to an integer before adding it to the sum
			sum += b & 0xFF; // Use bitwise AND to convert to an unsigned integer
		}

		return toHexBytes(sum);
	}

	private static byte[] toHexBytes(int value) {
		// Convert the integer value to a byte array
		byte[] byteArray = new byte[2];
		byteArray[0] = (byte) ((value >> 8) & 0xFF); // Most significant byte
		byteArray[1] = (byte) (value & 0xFF); // Least significant byte

		return byteArray;
	}

}
