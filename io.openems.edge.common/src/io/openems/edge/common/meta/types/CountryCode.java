package io.openems.edge.common.meta.types;

/**
 * Enum representing ISO 3166-1 alpha-2 country codes.
 *
 * <p>
 * Includes a special {@code UNDEFINED} constant for unknown or undefined codes.
 */
public enum CountryCode {
	UNDEFINED("Undefined"), //

	DE("Germany"), //
	AT("Austria"), //
	CH("Switzerland"), //
	ES("Spain"), //
	GR("Greece"), //
	NL("Netherlands"), //
	RO("Romania"), //
	SE("Sweden"), //
	LT("Lithuania"), //
	CZ("Czechia"), //

	;

	private final String countryName;

	private CountryCode(String countryName) {
		this.countryName = countryName;
	}

	/**
	 * Gets the ISO 3166-1 alpha-2 country code.
	 * 
	 * @return ISO 3166-1 alpha-2 country code
	 */
	public String getCode() {
		return this.name();
	}

	/**
	 * Gets the Country Name in English language.
	 * 
	 * @return country name
	 */
	public String getCountryName() {
		return this.countryName;
	}

	/**
	 * Converts a ISO 3166-1 alpha-2 country code string (e.g. "DE") to the
	 * corresponding {@link CountryCode} enum constant. Returns {@link #UNDEFINED}
	 * if the input is null, blank, or not recognized.
	 *
	 * @param code the country code string
	 * @return the matching {@link CountryCode} or {@link #UNDEFINED}
	 */
	public static CountryCode fromCode(String code) {
		if (code == null || code.isBlank()) {
			return UNDEFINED;
		}
		try {
			return valueOf(code.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return UNDEFINED;
		}
	}
}
