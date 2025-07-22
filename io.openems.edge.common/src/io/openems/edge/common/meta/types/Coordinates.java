package io.openems.edge.common.meta.types;

public record Coordinates(double latitude, double longitude) {

	/**
	 * Creates a {@link Coordinates} instance if the given latitude and longitude
	 * are within valid ranges.
	 *
	 * @param latitude  the latitude value (must be between -90 and 90)
	 * @param longitude the longitude value (must be between -180 and 180)
	 * @return a new {@link Coordinates} object if valid, or null if invalid or not
	 *         set
	 */
	public static Coordinates of(double latitude, double longitude) {
		if (isLatitudeValid(latitude) && isLongitudeValid(longitude)) {
			return new Coordinates(latitude, longitude);
		}
		return null;
	}

	public Coordinates {
		validate(latitude, longitude);
	}

	private static void validate(double latitude, double longitude) {
		if (!isLatitudeValid(latitude)) {
			throw new IllegalArgumentException("Latitude must be between -90 and 90, was: " + latitude);
		}
		if (!isLongitudeValid(longitude)) {
			throw new IllegalArgumentException("Longitude must be between -180 and 180, was: " + longitude);
		}
	}

	private static boolean isLatitudeValid(double latitude) {
		return latitude >= -90 && latitude <= 90;
	}

	private static boolean isLongitudeValid(double longitude) {
		return longitude >= -180 && longitude <= 180;
	}
}