package io.openems.edge.meter.discovergy.jsonrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

public class DiscovergyMeter {

	/**
	 * Factory.
	 * 
	 * @param j the {@link JsonElement}
	 * @return the {@link DiscovergyMeter}
	 * @throws OpenemsNamedException on error
	 */
	public static DiscovergyMeter fromJson(JsonElement j) throws OpenemsNamedException {
		var meterId = JsonUtils.getAsString(j, "meterId");
		// e.g. "ESY"
		var manufacturerId = JsonUtils.getAsOptionalString(j, "manufacturerId").orElse("");
		// e.g. "12345678"
		var serialNumber = JsonUtils.getAsOptionalString(j, "serialNumber").orElse("");
		// e.g. 1ESY1234567890
		var fullSerialNumber = JsonUtils.getAsOptionalString(j, "fullSerialNumber").orElse("");
		var location = Location.fromJson(JsonUtils.getAsJsonObject(j, "location"));
		// e.g. "DE0012345678910000000000001234567"
		var administrationNumber = JsonUtils.getAsOptionalString(j, "administrationNumber").orElse("");
		// e.g. "EASYMETER"
		var type = JsonUtils.getAsOptionalString(j, "type").orElse("");
		// e.g. "ELECTRICITY"
		var measurementType = JsonUtils.getAsOptionalString(j, "measurementType").orElse("");
		// e.g. "SLP"
		var loadProfileType = JsonUtils.getAsOptionalString(j, "loadProfileType").orElse("");
		// e.g. "1"
		int scalingFactor = JsonUtils.getAsOptionalInt(j, "scalingFactor").orElse(1);
		// e.g. "1"
		int currentScalingFactor = JsonUtils.getAsOptionalInt(j, "currentScalingFactor").orElse(1);
		// e.g. "1"
		int voltageScalingFactor = JsonUtils.getAsOptionalInt(j, "voltageScalingFactor").orElse(1);
		// e.g. "1"
		int internalMeters = JsonUtils.getAsOptionalInt(j, "internalMeters").orElse(1);
		// e.g. "1538684548220"
		long firstMeasurementTime = JsonUtils.getAsOptionalInt(j, "firstMeasurementTime").orElse(1);
		// e.g. "1572172771244"
		long lastMeasurementTime = JsonUtils.getAsOptionalInt(j, "lastMeasurementTime").orElse(1);
		return new DiscovergyMeter(meterId, manufacturerId, serialNumber, fullSerialNumber, location,
				administrationNumber, type, measurementType, loadProfileType, scalingFactor, currentScalingFactor,
				voltageScalingFactor, internalMeters, firstMeasurementTime, lastMeasurementTime);
	}

	public static class Location {

		/**
		 * Factory.
		 * 
		 * @param j the {@link JsonElement}
		 * @return the {@link Location}
		 * @throws OpenemsNamedException on error
		 */
		public static Location fromJson(JsonObject j) throws OpenemsNamedException {
			var street = JsonUtils.getAsString(j, "street");
			var streetNumber = JsonUtils.getAsString(j, "streetNumber");
			var zip = JsonUtils.getAsString(j, "zip");
			var city = JsonUtils.getAsString(j, "city");
			var country = JsonUtils.getAsString(j, "country");
			return new Location(street, streetNumber, zip, city, country);
		}

		private final String street;
		private final String streetNumber;
		private final String zip;
		private final String city;
		private final String country;

		public Location(String street, String streetNumber, String zip, String city, String country) {
			this.street = street;
			this.streetNumber = streetNumber;
			this.zip = zip;
			this.city = city;
			this.country = country;
		}

