package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
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
		private final String query;

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
			var query = JsonUtils.getAsOptionalString(params, "query").orElse(null);
			return new PaginationOptions(page, limit, query);
		}

		private PaginationOptions(int page, int limit, String query) {
			this.page = page;
			this.limit = limit;
			this.query = query;
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

}
