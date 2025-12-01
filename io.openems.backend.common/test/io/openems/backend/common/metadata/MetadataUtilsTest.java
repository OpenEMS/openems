package io.openems.backend.common.metadata;

import static io.openems.backend.common.metadata.MetadataUtils.getPageDevice;
import static io.openems.backend.common.test.DummyUser.DUMMY_ADMIN;
import static io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions.SortOrder.ASC;
import static io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions.SortOrder.DESC;
import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.openems.backend.common.test.DummyEdge;
import io.openems.backend.common.test.DummyMetadata;
import io.openems.common.channel.Level;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.jsonrpc.response.GetEdgesResponse;
import io.openems.common.session.Role;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.FunctionUtils;

public class MetadataUtilsTest {

	private static final Edge EDGE_1 = dummyEdge("edge1", "Edge #1", Level.INFO);
	private static final Edge EDGE_2 = dummyEdge("edge2", "Edge #2", Level.WARNING);
	private static final Edge EDGE_3 = dummyEdge("edge3", "Edge #3", Level.FAULT);
	private static final Edge EDGE_4 = dummyEdge("edge4", "Edge #4", Level.WARNING);
	private static final Edge EDGE_5 = dummyEdge("edge5", "Edge #5", Level.INFO);
	private static final Edge EDGE_6 = dummyEdge("edge6", "Edge #6", Level.OK);
	private static final Edge EDGE_7 = dummyEdge("edge7", "Edge #7", Level.OK);
	private static final Edge EDGE_8 = dummyEdge("edge8", "Edge #8", Level.WARNING);

	private static final GetEdgesResponse.EdgeMetadata EDGE_1_METADATA = toEdgeMetadata(EDGE_1);
	private static final GetEdgesResponse.EdgeMetadata EDGE_2_METADATA = toEdgeMetadata(EDGE_2);
	private static final GetEdgesResponse.EdgeMetadata EDGE_3_METADATA = toEdgeMetadata(EDGE_3);
	private static final GetEdgesResponse.EdgeMetadata EDGE_4_METADATA = toEdgeMetadata(EDGE_4);
	private static final GetEdgesResponse.EdgeMetadata EDGE_5_METADATA = toEdgeMetadata(EDGE_5);
	private static final GetEdgesResponse.EdgeMetadata EDGE_6_METADATA = toEdgeMetadata(EDGE_6);
	private static final GetEdgesResponse.EdgeMetadata EDGE_7_METADATA = toEdgeMetadata(EDGE_7);
	private static final GetEdgesResponse.EdgeMetadata EDGE_8_METADATA = toEdgeMetadata(EDGE_8);

	private static final List<Edge> SHUFFLED_EDGES = List.of(//
			EDGE_5, EDGE_4, EDGE_7, EDGE_6, //
			EDGE_3, EDGE_1, EDGE_8, EDGE_2 //
	);

	@Test
	public void testGetPageDeviceOrderIdAsc() throws Exception {
		final var edges = getPageDevice(DUMMY_ADMIN, SHUFFLED_EDGES, createRequest(//
				new GetEdgesRequest.PaginationOptions.OrderItem("id", ASC)));

		assertEquals(List.of(//
				EDGE_1_METADATA, EDGE_2_METADATA, EDGE_3_METADATA, EDGE_4_METADATA, //
				EDGE_5_METADATA, EDGE_6_METADATA, EDGE_7_METADATA, EDGE_8_METADATA //
		), edges);
	}

	@Test
	public void testGetPageDeviceOrderIdDesc() throws Exception {
		final var edges = getPageDevice(DUMMY_ADMIN, SHUFFLED_EDGES, createRequest(//
				new GetEdgesRequest.PaginationOptions.OrderItem("id", DESC)));

		assertEquals(List.of(//
				EDGE_8_METADATA, EDGE_7_METADATA, EDGE_6_METADATA, EDGE_5_METADATA, //
				EDGE_4_METADATA, EDGE_3_METADATA, EDGE_2_METADATA, EDGE_1_METADATA //
		), edges);
	}

