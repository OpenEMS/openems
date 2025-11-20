package io.vev.backend.metadata.token;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Minimal repository that showcases how the token metadata provider can interact
 * with MongoDB for storing user and edge relations.
 */
final class MongoTenantRepository {
    private final MongoRepository parent;
    private final String tenantId;
    private static final String FIELD_ID = "_id";
    private static final String FIELD_TENANT_ID = "tenantID";
    private static final String USERS_COLLECTION = "users";
    private static final String EDGES_COLLECTION = "emsedges";
    private static final String DEFAULT_TENANT = "default";

    public MongoTenantRepository(MongoRepository parent, String tenantId) {
        Objects.requireNonNull(parent, "parent must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");

        this.parent = parent;
        this.tenantId = tenantId;
    }

    public Document getUser(String userId) {
        var collection = this.parent.getCollection(this.tenantId, USERS_COLLECTION);
        return collection.find(Filters.eq(FIELD_ID, new ObjectId(userId))).first();
    }

    public List<Document> getEdgeList() {
        var collection = this.parent.getCollection(DEFAULT_TENANT, EDGES_COLLECTION);
        return collection.find(Filters.eq(FIELD_TENANT_ID, new ObjectId(this.tenantId))).into(new ArrayList<>());
    }

    public Optional<Document> getEdgeById(String edgeId) {
        var collection = this.parent.getCollection(DEFAULT_TENANT, EDGES_COLLECTION);
        var edge = collection.find(Filters.and(
                    Filters.eq(FIELD_ID, new ObjectId(edgeId)),
                    Filters.eq(FIELD_TENANT_ID, new ObjectId(this.tenantId))
                ))
                .first();
        return Optional.ofNullable(edge);
    }

    public Optional<Document> getEdgeByApiKey(String apiKey) {
        var collection = this.parent.getCollection(DEFAULT_TENANT, EDGES_COLLECTION);
        var edge = collection.find(Filters.eq("apikey", apiKey))
                .first();
        return Optional.ofNullable(edge);
    }

    public Optional<Document> getEdgeBySetupPassword(String setupPassword) {
        var collection = this.parent.getCollection(DEFAULT_TENANT, EDGES_COLLECTION);
        var edge = collection.find(Filters.eq("setupPassword", setupPassword))
                .first();
        return Optional.ofNullable(edge);
    }
}
