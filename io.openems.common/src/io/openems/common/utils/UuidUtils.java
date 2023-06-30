package io.openems.common.utils;

import java.util.UUID;

public class UuidUtils {

	/**
	 * Create a 'Nil' UUID: "00000000-0000-0000-0000-000000000000".
	 *
	 * <p>
	 *
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Universally_unique_identifier#Nil_UUID">Wikipedia</a>
	 *
	 * @return a Nil UUID
	 */
	public static UUID getNilUuid() {
		return new UUID(0, 0);
	}

}
