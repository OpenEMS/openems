package io.openems.common.xml.serialization;

import java.util.List;
import java.util.stream.Stream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedRuntimeException;
import io.openems.common.utils.XmlUtils;

/**
 * XML helper class that gets created for every XML object. An object contains
 * children, an element does not.
 */
public class XmlObject extends XmlElement {
	XmlObject(Element node) {
		super(node);
	}

	/**
	 * Returns whatever a child with the given name exists in this object.
	 *
	 * @param name Child name
	 * @return True if the child exists
	 */
	public boolean hasChild(String name) {
		return this.node.getElementsByTagName(name).getLength() != 0;
	}

	/**
	 * Returns the element of a child. Can be a string, integer, double, and more.
	 * Throws {@link OpenemsNamedRuntimeException} if child is not found or is not a
	 * element.
	 *
	 * @param name Child name
	 * @return Child element
	 */
	public XmlElement getChild(String name) {
		return new XmlElement(this.getChildElement(name));
	}

	/**
	 * Returns all children with the matching name as a java stream. Should be used
	 * for value elements, like string, integer, double, and more.
	 *
	 * @param name Children name
	 * @return Stream of children elements
	 */
	public Stream<XmlElement> getChildrenAsStream(String name) {
		return this.getChildrenNodesAsStream(name)//
				.filter(x -> x instanceof Element)//
				.map(x -> new XmlElement((Element) x));
	}

	/**
	 * Returns a child object. Should be used if the child is a object, e.g. it
	 * contains other children. Throws {@link OpenemsNamedRuntimeException} if child
	 * is not found or is not a object.
	 *
	 * @param name Child name
	 * @return Object
	 */
	public XmlObject getChildObject(String name) {
		return new XmlObject(this.getChildElement(name));
	}

	/**
	 * Returns a list of children objects with matching name. If there is no
	 * matching child or if one of the elements is not a object, it is ignored.
	 *
	 * @param name Children name
	 * @return Object
	 */
	public List<XmlObject> getChildObjects(String name) {
		return this.getChildObjectsStream(name).toList();
	}

	/**
	 * Returns a list of children objects with matching name as stream. If there is
	 * no matching child or if one of the elements is not a object, it is ignored.
	 *
	 * @param name Children name
	 * @return Java stream with xml objects
	 */
	public Stream<XmlObject> getChildObjectsStream(String name) {
		return this.getChildrenNodesAsStream(name)//
				.filter(x -> x instanceof Element)//
				.map(x -> new XmlObject((Element) x));
	}

	private Element getChildElement(String name) {
		var node = this.getChildNode(name);
		if (!(node instanceof Element)) {
			throw OpenemsError.XML_NO_ELEMENT_MEMBER.runtimeException(this.getName(), name);
		}

		return (Element) node;
	}

	private Node getChildNode(String name) {
		var nodeList = this.node.getElementsByTagName(name);
		if (nodeList.getLength() == 0) {
			throw OpenemsError.XML_HAS_NO_MEMBER.runtimeException(this.getName(), name);
		}

		return nodeList.item(0);
	}

	private Stream<Node> getChildrenNodesAsStream(String name) {
		var nodeList = this.node.getElementsByTagName(name);
		return XmlUtils.stream(nodeList);
	}
}
