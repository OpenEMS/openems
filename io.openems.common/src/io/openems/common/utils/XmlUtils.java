package io.openems.common.utils;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

public class XmlUtils {

	public static String namedNodeMapToString(NamedNodeMap attrs) {
		List<Node> list = new ArrayList<>();
		for (int i = 0; i < attrs.getLength(); i++) {
			list.add(attrs.item(i));
		}
		return list.toString();
	}

	public static Node getSubNode(NamedNodeMap attrs, String name) throws OpenemsNamedException {
		Node subNode = attrs.getNamedItem(name);
		if (subNode == null) {
			throw OpenemsError.XML_HAS_NO_MEMBER.exception(namedNodeMapToString(attrs).replaceAll("%", "%%"), name);
		} else {
			return subNode;
		}
	}

	public static String getAsString(NamedNodeMap attrs, String name) throws OpenemsNamedException {
		try {
			return getSubNode(attrs, name).getNodeValue();
		} catch (DOMException | NullPointerException e) {
			throw OpenemsError.XML_NO_STRING_MEMBER.exception(namedNodeMapToString(attrs).replaceAll("%", "%%"), name);
		}
	}

	public static String getAsStringOrElse(NamedNodeMap attrs, String name, String def) {
		try {
			return getAsString(attrs, name);
		} catch (OpenemsNamedException e) {
			return def;
		}
	}

	public static int getAsInt(NamedNodeMap attrs, String name) throws OpenemsNamedException {
		try {
			return Integer.parseInt(getAsString(attrs, name));
		} catch (NumberFormatException e) {
			throw new OpenemsException(e);
		}
	}

	public static int getAsIntOrElse(NamedNodeMap attrs, String name, int def) {
		try {
			return getAsInt(attrs, name);
		} catch (OpenemsNamedException e) {
			return def;
		}
	}

	public static <E extends Enum<E>> E getAsEnum(Class<E> enumType, NamedNodeMap attrs, String name)
			throws OpenemsNamedException {
		String element = getAsString(attrs, name);
		try {
			return (E) Enum.valueOf(enumType, element.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new OpenemsException(e);
		}
	}

	public static <E extends Enum<E>> E getAsEnumOrElse(Class<E> enumType, NamedNodeMap attrs, String name, E def) {
		try {
			return getAsEnum(enumType, attrs, name);
		} catch (OpenemsNamedException e) {
			return def;
		}
	}

	public static boolean getAsBoolean(NamedNodeMap attrs, String name) throws OpenemsNamedException {
		String element = getAsString(attrs, name);
		return Boolean.parseBoolean(element);
	}

	public static boolean getAsBooleanOrElse(NamedNodeMap attrs, String name, boolean def) {
		try {
			return getAsBoolean(attrs, name);
		} catch (OpenemsNamedException e) {
			return def;
		}
	}

	public static String getContentAsString(Node node) {
		return node.getTextContent();
	}

	public static int getContentAsInt(Node node) {
		return Integer.valueOf(node.getTextContent());
	}
}
