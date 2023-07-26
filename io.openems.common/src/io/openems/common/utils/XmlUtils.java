package io.openems.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class XmlUtils {

	/**
	 * Converts a {@link NamedNodeMap} to a string representative.
	 *
	 * @param attrs the {@link NamedNodeMap}
	 * @return a string
	 */
	public static String namedNodeMapToString(NamedNodeMap attrs) {
		List<Node> list = new ArrayList<>();
		for (var i = 0; i < attrs.getLength(); i++) {
			list.add(attrs.item(i));
		}
		return list.toString();
	}

	/**
	 * Gets the Sub-Node of a {@link NamedNodeMap} with the given name.
	 *
	 * @param attrs the {@link NamedNodeMap}
	 * @param name  the name of the Sub-Node
	 * @return the {@link Node}
	 * @throws OpenemsNamedException on error
	 */
	public static Node getSubNode(NamedNodeMap attrs, String name) throws OpenemsNamedException {
		var subNode = attrs.getNamedItem(name);
		if (subNode == null) {
			throw OpenemsError.XML_HAS_NO_MEMBER.exception(XmlUtils.namedNodeMapToString(attrs).replace("%", "%%"),
					name);
		}
		return subNode;
	}

	/**
	 * Gets the value of a Sub-Node of a {@link NamedNodeMap} with the given name as
	 * String.
	 *
	 * @param attrs the {@link NamedNodeMap}
	 * @param name  the name of the Sub-Node
	 * @return the value of the {@link Node}
	 * @throws OpenemsNamedException on error
	 */
	public static String getAsString(NamedNodeMap attrs, String name) throws OpenemsNamedException {
		try {
			return XmlUtils.getSubNode(attrs, name).getNodeValue();
		} catch (DOMException | NullPointerException e) {
			throw OpenemsError.XML_NO_STRING_MEMBER.exception(XmlUtils.namedNodeMapToString(attrs).replace("%", "%%"),
					name);
		}
	}

	/**
	 * Gets the value of a Sub-Node of a {@link NamedNodeMap} with the given name as
	 * String; otherwise the alternative value.
	 *
	 * @param attrs the {@link NamedNodeMap}
	 * @param name  the name of the Sub-Node
	 * @param def   the alternative value
	 * @return the value of the {@link Node}
	 * @throws OpenemsNamedException on error
	 */
	public static String getAsStringOrElse(NamedNodeMap attrs, String name, String def) {
		try {
			return XmlUtils.getAsString(attrs, name);
		} catch (OpenemsNamedException e) {
			return def;
		}
	}

	/**
	 * Gets the value of a Sub-Node of a {@link NamedNodeMap} with the given name as
	 * Integer.
	 *
	 * @param attrs the {@link NamedNodeMap}
	 * @param name  the name of the Sub-Node
	 * @return the value of the {@link Node}
	 * @throws OpenemsNamedException on error
	 */
	public static int getAsInt(NamedNodeMap attrs, String name) throws OpenemsNamedException {
		try {
			return Integer.parseInt(XmlUtils.getAsString(attrs, name));
		} catch (NumberFormatException e) {
			throw new OpenemsException(e);
		}
	}

	/**
	 * Gets the value of a Sub-Node of a {@link NamedNodeMap} with the given name as
	 * Integer; otherwise the alternative value.
	 *
	 * @param attrs the {@link NamedNodeMap}
	 * @param name  the name of the Sub-Node
	 * @param def   the alternative value
	 * @return the value of the {@link Node}
	 * @throws OpenemsNamedException on error
	 */
	public static int getAsIntOrElse(NamedNodeMap attrs, String name, int def) {
		try {
			return XmlUtils.getAsInt(attrs, name);
		} catch (OpenemsNamedException e) {
			return def;
		}
	}

	/**
	 * Gets the value of a Sub-Node of a {@link NamedNodeMap} with the given name as
	 * Enum.
	 *
	 * @param <E>      the type of the {@link Enum}
	 * @param enumType the class of the {@link Enum}
	 * @param attrs    the {@link NamedNodeMap}
	 * @param name     the name of the Sub-Node
	 * @return the value of the {@link Node}
	 * @throws OpenemsNamedException on error
	 */
	public static <E extends Enum<E>> E getAsEnum(Class<E> enumType, NamedNodeMap attrs, String name)
			throws OpenemsNamedException {
		var element = XmlUtils.getAsString(attrs, name);
		try {
			return Enum.valueOf(enumType, element.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new OpenemsException(e);
		}
	}

	/**
	 * Gets the value of a Sub-Node of a {@link NamedNodeMap} with the given name as
	 * Enum; otherwise the alternative value.
	 *
	 * @param <E>      the type of the {@link Enum}
	 * @param enumType the class of the {@link Enum}
	 * @param attrs    the {@link NamedNodeMap}
	 * @param name     the name of the Sub-Node
	 * @param def      the alternative value
	 * @return the value of the {@link Node}
	 * @throws OpenemsNamedException on error
	 */
	public static <E extends Enum<E>> E getAsEnumOrElse(Class<E> enumType, NamedNodeMap attrs, String name, E def) {
		try {
			return XmlUtils.getAsEnum(enumType, attrs, name);
		} catch (OpenemsNamedException e) {
			return def;
		}
	}

	/**
	 * Gets the value of a Sub-Node of a {@link NamedNodeMap} with the given name as
	 * Boolean.
	 *
	 * @param attrs the {@link NamedNodeMap}
	 * @param name  the name of the Sub-Node
	 * @return the value of the {@link Node}
	 * @throws OpenemsNamedException on error
	 */
	public static boolean getAsBoolean(NamedNodeMap attrs, String name) throws OpenemsNamedException {
		var element = XmlUtils.getAsString(attrs, name);
		return Boolean.parseBoolean(element);
	}

	/**
	 * Gets the value of a Sub-Node of a {@link NamedNodeMap} with the given name as
	 * Boolean; otherwise the alternative value.
	 *
	 * @param attrs the {@link NamedNodeMap}
	 * @param name  the name of the Sub-Node
	 * @param def   the alternative value
	 * @return the value of the {@link Node}
	 * @throws OpenemsNamedException on error
	 */
	public static boolean getAsBooleanOrElse(NamedNodeMap attrs, String name, boolean def) {
		try {
			return XmlUtils.getAsBoolean(attrs, name);
		} catch (OpenemsNamedException e) {
			return def;
		}
	}

	/**
	 * Gets the Content of a {@link Node}.
	 *
	 * @param node the {@link Node}
	 * @return the text content as string
	 */
	public static String getContentAsString(Node node) {
		return node.getTextContent();
	}

	/**
	 * Gets the Content of a {@link Node} as Integer.
	 *
	 * @param node the {@link Node}
	 * @return the text content as string
	 */
	public static int getContentAsInt(Node node) {
		return Integer.parseInt(node.getTextContent());
	}

	/**
	 * Iterates through a {@link Node}.
	 * 
	 * <p>
	 * Source: https://stackoverflow.com/a/48153597/4137113
	 * 
	 * @param node the {@link Node}
	 * @return the {@link Iterable}
	 */
	public static Iterable<Node> list(final Node node) {
		return () -> new Iterator<Node>() {

			private int index = 0;

			@Override
			public boolean hasNext() {
				return this.index < node.getChildNodes().getLength();
			}

			@Override
			public Node next() {
				if (!this.hasNext()) {
					throw new NoSuchElementException();
				}
				return node.getChildNodes().item(this.index++);
			}
		};
	}

	/**
	 * Iterates over {@link Node} through {@link Stream}.
	 * 
	 * <p>
	 * Source: https://stackoverflow.com/a/62171621/4137113
	 * 
	 * @param node the {@link Node}
	 * @return the {@link Stream}
	 */
	public static Stream<Node> stream(final Node node) {
		var childNodes = node.getChildNodes();
		return IntStream.range(0, childNodes.getLength()).boxed().map(childNodes::item);
	}
}