	@Test
	public void testGetPageDeviceOrderCommentAsc() throws Exception {
		final var edges = getPageDevice(DUMMY_ADMIN, SHUFFLED_EDGES, createRequest(//
				new GetEdgesRequest.PaginationOptions.OrderItem("comment", ASC)));

		assertEquals(List.of(//
				EDGE_1_METADATA, EDGE_2_METADATA, EDGE_3_METADATA, EDGE_4_METADATA, //
				EDGE_5_METADATA, EDGE_6_METADATA, EDGE_7_METADATA, EDGE_8_METADATA //
		), edges);
	}

	@Test
	public void testGetPageDeviceOrderCommentDesc() throws Exception {
		final var edges = getPageDevice(DUMMY_ADMIN, SHUFFLED_EDGES, createRequest(//
				new GetEdgesRequest.PaginationOptions.OrderItem("comment", DESC)));

		assertEquals(List.of(//
				EDGE_8_METADATA, EDGE_7_METADATA, EDGE_6_METADATA, EDGE_5_METADATA, //
				EDGE_4_METADATA, EDGE_3_METADATA, EDGE_2_METADATA, EDGE_1_METADATA //
		), edges);
	}

	@Test
	public void testGetPageDeviceOrderSumStateAsc() throws Exception {
		final var edges = getPageDevice(DUMMY_ADMIN, SHUFFLED_EDGES, createRequest(//
				new GetEdgesRequest.PaginationOptions.OrderItem("sumState", ASC), //
				new GetEdgesRequest.PaginationOptions.OrderItem("id", ASC)));

		assertEquals(List.of(//
				EDGE_6_METADATA, EDGE_7_METADATA, EDGE_1_METADATA, EDGE_5_METADATA, //
				EDGE_2_METADATA, EDGE_4_METADATA, EDGE_8_METADATA, EDGE_3_METADATA //
		), edges);
	}

	@Test
	public void testGetPageDeviceOrderSumStateDesc() throws Exception {
		final var edges = getPageDevice(DUMMY_ADMIN, SHUFFLED_EDGES, createRequest(//
				new GetEdgesRequest.PaginationOptions.OrderItem("sumState", DESC), //
				new GetEdgesRequest.PaginationOptions.OrderItem("id", ASC)));

		assertEquals(List.of(//
				EDGE_3_METADATA, EDGE_2_METADATA, EDGE_4_METADATA, EDGE_8_METADATA, //
				EDGE_1_METADATA, EDGE_5_METADATA, EDGE_6_METADATA, EDGE_7_METADATA //
		), edges);
	}

	private static GetEdgesRequest.PaginationOptions createRequest(
			GetEdgesRequest.PaginationOptions.OrderItem... orderItems) {
		return new GetEdgesRequest.PaginationOptions(0, 10, null,
				new GetEdgesRequest.PaginationOptions.SearchParams(Collections.emptyList(), Collections.emptyList(),
						new GetEdgesRequest.PaginationOptions.OrderState(List.of(orderItems)), false, false));
	}

	private static Edge dummyEdge(String id, String comment, Level sumState) {
		final var edge = new DummyEdge(new DummyMetadata(FunctionUtils::doNothing), id, comment,
				SemanticVersion.ZERO.toString(), "", ZonedDateTime.now());
		edge.setSumState(sumState);
		return edge;
	}

	private static GetEdgesResponse.EdgeMetadata toEdgeMetadata(Edge edge) {
		return new GetEdgesResponse.EdgeMetadata(//
				edge.getId(), //
				edge.getComment(), //
				edge.getProducttype(), //
				edge.getVersion(), //
				Role.ADMIN, //
				edge.isOnline(), //
				edge.getLastmessage(), //
				null, // firstSetupProtocol
				edge.getSumState());
	}

}