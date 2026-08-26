package io.openems.edge.bridge.modbus.sunspec.generator;

import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.getAsStringOrElse;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;

/**
 * POJO container for a SunSpec Model.
 */
public record Model(int id, int len, String name, String label, String description, ImmutableList<Point> points,
		SunSpecModelType modelType) {

	/**
	 * Builds a {@link Model} from a {@link JsonObject}.
	 * 
	 * @param model the {@link JsonObject}
	 * @return a {@link Model}
	 * @throws OpenemsNamedException on error
	 */
	public static Model fromJson(JsonObject model) throws OpenemsNamedException {
		var id = getAsInt(model, "id");
		// Fixed blocks elements
		var group = model.get("group").getAsJsonObject();
		// Repeated elements
		var groups = group.has("groups") ? group.get("groups").getAsJsonArray() : new JsonArray();
		final var name = getAsString(group, "name");
		final var label = getAsStringOrElse(group, "label", "");
		final var description = getAsStringOrElse(group, "desc", "");

		var offset = 0;
		var sizeOfFixedRegisters = 0;

		final var list = ImmutableList.<Point>builder();

		// Add the fixed block to list
		for (var point : getAsJsonArray(group, "points")) {
			var p = Point.fromJson(getAsJsonObject(point), offset);
			// ID and length not to be considered as points
			if (!p.id().equals("ID") && !p.id().equals("L")) {
				list.add(p);
				sizeOfFixedRegisters += p.len();
			}
			offset += p.len();

		}
		// Calculate the size of the repeated registers
		var sizeOfRepeatedRegisters = 0;
		for (var point : groups) {
			var pointObj = point.getAsJsonObject();
			var fp = pointObj.get("points").getAsJsonArray();
			for (var subPoint : fp) {
				var p = Point.fromJson(getAsJsonObject(subPoint), offset);
				sizeOfRepeatedRegisters += p.len();
			}
		}

		// this is value is taken from the excel sheet
		// This should be basically read from the modbus
		var modelLength = 16;
		var numberOfRepeatingElements = 0;

		if (sizeOfRepeatedRegisters > 0) {
			numberOfRepeatingElements = (modelLength - sizeOfFixedRegisters) / sizeOfRepeatedRegisters;
			// Ensure at least one repeating block is present if sizeOfRepeatedRegisters > 0
			numberOfRepeatingElements = Math.max(numberOfRepeatingElements, 1);
		}

		// add the repeateable registers to list with appending the number repetition
		for (int i = 1; i <= numberOfRepeatingElements; i++) {

			for (var point : groups) {

				var pointObj = point.getAsJsonObject();
				var fp = pointObj.get("points").getAsJsonArray();

				for (var subPoint : fp) {
					var p = Point.fromJson(getAsJsonObject(subPoint), offset, i);
					// ID and length not to be considered as points
					if (!p.id().equals("ID") && !p.id().equals("L")) {
						list.add(p);
					}
					offset += p.len();
				}

			}
		}
		var points = list.build();
		var len = points.stream() //
				.map(Point::len) //
				.reduce(0, (t, p) -> t + p);
		var modelType = SunSpecModelType.getModelType(id);
		return new Model(id, len, name, label, description, points, modelType);
	}

	/**
	 * Gets the Point with the given Id.
	 *
	 * @param id the Point-ID
	 * @return the Point
	 * @throws OpenemsException on error
	 */
	public Point getPoint(String id) throws OpenemsException {
		return this.points.stream() //
				.filter(p -> p.id() == id) //
				.findFirst() //
				.orElseThrow(() -> new OpenemsException("Unable to find Point with ID " + id));
	}

	@Override
	public String toString() {
		return "Model [id=" + this.id + ", name=" + this.name + ", points=" + this.points + ", label=" + this.label
				+ ", description=" + this.description + "]";
	}
}