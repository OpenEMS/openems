package io.vev.backend.metadata.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.bson.Document;

import io.openems.backend.common.edge.jsonrpc.UpdateMetadataCache;
import io.openems.backend.common.metadata.Edge;

final class EdgeCache {

	private final MetadataToken parent;
	private final ConcurrentMap<String, VevEdge> edgesById = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, String> apikeyToEdgeId = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, String> setupPasswordToEdgeId = new ConcurrentHashMap<>();

	EdgeCache(MetadataToken parent) {
		this.parent = parent;
	}

	VevEdge upsert(Document doc) {
		var edgeId = VevEdge.buildEdgeId(doc);
		var edge = this.edgesById.compute(edgeId, (id, existing) -> {
			if (existing == null) {
				return new VevEdge(this.parent, doc);
			}
			var oldApikey = existing.getApikey();
			var oldSetupPassword = existing.getSetupPassword();
			existing.updateFromDocument(doc);
			this.removeLookupIfChanged(oldApikey, existing.getApikey(), id, this.apikeyToEdgeId);
			this.removeLookupIfChanged(oldSetupPassword, existing.getSetupPassword(), id, this.setupPasswordToEdgeId);
			return existing;
		});

		this.updateLookup(this.apikeyToEdgeId, edge.getApikey(), edge.getId());
		this.updateLookup(this.setupPasswordToEdgeId, edge.getSetupPassword(), edge.getId());
		return edge;
	}

	List<VevEdge> syncEdgesForTenant(String tenantId, List<Document> documents) {
		var updated = new ArrayList<VevEdge>();
		var expectedEdgeIds = new HashSet<String>();
		if (documents != null) {
			for (Document doc : documents) {
				var edge = this.upsert(doc);
				updated.add(edge);
				expectedEdgeIds.add(edge.getId());
			}
		}

		var prefix = tenantId + ":";
		var obsoleteEdgeIds = new ArrayList<String>();
		for (var edgeId : this.edgesById.keySet()) {
			if (edgeId.startsWith(prefix) && !expectedEdgeIds.contains(edgeId)) {
				obsoleteEdgeIds.add(edgeId);
			}
		}
		for (var obsoleteId : obsoleteEdgeIds) {
			this.removeEdge(obsoleteId);
		}

		return List.copyOf(updated);
	}

	void removeEdge(String edgeId) {
		var removed = this.edgesById.remove(edgeId);
		if (removed == null) {
			return;
		}
		this.removeLookup(removed.getApikey(), edgeId, this.apikeyToEdgeId);
		this.removeLookup(removed.getSetupPassword(), edgeId, this.setupPasswordToEdgeId);
	}

	void removeApikey(String apikey) {
		this.removeLookup(apikey, null, this.apikeyToEdgeId);
	}

	void removeSetupPassword(String setupPassword) {
		this.removeLookup(setupPassword, null, this.setupPasswordToEdgeId);
	}

	Collection<Edge> getOfflineEdges() {
		return Collections.unmodifiableCollection(
				this.edgesById.values().stream().filter(Edge::isOffline).collect(Collectors.toList()));
	}

	UpdateMetadataCache.Notification generateUpdateMetadataCacheNotification() {
		return new UpdateMetadataCache.Notification(Map.copyOf(this.apikeyToEdgeId));
	}

	Optional<VevEdge> getCachedEdge(String edgeId) {
		return Optional.ofNullable(this.edgesById.get(edgeId));
	}

	private void updateLookup(ConcurrentMap<String, String> map, String key, String edgeId) {
		if (key == null || key.isBlank()) {
			return;
		}
		map.put(key, edgeId);
	}

	private void removeLookupIfChanged(String oldKey, String newKey, String edgeId,
			ConcurrentMap<String, String> map) {
		if (oldKey != null && !oldKey.equals(newKey)) {
			this.removeLookup(oldKey, edgeId, map);
		}
	}

	private void removeLookup(String key, String edgeId, ConcurrentMap<String, String> map) {
		if (key == null || key.isBlank()) {
			return;
		}
		if (edgeId == null) {
			map.remove(key);
			return;
		}
		map.remove(key, edgeId);
	}
}
