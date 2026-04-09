package io.openems.backend.metadata.odoo.odoo.http;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.List;

import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Level;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record OdooGetEdgesRequest(//
		String externalUserId, //
		int page, //
		int limit, //
		String query, //
		SearchParams searchParams//
) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link OdooGetEdgesRequest}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<OdooGetEdgesRequest> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(OdooGetEdgesRequest.class, json -> {
			return new OdooGetEdgesRequest(//
					json.getString("external_uid"), //
					json.getInt("page"), //
					json.getInt("limit"), //
					json.getStringOrNull("query"), //
					json.getObjectOrNull("searchParams", SearchParams.serializer()));
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("external_uid", obj.externalUserId()) //
					.addProperty("page", obj.page()) //
					.addProperty("limit", obj.limit()) //
					.addPropertyIfNotNull("query", obj.query()) //
					.onlyIf(obj.searchParams() != null,
							b -> b.add("searchParams", SearchParams.serializer().serialize(obj.searchParams())))//
					.build();
		});
	}

	public record SearchParams(//
			List<String> productTypes, //
			List<Level> sumStates, //
			OdooGetEdgesRequest.OrderState orderState, //
			boolean searchIsOnline, //
			boolean isOnline //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link OdooGetEdgesRequest.SearchParams}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OdooGetEdgesRequest.SearchParams> serializer() {
			return jsonObjectSerializer(OdooGetEdgesRequest.SearchParams.class, json -> {
				final var isOnline = json.getBooleanPathNullable("isOnline");
				return new OdooGetEdgesRequest.SearchParams(//
						json.getList("producttype", JsonElementPath::getAsString), //
						json.getJsonArrayPath("sumState") //
								.collect(mapping(t -> t.getAsEnum(Level.class), toList())), //
						json.getObjectOrNull("orderState", OdooGetEdgesRequest.OrderState.serializer()), //
						isOnline.isPresent(), //
						isOnline.getOrDefault(false));
			}, obj -> JsonUtils.buildJsonObject() //
					.onlyIf(!obj.productTypes().isEmpty(), b -> b.add("producttype", obj.productTypes().stream() //
							.map(JsonPrimitive::new) //
							.collect(toJsonArray())))
					.onlyIf(!obj.sumStates().isEmpty(), b -> b.add("sumState", obj.sumStates().stream() //
							.map(Level::name) //
							.map(JsonPrimitive::new) //
							.collect(toJsonArray())))
					.onlyIf(obj.orderState() != null, b -> {
						b.add("orderState", OdooGetEdgesRequest.OrderState.serializer().serialize(obj.orderState()));
					}) //
					.onlyIf(obj.searchIsOnline(), t -> t.addProperty("isOnline", obj.isOnline())) //
					.build());
		}

		/**
		 * Creates a new instance of {@link OdooGetEdgesRequest.SearchParams} from the
		 * given {@link GetEdgesRequest.PaginationOptions.SearchParams}.
		 * 
		 * @param searchParams the
		 *                     {@link GetEdgesRequest.PaginationOptions.SearchParams} to
		 *                     create the {@link OdooGetEdgesRequest.SearchParams} from
		 * @return the created {@link OdooGetEdgesRequest.SearchParams}
		 */
		public static SearchParams from(GetEdgesRequest.PaginationOptions.SearchParams searchParams) {
			return new SearchParams(//
					searchParams.productTypes(), //
					searchParams.sumStates(), //
					searchParams.orderState() == null ? null
							: new OrderState(searchParams.orderState().orderItems().stream() //
									.map(t -> {
										return new OrderItem(t.field(), switch (t.sortOrder()) {
										case ASC -> OdooGetEdgesRequest.SortOrder.ASC;
										case DESC -> OdooGetEdgesRequest.SortOrder.DESC;
										});
									}) //
									.toList()), //
					searchParams.searchIsOnline(), //
					searchParams.isOnline());
		}

	}

	public enum SortOrder {
		ASC, DESC
	}

	public record OrderItem(String field, OdooGetEdgesRequest.SortOrder sortOrder) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link OdooGetEdgesRequest.OrderState}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OdooGetEdgesRequest.OrderItem> serializer() {
			return jsonObjectSerializer(OdooGetEdgesRequest.OrderItem.class, json -> {
				return new OdooGetEdgesRequest.OrderItem(//
						json.getString("field"), //
						json.getEnum("sortOrder", OdooGetEdgesRequest.SortOrder.class) //
				);
			}, obj -> JsonUtils.buildJsonObject() //
					.addProperty("field", obj.field()) //
					.addProperty("sortOrder", obj.sortOrder()) //
					.build());
		}

	}

	public record OrderState(List<OdooGetEdgesRequest.OrderItem> orderItems) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link OdooGetEdgesRequest.OrderState}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OdooGetEdgesRequest.OrderState> serializer() {
			return jsonSerializer(OdooGetEdgesRequest.OrderState.class, json -> {
				return new OdooGetEdgesRequest.OrderState(//
						json.getAsObject(OdooGetEdgesRequest.OrderItem.serializer().toListSerializer()) //
				);
			}, obj -> OdooGetEdgesRequest.OrderItem.serializer().toListSerializer().serialize(obj.orderItems()));
		}

	}

}