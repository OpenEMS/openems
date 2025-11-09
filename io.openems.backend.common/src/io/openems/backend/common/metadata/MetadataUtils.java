package io.openems.backend.common.metadata;

import static io.openems.common.utils.StringUtils.containsWithNullCheck;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.session.Role;
import io.openems.common.utils.ComparatorUtils;

public class MetadataUtils {

	private MetadataUtils() {
	}

	/**
	 * Common implementation for
	 * {@link Metadata#getPageDevice(User, PaginationOptions)}.
	 * 
	 * @param <EDGE>            the type of the {@link Edge}
	 * @param user              the {@link User}
	 * @param edges             a Collection of {@link Edge}s
	 * @param paginationOptions the {@link PaginationOptions}
	 * @return the result, a list of {@link EdgeMetadata}
	 * @throws OpenemsNamedException on error
	 */
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
		var comparator = ComparatorUtils.<EDGE>empty();
		final var searchParams = paginationOptions.getSearchParams();
		if (searchParams != null) {
			if (searchParams.searchIsOnline()) {
				pagesStream = pagesStream.filter(edge -> edge.isOnline() == searchParams.isOnline());
			}
			if (searchParams.productTypes() != null && !searchParams.productTypes().isEmpty()) {
				pagesStream = pagesStream.filter(edge -> searchParams.productTypes().contains(edge.getProducttype()));
			}
			// TODO sum state filter

			final var orderState = searchParams.orderState();
			if (orderState != null) {
				for (var orderItem : orderState.orderItems()) {
					var c = switch (orderItem.field()) {
					case "id" -> Comparator.comparing(EDGE::getId);
					case "comment" -> Comparator.comparing(EDGE::getComment);
					case "sumState" -> Comparator.<EDGE, Level>comparing(o -> {
						return Optional.ofNullable(o.getSumState()).orElse(Level.OK);
					});
					default -> null;
					};
					if (c == null) {
						continue;
					}
					if (orderItem.sortOrder() == PaginationOptions.SortOrder.DESC) {
						c = c.reversed();
					}
					comparator = comparator.thenComparing(c);
				}
			} else {
				comparator = comparator.thenComparing(EDGE::getId);
			}
		}
		return pagesStream //
				.sorted(comparator) //
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
							Optional.ofNullable(myEdge.getSumState()).orElse(Level.OK));
				}).toList();
	}

}
