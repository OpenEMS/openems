package io.openems.common.jsonrpc.request;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * JSON-RPC Request for getting a edges with a {@link PaginationOptions}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getEdges",
 *   "params": {
 *     "edgeId": string
 *     "query": string,
 *     "page": int,
 *     "limit": int,
 *     "searchParams": {
 *       "productTypes?": string[], -- if omitted or empty searches for all
 *       "sumStates?": {@link Level}[], -- if omitted or empty searches for all
 *       "isOnline?": boolean -- if omitted searches for both
 *     }
 *   }
 * }
 * </pre>
 */
public class GetEdgesRequest extends JsonrpcRequest {

	public static class PaginationOptions {
		private static final int MAX_LIMIT = 100;
		private static final int DEFAULT_LIMIT = 20;

		private final int page;
		private final int limit;
		private final SearchParams searchParams;
		private final String query;

		public record SearchParams(//
				List<String> productTypes, //
				List<Level> sumStates, //
				boolean searchIsOnline, //
				boolean isOnline //
		) {

			/**
			 * Creates a {@link SearchParams} from a {@link JsonObject}.
			 * 
			 * @param obj the {@link JsonObject}
			 * @return the {@link SearchParams}
			 */
			public static SearchParams from(JsonObject obj) throws OpenemsNamedException {
				final var isOnline = JsonUtils.getAsOptionalBoolean(obj, "isOnline");
				return new SearchParams(//
						exceptionalParsing(JsonUtils.getAsOptionalJsonArray(obj, "producttype").orElse(null),
								JsonUtils::getAsString), //
						exceptionalParsing(JsonUtils.getAsOptionalJsonArray(obj, "sumState").orElse(null), t -> {
							return Level.valueOf(JsonUtils.getAsString(t).toUpperCase());
						}), isOnline.isPresent(), isOnline.orElse(false));
			}

			/**
			 * Creates a {@link JsonElement} out of this {@link SearchParams}.
			 * 
			 * @return the created {@link JsonElement}
			 */
			public JsonElement toJson() {
				final var result = JsonUtils.buildJsonObject() //
						.onlyIf(!this.productTypes().isEmpty(), b -> b //
								.add("producttype", this.productTypes().stream() //
										.map(JsonPrimitive::new) //
										.collect(JsonUtils.toJsonArray()))) //
						.onlyIf(!this.sumStates().isEmpty(), b -> b //
								.add("sumState", this.sumStates().stream() //
										.map(Enum::name) //
										.map(JsonPrimitive::new) //
										.collect(JsonUtils.toJsonArray()))) //
						.onlyIf(this.searchIsOnline(), b -> b.addProperty("isOnline", this.isOnline())) //
						.build();
				if (result.isEmpty()) {
					return null;
				}
				return result;
			}

		}

		/**
		 * Creates a {@link PaginationOptions} from a {@link JsonObject}.
		 *
		 * @param params the {@link JsonObject}
		 * @return the {@link PaginationOptions}
		 * @throws OpenemsNamedException on parse error
		 */
		public static PaginationOptions from(JsonObject params) throws OpenemsNamedException {
			var page = JsonUtils.getAsInt(params, "page");
			var limit = JsonUtils.getAsOptionalInt(params, "limit").orElse(DEFAULT_LIMIT);
			if (limit <= 0 || limit > MAX_LIMIT) {
				throw new OpenemsException("Limit is not in range [1:" + MAX_LIMIT + "]");
			}

			final var searchParams = exceptionalParsing(JsonUtils.getAsOptionalJsonObject(params, "searchParams") //
					.orElse(null), SearchParams::from);
			var query = JsonUtils.getAsOptionalString(params, "query").orElse(null);
			return new PaginationOptions(page, limit, query, searchParams);
		}

		private PaginationOptions(int page, int limit, String query, SearchParams searchParams) {
			this.page = page;
			this.limit = limit;
			this.query = query;
			this.searchParams = searchParams;
		}

		public int getPage() {
			return this.page;
		}

		public int getLimit() {
			return this.limit;
		}

		public String getQuery() {
			return this.query;
		}

		public SearchParams getSearchParams() {
			return this.searchParams;
		}

		/**
		 * Returns {@link JsonObject} from current {@link PaginationOptions}.
		 * 
		 * @return {@link JsonObject} from {@link PaginationOptions}
		 */
		public JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("page", this.page) //
					.addProperty("limit", this.limit) //
					.addPropertyIfNotNull("query", this.query) //
					.onlyIf(this.searchParams != null, b -> {
						final var json = this.searchParams.toJson();
						if (json != null) {
							b.add("searchParams", json);
						}
					}) //
					.build();
		}
	}

	/**
	 * Creates a {@link GetEdgesRequest} from a {@link JsonrpcRequest}.
	 *
	 * @param request the {@link JsonrpcRequest}
	 * @return the {@link GetEdgesRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static GetEdgesRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();
		var paginationOptions = PaginationOptions.from(params);
		return new GetEdgesRequest(request, paginationOptions);
	}

	public static final String METHOD = "getEdges";

	private final PaginationOptions paginationOptions;

	protected GetEdgesRequest(JsonrpcRequest request, PaginationOptions paginationOptions) {
		super(request, METHOD);
		this.paginationOptions = paginationOptions;
	}

	public PaginationOptions getPaginationOptions() {
		return this.paginationOptions;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("pagination", this.paginationOptions.toJsonObject()) //
				.build();
	}

	private static <T> List<T> exceptionalParsing(//
			final JsonArray array, //
			final ThrowingFunction<JsonElement, T, OpenemsNamedException> mapper //
	) throws OpenemsNamedException {
		if (array == null) {
			return emptyList();
		}
		final var result = new ArrayList<T>(array.size());
		for (var element : array) {
			result.add(mapper.apply(element));
		}
		return result;
	}

	private static <T> T exceptionalParsing(//
			final JsonObject object, //
			final ThrowingFunction<JsonObject, T, OpenemsNamedException> mapper //
	) throws OpenemsNamedException {
		if (object == null) {
			return null;
		}
		return mapper.apply(object);
	}

}
