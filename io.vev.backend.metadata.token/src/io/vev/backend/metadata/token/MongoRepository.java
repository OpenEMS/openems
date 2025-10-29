package io.vev.backend.metadata.token;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

import io.openems.common.exceptions.OpenemsError;
import org.bson.Document;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * Minimal repository that showcases how the token metadata provider can interact
 * with MongoDB for storing user and edge relations.
 */
final class MongoRepository implements AutoCloseable {
	private final MongoClient client;
	private final MongoDatabase database;

	MongoRepository(String connectionUri, String databaseName) {
		Objects.requireNonNull(connectionUri, "connectionUri must not be null");
		Objects.requireNonNull(databaseName, "databaseName must not be null");

		this.client = MongoClients.create(connectionUri);
		this.database = this.client.getDatabase(databaseName);
	}

	MongoCollection<Document> getCollection(String tenantId, String collectionName) {
        return this.database.getCollection(tenantId + "." + collectionName);
    }

    MongoTenantRepository forTenant(String tenantId) {
        return new MongoTenantRepository(this, tenantId);
    }

    MongoTenantRepository forDefaultTenant() {
        return new MongoTenantRepository(this, "default");
    }

    @Override
    public void close() throws RuntimeException {
        this.client.close();
    }
}
