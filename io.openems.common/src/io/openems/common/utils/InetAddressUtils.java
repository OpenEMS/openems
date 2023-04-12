package io.openems.common.utils;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import io.openems.common.exceptions.OpenemsException;

public final class InetAddressUtils {

	private InetAddressUtils() {

	}

	/**
	 * Parses a string to an {@link Inet4Address}.
	 * 
	 * <p>
	 * See {@link Inet4Address#getByName(String)}
	 * 
	 * @param value the string value
	 * @return an {@link Inet4Address} or null
	 */
	public static Inet4Address parseOrNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return (Inet4Address) Inet4Address.getByName(value.strip());
		} catch (UnknownHostException e) {
			// handled below
		}
		return null;
	}

	/**
	 * Parses a string to an {@link Inet4Address} or throws an error.
	 * 
	 * <p>
	 * See {@link Inet4Address#getByName(String)}
	 * 
	 * @param value the string value
	 * @throws OpenemsException on error
	 */
	public static Inet4Address parseOrError(String value) throws OpenemsException {
		if (value == null) {
			throw new OpenemsException("IPv4 address is null");
		}
		if (value.isBlank()) {
			throw new OpenemsException("IPv4 address is blank");
		}
		try {
			return (Inet4Address) Inet4Address.getByName(value.strip());
		} catch (UnknownHostException e) {
			throw new OpenemsException("Unable to parse IPv4 address [" + value + "] " + e.getMessage());
		}
	}

}
