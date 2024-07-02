package io.openems.edge.meter.tibber.pulse.smlparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openmuc.jsml.EUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.meter.tibber.pulse.smlparser.SmlTime.SmlTimeSecIndex;
import io.openems.edge.meter.tibber.pulse.smlparser.asn1.ASN1BERTokenizer;
import io.openems.edge.meter.tibber.pulse.smlparser.asn1.ASN1BERTokenizer.Type;

/**
 * SML message parser, based on
 * https://www.bsi.bund.de/SharedDocs/Downloads/DE/BSI/Publikationen/TechnischeRichtlinien/TR03109/TR-03109-1_Anlage_Feinspezifikation_Drahtgebundene_LMN-Schnittstelle_Teilb.pdf?__blob=publicationFile&v=1
 * References in comments are to the document above. License: AGPLv3
 * 
 * @author mwyraz
 */
public class SmlMessageParser {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Parses a byte array payload into a collection of SMLMessage objects. This
	 * method creates an instance of SmlMessageParser with the given payload and
	 * invokes its parseSmlMessages method to process the SML (Smart Message
	 * Language) data. It is designed to interpret the structured data encoded in
	 * the SML format commonly used for energy metering and other utility readings,
	 * converting it into a more accessible collection of message objects.
	 * 
	 * @param payload the byte array containing the SML encoded data to be parsed.
	 * @return a collection of SMLMessage objects representing the parsed data.
	 */

	public static Collection<SmlMessage> parse(byte[] payload) {
		return new SmlMessageParser(payload).parseSmlMessages();
	}

	protected final ASN1BERTokenizer tokenizer;

	protected SmlMessageParser(byte[] payload) {
		this.tokenizer = new ASN1BERTokenizer(payload);
	}

	protected Collection<SmlMessage> parseSmlMessages() {

		List<SmlMessage> results = new ArrayList<>();

		while (this.tokenizer.hasMoreData()) {

			this.tokenizer.readListOfElements(6, false);

			final int crcStartOffset = this.tokenizer.getOffset();

			SmlMessage result = new SmlMessage();
			result.transactionId = this.tokenizer.readOctetString(false);
			result.groupNo = this.tokenizer.readUnsigned8(false);
			result.abortOnError = this.tokenizer.readUnsigned8(false);
			result.messageBody = this.parseSmlMessageBody();
			result.crc16Actual = Crc16.getCrc16(this.tokenizer.getMessage(), crcStartOffset,
					this.tokenizer.getOffset() - 1);
			result.crc16Expected = this.tokenizer.readUnsigned16(false);
			result.crc16Ok = result.crc16Actual.equals(result.crc16Expected);

			this.tokenizer.readEndOfMessage(false);

			results.add(result);
		}

		return results;
	}

	protected SmlMessageBody parseSmlMessageBody() {
		long choice = this.readChoice32();

		if (choice == 0x00000101) {
			return this.parseSmlPublicOpenResponse();
		}
		if (choice == 0x00000201) {
			return this.parseSmlPublicCloseResponse();
		}
		if (choice == 0x00000701) {
			return this.parseSmlGetListResponse();
		}

		throw new RuntimeException("Unimplemented SML message body: " + ByteUtil.int32ToHex(choice));
	}

	protected SmlPublicOpenResponse parseSmlPublicOpenResponse() {
		this.tokenizer.readListOfElements(6, false);

		SmlPublicOpenResponse result = new SmlPublicOpenResponse();
		result.codepage = this.tokenizer.readOctetString(true);
		result.clientId = this.tokenizer.readOctetString(true);
		result.reqFileId = this.tokenizer.readOctetString(true);
		result.serverId = this.tokenizer.readOctetString(true);
		result.refTime = this.parseSmlTime(true);
		result.smlVersion = this.tokenizer.readUnsigned8(true);

		return result;
	}

	protected SmlPublicCloseResponse parseSmlPublicCloseResponse() {
		this.tokenizer.readListOfElements(1, false);

		SmlPublicCloseResponse result = new SmlPublicCloseResponse();
		result.globalSignature = this.tokenizer.readOctetString(true);

		return result;
	}

	protected SmlGetListResponse parseSmlGetListResponse() {

		this.tokenizer.readListOfElements(7, false);

		SmlGetListResponse result = new SmlGetListResponse();
		result.clientId = this.tokenizer.readOctetString(true);
		result.serverId = this.tokenizer.readOctetString(true);
		result.listName = this.tokenizer.readOctetString(true);
		result.actSensorTime = this.parseSmlTime(true);

		int elementCount = this.tokenizer.readListOfElements(-1, false);
		result.valList = this.parseSmlList(elementCount);

		result.listSignature = this.tokenizer.readOctetString(true);
		result.actGatewayTime = this.parseSmlTime(true);

		return result;

	}

	protected List<SmlListEntry> parseSmlList(int elementCount) {
		List<SmlListEntry> result = new ArrayList<>(elementCount);
		for (int i = 0; i < elementCount; i++) {
			result.add(this.parseSmlListEntry());
		}
		return result;
	}

	protected SmlListEntry parseSmlListEntry() {
		this.tokenizer.readListOfElements(7, false);

		SmlListEntry result = new SmlListEntry();

		result.objName = this.tokenizer.readOctetString(false);
		{
			Number status = (Number) this.tokenizer.readNext(Type.UNSIGNED, -1, true);
			result.status = status == null ? null : status.longValue();
		}
		result.valTime = this.parseSmlTime(true);
		Integer unit = this.tokenizer.readUnsigned8(true);
		result.valUnit = unit == null ? null : EUnit.from(unit);
		result.scaler = this.tokenizer.readSigned8(true);
		result.value = this.parseSmlValue(false);
		result.valueSignature = this.tokenizer.readOctetString(true);

		return result;
	}

	protected Object parseSmlValue(boolean optional) {
		switch (this.tokenizer.readNext()) {
		case NULL:
		case OCTET_STRING:
		case SIGNED:
		case UNSIGNED:
			return this.tokenizer.getObject();
		default:
			throw new RuntimeException("Unsuported type for SML entry value: " + this.tokenizer.getType());
		}

	}

	protected SmlTime parseSmlTime(boolean optional) {

		Type type = this.tokenizer.readNext();

		if (optional && this.tokenizer.getType() == Type.NULL) {
			return null;
		}

		if (type == Type.UNSIGNED && this.tokenizer.getDataLength() == 4) {
			// spec requires a unsigned8 choice here but HYD uses unsigned32 (as value, not as choice) here we just skip it and try to figure out what they mean with this
			return null;
		}

		// Expect a choice (list of 2 elements), maybe optional
		this.tokenizer.expect(Type.LIST, 2, false);

		Integer choice = this.tokenizer.readUnsigned8(false);
		if (choice == null) {
			return null;
		}

		if (choice == 0x01) {
			SmlTimeSecIndex time = new SmlTimeSecIndex();
			// Spec Unsigned32 but Meter from testDZG_DVS_7412_2_jmberg uses Unsigned8 here
			time.secIndex = ((Number) this.tokenizer.readNext(Type.UNSIGNED, -1, false)).longValue();
			return time;
		}

		throw new RuntimeException("Unimplemented SML time: " + ByteUtil.int32ToHex(choice));
	}

	protected long readChoice32() {
		this.tokenizer.readListOfElements(2, false);

		Number value = (Number) this.tokenizer.readNext(Type.UNSIGNED, -1, false);

		return value.longValue();
	}

}