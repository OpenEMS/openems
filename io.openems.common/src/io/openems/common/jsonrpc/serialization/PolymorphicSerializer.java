package io.openems.common.jsonrpc.serialization;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsRuntimeException;

public final class PolymorphicSerializer<T> {

	private record PolymorphicSerializerEntry<T>(Class<T> clazz, JsonSerializer<T> serializer, String identifier) {

	}

	public static final class PolymorphicSerializerBuilder<T> {

		private final List<PolymorphicSerializerEntry<? extends T>> entries = new ArrayList<>();

		private PolymorphicSerializerBuilder() {
		}

		/**
		 * Adds a {@link JsonSerializer} to use for a subtype.
		 * 
		 * @param <D>        the subtype
		 * @param clazz      the {@link Class} of the subtype
		 * @param serializer the {@link JsonSerializer}
		 * @param identifier the {@link String} identifier in the json
		 * @return this
		 */
		public <D extends T> PolymorphicSerializerBuilder<T> add(//
				Class<D> clazz, //
				JsonSerializer<D> serializer, //
				String identifier //
		) {
			this.entries.add(new PolymorphicSerializerEntry<D>(clazz, serializer, identifier));
			return this;
		}

		public PolymorphicSerializer<T> build() {
			return new PolymorphicSerializer<>(this.entries);
		}

	}

	/**
	 * Creates a builder for a {@link PolymorphicSerializer}.
	 *
	 * @param <T> the type of the {@link PolymorphicSerializer}
	 * @return the builder
	 */
	public static <T> PolymorphicSerializerBuilder<T> create() {
		return new PolymorphicSerializerBuilder<T>();
	}

	private final Map<String, JsonSerializer<? extends T>> serializerByIdentifier;
	private final Map<Class<? extends T>, JsonSerializer<? extends T>> serializerByClass;

	private PolymorphicSerializer(List<PolymorphicSerializerEntry<? extends T>> entries) {
		this.serializerByIdentifier = entries.stream()
				.collect(toMap(PolymorphicSerializerEntry::identifier, PolymorphicSerializerEntry::serializer));
		this.serializerByClass = entries.stream()
				.collect(toMap(PolymorphicSerializerEntry::clazz, PolymorphicSerializerEntry::serializer));
	}

	/**
	 * Gets a map with identifier to {@link JsonSerializer} values.
	 * 
	 * @return the map
	 */
	public Map<String, JsonSerializer<? extends T>> serializerByIdentifier() {
		return this.serializerByIdentifier;
	}

	/**
	 * Serializes the object to a json with the {@link JsonSerializer
	 * JsonSerializers} set in this instance.
	 * 
	 * @param <D>    the type of the object to serialize
	 * @param object the object to serialize
	 * @return a {@link JsonElement} representing the object
	 */
	@SuppressWarnings("unchecked")
	public <D extends T> JsonElement serialize(D object) {
		@SuppressWarnings("rawtypes")
		final var serializer = (JsonSerializer) this.serializerByClass.get(object.getClass());

		if (serializer == null) {
			throw new OpenemsRuntimeException("Missing serializer for class " + object.getClass().getCanonicalName());
		}

		return serializer.serialize(object);
	}

}