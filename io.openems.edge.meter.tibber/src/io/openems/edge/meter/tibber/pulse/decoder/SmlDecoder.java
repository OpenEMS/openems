package io.openems.edge.meter.tibber.pulse.decoder;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Hex;
import org.openmuc.jsml.structures.OctetString;
import org.openmuc.jsml.transport.MessageExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.meter.tibber.pulse.decoder.SmlMeterData.Reading;
import io.openems.edge.meter.tibber.pulse.smlparser.AbstractSmlObject;
import io.openems.edge.meter.tibber.pulse.smlparser.ByteUtil;
import io.openems.edge.meter.tibber.pulse.smlparser.SmlGetListResponse;
import io.openems.edge.meter.tibber.pulse.smlparser.SmlListEntry;
import io.openems.edge.meter.tibber.pulse.smlparser.SmlMessage;
import io.openems.edge.meter.tibber.pulse.smlparser.SmlMessageParser;
import io.openems.edge.meter.tibber.pulse.smlparser.SmlPublicCloseResponse;
import io.openems.edge.meter.tibber.pulse.smlparser.SmlPublicOpenResponse;
import io.openems.edge.meter.tibber.pulse.smlparser.asn1.ASN1BERTokenizer;

/**
 * SML Spec:
 * https://www.bsi.bund.de/SharedDocs/Downloads/DE/BSI/Publikationen/TechnischeRichtlinien/TR03109/TR-03109-1_Anlage_Feinspezifikation_Drahtgebundene_LMN-Schnittstelle_Teilb.pdf?__blob=publicationFile&v=1.
 * 
 * @author mwyraz
 *
 */
public class SmlDecoder {

	protected static final Logger log = LoggerFactory.getLogger(SmlDecoder.class);

	protected static boolean DUMP_RAW_SML = false;

	/**
	 * Decodes SML data from a byte array with default settings. Assumes the SML
	 * frame is present and does not tolerate corrupt message parts.
	 *
	 * @param smlPayload the byte array containing the SML data.
	 * @return a {@link SmlMeterData} object representing the decoded data.
	 * @throws IOException if an I/O error occurs or the data is corrupted.
	 */
	public static SmlMeterData decode(byte[] smlPayload) throws IOException {
		return decode(smlPayload, true);
	}

	/**
	 * Decodes SML data from a byte array with an option to fail on corrupt message
	 * parts. Assumes the SML frame is present.
	 *
	 * @param smlPayload               the byte array containing the SML data.
	 * @param failOnCorruptMessagePart indicates whether to throw an exception on
	 *                                 corrupt data parts.
	 * @return a {@link SmlMeterData} object representing the decoded data.
	 * @throws IOException if an I/O error occurs or the data is corrupted and
	 *                     {@code failOnCorruptMessagePart} is true.
	 */
	public static SmlMeterData decode(byte[] smlPayload, boolean failOnCorruptMessagePart) throws IOException {
		return decode(smlPayload, true, failOnCorruptMessagePart);
	}

	/**
	 * Decodes SML data from a byte array, specifying whether an SML frame is
	 * present and whether to fail on corrupt message parts.
	 *
	 * @param smlPayload               the byte array containing the SML data.
	 * @param hasSmlFrame              indicates whether the data includes an SML
	 *                                 frame.
	 * @param failOnCorruptMessagePart indicates whether to throw an exception on
	 *                                 corrupt data parts.
	 * @return a {@link SmlMeterData} object representing the decoded data.
	 * @throws IOException if an I/O error occurs, the data is corrupted and
	 *                     {@code failOnCorruptMessagePart} is true, or if the SML
	 *                     frame is expected but not found.
	 */
	public static SmlMeterData decode(byte[] smlPayload, boolean hasSmlFrame, boolean failOnCorruptMessagePart)
			throws IOException {

		if (log.isDebugEnabled()) {
			log.debug("Parsing SML: {}", Hex.encodeHexString(smlPayload));
		}

		byte[] messagePayload = hasSmlFrame ? extractMessage(smlPayload) : smlPayload;

		if (DUMP_RAW_SML) {
			System.err.println(ByteUtil.toHex(messagePayload));

			System.err.println("--- Dump start");
			new ASN1BERTokenizer(messagePayload).dump(System.err);
			System.err.println("--- Dump end");
			System.err.println();
		}

		SmlMeterData result = new SmlMeterData();

		for (SmlMessage sml : SmlMessageParser.parse(messagePayload)) {
			decodeSmlObject(result, sml.getMessageBody());
		}

		return result;
	}

