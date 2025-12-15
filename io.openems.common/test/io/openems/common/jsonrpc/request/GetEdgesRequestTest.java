package io.openems.common.jsonrpc.request;

import static io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions.SortOrder.ASC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class GetEdgesRequestTest {

	@Test
	public void testFrom() throws Exception {
		final var request = new GenericJsonrpcRequest(GetEdgesRequest.METHOD, JsonUtils.buildJsonObject() //
				.addProperty("page", 3) //
				.addProperty("limit", 19) //
				.build());

		final var parsed = GetEdgesRequest.from(request);
		assertNotNull(parsed);
		assertEquals(3, parsed.getPaginationOptions().getPage());
		assertEquals(19, parsed.getPaginationOptions().getLimit());
		assertNull(parsed.getPaginationOptions().getQuery());
		assertNull(parsed.getPaginationOptions().getSearchParams());
	}

	@Test
	public void testFromSearchParamsNoOrderState() throws Exception {
		final var request = new GenericJsonrpcRequest(GetEdgesRequest.METHOD, JsonUtils.buildJsonObject() //
				.addProperty("page", 0) //
				.addProperty("limit", 20) //
				.add("searchParams", JsonUtils.buildJsonObject() //
						.addProperty("isOnline", true) //
						.build())
				.build());

		final var parsed = GetEdgesRequest.from(request);
		assertNotNull(parsed);
		assertNotNull(parsed.getPaginationOptions().getSearchParams());
		assertTrue(parsed.getPaginationOptions().getSearchParams().searchIsOnline());
		assertTrue(parsed.getPaginationOptions().getSearchParams().isOnline());
		assertNull(parsed.getPaginationOptions().getSearchParams().orderState());
	}

	@Test
	public void testFromSearchParamsOrderState() throws Exception {
		final var request = new GenericJsonrpcRequest(GetEdgesRequest.METHOD, JsonUtils.buildJsonObject() //
				.addProperty("page", 0) //
				.addProperty("limit", 20) //
				.add("searchParams", JsonUtils.buildJsonObject() //
						.add("orderState", JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonObject() //
										.addProperty("field", "id") //
										.addProperty("sortOrder", "ASC") //
										.build())
								.build())
						.build())
				.build());

		final var parsed = GetEdgesRequest.from(request);
		assertNotNull(parsed);
		assertNotNull(parsed.getPaginationOptions().getSearchParams());
		assertNotNull(parsed.getPaginationOptions().getSearchParams().orderState());
		assertEquals(List.of(new GetEdgesRequest.PaginationOptions.OrderItem("id", ASC)),
				parsed.getPaginationOptions().getSearchParams().orderState().orderItems());
	}

	@Test
	public void testToJsonObjectSearchParamsNoOrderState() throws Exception {
		final var rawJsonRequest = JsonUtils.buildJsonObject() //
				.addProperty("page", 0) //
				.addProperty("limit", 20) //
				.add("searchParams", JsonUtils.buildJsonObject() //
						.addProperty("isOnline", true) //
						.build())
				.build();

		final var request = GetEdgesRequest.from(new GenericJsonrpcRequest(GetEdgesRequest.METHOD, rawJsonRequest));
		final var json = request.getPaginationOptions().toJsonObject();
		assertEquals(rawJsonRequest, json);
	}

	@Test
	public void testToJsonObjectSearchParamsOrderState() throws Exception {
		final var rawJsonRequest = JsonUtils.buildJsonObject() //
				.addProperty("page", 0) //
				.addProperty("limit", 20) //
				.add("searchParams", JsonUtils.buildJsonObject() //
						.add("orderState", JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonObject() //
										.addProperty("field", "id") //
										.addProperty("sortOrder", "ASC") //
										.build())
								.build())
						.build())
				.build();

		final var request = GetEdgesRequest.from(new GenericJsonrpcRequest(GetEdgesRequest.METHOD, rawJsonRequest));
		final var json = request.getPaginationOptions().toJsonObject();
		assertEquals(rawJsonRequest, json);
	}

}