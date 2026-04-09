package io.openems.common.xml.serialization;

import org.w3c.dom.Element;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedRuntimeException;

/**
 * XML helper class that gets created for every xml element. An element has a
 * value and does not have children.
 */
public class XmlElement {
	protected final Element node;

	XmlElement(Element node) {
		this.node = node;
	}

	public String getValue() {
		return this.node.getTextContent();
	}

	/**
	 * Returns the value as integer. Throws {@link OpenemsNamedRuntimeException} if
	 * value is not a int.
	 *
	 * @return Integer value
	 */
	public int getValueAsInt() {
		var parsedNumber = Ints.tryParse(this.getValue());
		if (parsedNumber == null) {
			throw OpenemsError.XML_NO_INT.runtimeException(this.node.getNodeName(), this.getValue());
		}
		return parsedNumber;
	}

	/**
	 * Returns the value as double. Throws {@link OpenemsNamedRuntimeException} if
	 * value is not a double.
	 *
	 * @return Double value
	 */
	public double getValueAsDouble() {
		var parsedNumber = Doubles.tryParse(this.getValue());
		if (parsedNumber == null) {
			throw OpenemsError.XML_NO_DOUBLE.runtimeException(this.node.getNodeName(), this.getValue());
		}
		return parsedNumber;
	}

	/**
	 * Returns an attribute of this element. Example for an attribute:
	 * 
	 * <pre>
	 *     &lt;person name="Hans"&gt;&lt;/person&gt;
	 * </pre>
	 *
	 * @param name Name of the attribute
	 * @return Value of the attribute or null if not found.
	 */
	public String getAttribute(String name) {
		return this.node.getAttribute(name);
	}

	public String getName() {
		return this.node.getNodeName();
	}

	/**
	 * Returns the underlying XML node.
	 * 
	 * @return XML node
	 */
	public Element getNode() {
		return this.node;
	}
}
