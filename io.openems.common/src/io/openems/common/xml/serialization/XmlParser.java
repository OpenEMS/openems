package io.openems.common.xml.serialization;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class XmlParser {
	public static final XmlParser INSTANCE = new XmlParser();

	private XmlParser() {
	}

	/**
	 * Deserializes from a {@link String} XML.
	 *
	 * @param xml XML string
	 * @return Returns {@link XmlObject} utility
	 * @throws OpenemsNamedException Throws error if parsing fails
	 */
	public XmlObject parseXml(String xml) throws OpenemsNamedException {
		var reader = new StringReader(xml);
		return this.parseXml(reader);
	}

	/**
	 * Deserializes from a {@link Reader} with XML data.
	 *
	 * @param reader Reader containing XML string
	 * @return Returns {@link XmlObject} utility
	 * @throws OpenemsNamedException Throws error if parsing fails
	 */
	public XmlObject parseXml(Reader reader) throws OpenemsNamedException {
		try {
			var documentBuilder = this.createDocumentBuilder();
			var inputSource = new InputSource(reader);

			var document = documentBuilder.parse(inputSource);
			return new XmlObject(document.getDocumentElement());
		} catch (Exception ex) {
			throw new OpenemsException("Failed to parse xml", ex);
		}
	}

	protected DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
		var factory = DocumentBuilderFactory.newInstance();
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setIgnoringComments(true);

		return factory.newDocumentBuilder();
	}
}