		/**
		 * Converts to {@link JsonObject}.
		 * 
		 * @return the {@link JsonObject}
		 */
		public JsonObject toJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("street", this.street) //
					.addProperty("streetNumber", this.streetNumber) //
					.addProperty("zip", this.zip) //
					.addProperty("city", this.city) //
					.addProperty("country", this.country) //
					.build();
		}
	}

	private final String meterId;
	private final String manufacturerId;
	private final String serialNumber;
	private final String fullSerialNumber;
	private final String type;
	private final String measurementType;
	private final Location location;
	private final String administrationNumber;
	private final String loadProfileType;
	private final int scalingFactor;
	private final int currentScalingFactor;
	private final int voltageScalingFactor;
	private final int internalMeters;
	private final long firstMeasurementTime;
	private final long lastMeasurementTime;

	private DiscovergyMeter(String meterId, String manufacturerId, String serialNumber, String fullSerialNumber,
			Location location, String administrationNumber, String type, String measurementType, String loadProfileType,
			int scalingFactor, int currentScalingFactor, int voltageScalingFactor, int internalMeters,
			long firstMeasurementTime, long lastMeasurementTime) {
		this.meterId = meterId;
		this.manufacturerId = manufacturerId;
		this.serialNumber = serialNumber;
		this.fullSerialNumber = fullSerialNumber;
		this.type = type;
		this.measurementType = measurementType;
		this.location = location;
		this.administrationNumber = administrationNumber;
		this.loadProfileType = loadProfileType;
		this.scalingFactor = scalingFactor;
		this.currentScalingFactor = currentScalingFactor;
		this.voltageScalingFactor = voltageScalingFactor;
		this.internalMeters = internalMeters;
		this.firstMeasurementTime = firstMeasurementTime;
		this.lastMeasurementTime = lastMeasurementTime;
	}

	@Override
	public String toString() {
		return "[meterId=" + this.meterId + ", manufacturerId=" + this.manufacturerId + ", serialNumber="
				+ this.serialNumber + ", fullSerialNumber=" + this.getFullSerialNumber() + ", type=" + this.type
				+ ", measurementType=" + this.measurementType + "]";
	}

	/**
	 * Converts to {@link JsonObject}.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("meterId", this.meterId) //
				.addProperty("manufacturerId", this.manufacturerId) //
				.addProperty("serialNumber", this.serialNumber) //
				.addProperty("fullSerialNumber", this.getFullSerialNumber()) //
				.add("location", this.location.toJson()) //
				.addProperty("type", this.type) //
				.addProperty("measurementType", this.measurementType) //
				.addProperty("administrationNumber", this.administrationNumber) //
				.addProperty("loadProfileType", this.loadProfileType) //
				.addProperty("scalingFactor", this.scalingFactor) //
				.addProperty("currentScalingFactor", this.currentScalingFactor) //
				.addProperty("voltageScalingFactor", this.voltageScalingFactor) //
				.addProperty("internalMeters", this.internalMeters) //
				.addProperty("firstMeasurementTime", this.firstMeasurementTime) //
				.addProperty("lastMeasurementTime", this.lastMeasurementTime) //
				.build();
	}

	public String getMeterId() {
		return this.meterId;
	}

	public String getManufacturerId() {
		return this.manufacturerId;
	}

	public String getSerialNumber() {
		return this.serialNumber;
	}

	public String getFullSerialNumber() {
		return this.fullSerialNumber;
	}

	public String getType() {
		return this.type;
	}

	public String getMeasurementType() {
		return this.measurementType;
	}

	public Location getLocation() {
		return this.location;
	}

	public String getAdministrationNumber() {
		return this.administrationNumber;
	}

	public String getLoadProfileType() {
		return this.loadProfileType;
	}

	public int getScalingFactor() {
		return this.scalingFactor;
	}

	public int getCurrentScalingFactor() {
		return this.currentScalingFactor;
	}

	public int getVoltageScalingFactor() {
		return this.voltageScalingFactor;
	}

	public int getInternalMeters() {
		return this.internalMeters;
	}

	public long getLastMeasurementTime() {
		return this.lastMeasurementTime;
	}

	public long getFirstMeasurementTime() {
		return this.firstMeasurementTime;
	}

}