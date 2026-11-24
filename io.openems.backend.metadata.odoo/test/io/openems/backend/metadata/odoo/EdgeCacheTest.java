package io.openems.backend.metadata.odoo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.common.exceptions.OpenemsException;

public class EdgeCacheTest {

	private EdgeCache edgeCache;

	@BeforeEach
	public void setUp() {
		this.edgeCache = new EdgeCache(mock(MetadataOdoo.class));
	}

	private static ResultSet resultSet(String edgeId, String apikey, String comment) throws SQLException {
		final var rs = mock(ResultSet.class);
		when(rs.getInt(EdgeDevice.ID.index())).thenReturn(1);
		when(rs.getString(EdgeDevice.NAME.index())).thenReturn(edgeId);
		when(rs.getString(EdgeDevice.APIKEY.index())).thenReturn(apikey);
		when(rs.getString(EdgeDevice.COMMENT.index())).thenReturn(comment);
		return rs;
	}

	@Test
	public void testInvalidKeysReturnNull() {
		assertNull(this.edgeCache.getEdgeFromEdgeId(null));
		assertNull(this.edgeCache.getEdgeForApikey(null));
		assertNull(this.edgeCache.getEdgeFromEdgeId("unknown0"));
		assertNull(this.edgeCache.getEdgeForApikey("unknown-apikey"));
	}

	@Test
	public void testAddOrUpdate() throws SQLException, OpenemsException {
		final var edge0 = this.edgeCache.addOrUpdate(resultSet("edge0", "apikey0", "comment0"));
		final var edge1first = this.edgeCache.addOrUpdate(resultSet("edge1", "apikey1", "old"));
		final var edge1second = this.edgeCache.addOrUpdate(resultSet("edge1", "apikey1", "new"));

		assertSame(edge0, this.edgeCache.getEdgeFromEdgeId("edge0"));
		assertSame(edge0, this.edgeCache.getEdgeForApikey("apikey0"));
		assertEquals(Map.of("apikey0", "edge0", "apikey1", "edge1"),
				this.edgeCache.generateUpdateMetadataCacheNotification().getApikeysToEdgeIds());

		assertSame(edge1first, edge1second);
		assertEquals("new", this.edgeCache.getEdgeFromEdgeId("edge1").getComment());
		assertTrue(this.edgeCache.getAllEdges().contains(edge1first));
	}

	@Test
	public void testAddOrUpdateWithNullApikeyIsRejected() throws SQLException {
		final var rs = resultSet("edge2", null, "comment2");

		assertThrows(OpenemsException.class, () -> this.edgeCache.addOrUpdate(rs));
		assertNull(this.edgeCache.getEdgeFromEdgeId("edge2"));
		assertTrue(this.edgeCache.getAllEdges().isEmpty());
	}

}
