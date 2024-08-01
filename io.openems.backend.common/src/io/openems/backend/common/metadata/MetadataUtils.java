package io.openems.backend.common.metadata;

import static io.openems.common.utils.StringUtils.containsWithNullCheck;

import java.util.Collection;
import java.util.List;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.session.Role;

public class MetadataUtils {

	private MetadataUtils() {
	}

	public static <EDGE extends Edge> List<EdgeMetadata> getPageDevice(User user, Collection<EDGE> edges,
			PaginationOptions paginationOptions) throws OpenemsNamedException {
		var pagesStream = edges.stream();
		final var query = paginationOptions.getQuery();
		if (query != null) {
			pagesStream = pagesStream.filter(//
					edge -> containsWithNullCheck(edge.getId(), query) //
							|| containsWithNullCheck(edge.getComment(), query) //
							|| containsWithNullCheck(edge.getProducttype(), query) //
			);
		}
		final var searchParams = paginationOptions.getSearchParams();
		if (searchParams != null) {
			if (searchParams.searchIsOnline()) {
				pagesStream = pagesStream.filter(edge -> edge.isOnline() == searchParams.isOnline());
			}
			if (searchParams.productTypes() != null && !searchParams.productTypes().isEmpty()) {
				pagesStream = pagesStream.filter(edge -> searchParams.productTypes().contains(edge.getProducttype()));
			}
			// TODO sum state filter
		}

		return pagesStream //
				.sorted((s1, s2) -> s1.getId().compareTo(s2.getId())) //
				.skip(paginationOptions.getPage() * paginationOptions.getLimit()) //
				.limit(paginationOptions.getLimit()) //
				.peek(t -> user.setRole(t.getId(), Role.ADMIN)) //
				.map(myEdge -> {
					return new EdgeMetadata(//
							myEdge.getId(), //
							myEdge.getComment(), //
							myEdge.getProducttype(), //
							myEdge.getVersion(), //
							Role.ADMIN, //
							myEdge.isOnline(), //
							myEdge.getLastmessage(), //
							null, // firstSetupProtocol
							Level.OK);
				}).toList();
	}

}