	protected static void decodeSmlObject(SmlMeterData result, AbstractSmlObject sml) {

		if (sml == null) {
			return; // may happen on incomplete SML message
		}

		if (sml instanceof SmlPublicCloseResponse) {
			// no usable data
			return;
		}

		if (sml instanceof SmlPublicOpenResponse) {
			if (result.meterId == null) {
				result.meterId = decodeMeterId(((SmlPublicOpenResponse) sml).getServerId());
			}
			return;
		}

		if (sml instanceof SmlGetListResponse) {
			SmlGetListResponse res = (SmlGetListResponse) sml;

			if (result.meterId == null) {
				result.meterId = decodeMeterId(res.getServerId());
			}
			// my meter has "ActSensorTime" as index of seconds - is this somehow useful?
			if (res.getValList() != null) {
				for (SmlListEntry e : res.getValList()) {
					decodeSmlObject(result, e);
				}
			}
			return;
		}

		if (sml instanceof SmlListEntry) {
			SmlListEntry e = (SmlListEntry) sml;

			String obisCode = decodeObisCode(e.getObjName());

			if (obisCode == null) {
				return; // may happen on incomplete SML message
			}

			// System.err.println(obisCode+" "+e.getValue());

			if ("129-129:199.130.3*255".equals(obisCode)) { // manufacturer id
				return;
			}
			if ("1-0:0.0.9*255".equals(obisCode)) { // meter serial
				return;
			}
			if ("1-0:0.0.0*255".equals(obisCode)) { // property number
				return;
			}
			if ("1-0:96.1.0*255".equals(obisCode)) { // meter id
				return;
			}
			if ("1-0:96.5.0*255".equals(obisCode)) { // operation status
				return;
			}
			if ("1-0:96.50.1*1".equals(obisCode)) { // manufacturer id
				return;
			}
			if ("1-0:96.50.1*4".equals(obisCode)) { // manufacturer id
				return;
			}
			if ("1-0:96.50.1*4".equals(obisCode)) { // hardware version
				return;
			}
			if ("1-0:96.50.4*4".equals(obisCode)) { // parameter version
				return;
			}
			if ("1-0:96.90.2*1".equals(obisCode)) { // firmware checksum
				return;
			}
			if ("1-0:0.2.0*0".equals(obisCode)) { // firmware version
				return;
			}
			if ("1-0:97.97.0*0".equals(obisCode)) { // status register
				return;
			}
			if ("129-129:199.130.5*255".equals(obisCode)) { // public key
				return;
			}

			Reading reading = new Reading();
			reading.obisCode = obisCode;
			reading.name = ObisNameMap.get(obisCode);
			reading.unit = (e.getValUnit() == null) ? null : e.getValUnit().toString();
			if (e.getValue() != null) {
				reading.value = decodeNumber(e.getValue(), e.getScaler());
			}

			if (result.readings == null) {
				result.readings = new ArrayList<>();
			}
			result.readings.add(reading);

			return;
		}

		log.warn("SML object not implemented: ", sml.getClass().getName());

	}

	/**
	 * Decodes numeric values, optionally applying a scaler to adjust the magnitude.
	 * This method handles various numeric types and applies a scaling factor if
	 * provided.
	 *
	 * @param value  The numeric value to be decoded.
	 * @param scaler The scaling factor to be applied. If null, no scaling is
	 *               applied.
	 * @return The scaled number if a scaler is provided; otherwise, the original
	 *         number.
	 */
	public static Number decodeNumber(Object value, Number scaler) {
		if (value == null) {
			return null;
		}

		int sc = (scaler == null) ? 0 : scaler.intValue();

		if (value instanceof Byte) {
			byte val = (Byte) value;
			if (sc == 0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		if (value instanceof Short) {
			short val = (Short) value;
			if (sc == 0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		if (value instanceof Integer) {
			int val = (Integer) value;
			if (sc == 0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		if (value instanceof Long) {
			long val = (Long) value;
			if (sc == 0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		if (value instanceof BigInteger) {
			BigInteger val = (BigInteger) value;
			if (sc == 0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		log.warn("Number conversion not implemented: {}", value.getClass().getName());
		return null;
	}

	/**
	 * Decodes an OBIS code from an OctetString.
	 *
	 * @param s The OctetString containing the OBIS code.
	 * @return The decoded OBIS code as a string.
	 */
	public static String decodeObisCode(OctetString s) {
		return (s == null) ? null : decodeObisCode(s.getValue());
	}

	/**
	 * Decodes an OBIS code from a byte array.
	 *
	 * @param bytes The byte array containing the OBIS code.
	 * @return The decoded OBIS code as a string.
	 */
	public static String decodeObisCode(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		sb.append(bytes[0] & 0xff);
		sb.append("-");
		sb.append(bytes[1] & 0xff);
		sb.append(":");
		sb.append(bytes[2] & 0xff);
		sb.append(".");
		sb.append(bytes[3] & 0xff);
		sb.append(".");
		sb.append(bytes[4] & 0xff);
		sb.append("*");
		sb.append(bytes[5] & 0xff);
		return sb.toString();
	}

	/**
	 * Decodes a meter identification number from a byte array. This method is based
	 * on a specific encoding format used for meter identification.
	 * https://netze.estw.de/erlangenGips/Erlangen/__attic__20210120_155237__estw1.de/Kopfnavigation/Netze/Messwesen/Messwesen/Herstelleruebergreifende-Identifikationsnummer-fuer-Messeinrichtungen.pdf
	 *
	 * @param bytes The byte array containing the meter ID.
	 * @return The decoded meter ID as a string.
	 */
	public static String decodeMeterId(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		// 1st byte is 09 on my meter - no idea what the meaning is
		sb.append(Hex.encodeHex(bytes, false)[1]); // 2nd byte is "media"

		sb.append((char) bytes[2]); // 3 bytes manufacturer
		sb.append((char) bytes[3]);
		sb.append((char) bytes[4]);

		// following byte is "version" with 2 digits
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setGroupingUsed(false);
		nf.setMinimumIntegerDigits(2);
		nf.setMaximumIntegerDigits(2);
		sb.append(nf.format(bytes[5]));

		// following 4 bytes are serial with 8 digits

		nf.setMinimumIntegerDigits(8);
		nf.setMaximumIntegerDigits(8);
		sb.append(nf.format(new BigInteger(bytes, 6, 4)));

		return sb.toString();
	}

	protected static byte[] extractMessage(byte[] smlPayload) throws IOException {
		try {
			return new MessageExtractor(new DataInputStream(new ByteArrayInputStream(smlPayload)), 1000)
					.getSmlMessage();
		} catch (IOException ex) {
			if ("Timeout".equals(ex.getMessage())) {
				throw new IOException("Invalid SML payload: " + Hex.encodeHexString(smlPayload));
			}
			throw new IOException("Invalid SML payload: " + ex.getMessage());
		}
	}

}
