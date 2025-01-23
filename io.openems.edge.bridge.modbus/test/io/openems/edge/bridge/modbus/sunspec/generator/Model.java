package io.openems.edge.bridge.modbus.sunspec.generator;

import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.getAsStringOrElse;

import com.google.common.collect.ImmutableList;
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
		var group = model.get("group").getAsJsonObject();
		var name = getAsString(group, "name");
		var label = getAsStringOrElse(group, "label", "");
		var description = getAsStringOrElse(group, "desc", "");

		var offset = 0;
		final var list = ImmutableList.<Point>builder();
		for (var point : getAsJsonArray(group, "points")) {
			var p = Point.fromJson(getAsJsonObject(point), offset);
			// ID and length not to be considered as points
			if (!p.id().equals("ID") && !p.id().equals("L")) {
				list.add(p);
			}
			offset += p.len();
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