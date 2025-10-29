package io.vev.backend.metadata.token;

import java.util.*;
import java.util.stream.Collectors;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import com.google.gson.JsonObject;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.AbstractUser;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.Tuple;
import org.bson.Document;

import javax.print.Doc;

import static io.openems.common.session.Role.ADMIN;
import static io.openems.common.session.Role.GUEST;

/**
 * Container for the claims embedded in the VEV user JWT.
 */
final class VevUser extends User {
    private final String tenantId;
    private final Document doc;
    private final Collection<String> edgeIds;

	private static Language parseVevLanguage(String language) {
		if (language == null || language.isBlank()) {
			return Language.DEFAULT;
		}

		var normalized = language.trim().replace('-', '_');
		var separatorIndex = normalized.indexOf('_');
		if (separatorIndex > 0) {
			normalized = normalized.substring(0, separatorIndex);
		}

		try {
			return Language.from(normalized);
		} catch (OpenemsException e) {
			return Language.DEFAULT;
		}
	}

    public Collection<String> getEdgeIds() {
        return edgeIds;
    }

	private static Role parseVevRole(String role) {
        return switch (role) {
            case "A" -> ADMIN;
            default -> GUEST;
        };
	}

    public static VevUser fromFullId(MongoRepository mongoRepository, Tuple<String, String> fullId) {
        var tenantId = fullId.a();
        var objectId = fullId.b();
        var tenantRepo = new MongoTenantRepository(mongoRepository, tenantId);
        var userDoc = tenantRepo.getUser(objectId);
        if (userDoc == null) {
            throw new NoSuchElementException("User not found: " + fullId);
        }
        var edgesDocs = tenantRepo.getEdgeList();
        return new VevUser(tenantId, userDoc, edgesDocs, Optional.empty());
    }

	public VevUser(String tenantId, Document doc, List<Document> edgesDoc, Optional<String> tokenStr) {
        super(
          tenantId + ":" + doc.getObjectId("_id").toHexString(),
          doc.getString("firstName") + " " + doc.getString("lastName"),
            tokenStr.orElse(""),
            parseVevLanguage(doc.getString("language")),
            parseVevRole(doc.getString("role")),
            edgesDoc.size() > 1,
            new JsonObject()
        );
        this.tenantId = tenantId;
        this.doc = doc;
        this.edgeIds = edgesDoc.stream()
                .map(edgeDoc -> edgeDoc.getObjectId("_id").toHexString())
                .collect(Collectors.toList());
    }


    @Override
    public boolean hasMultipleEdges() {
        return this.edgeIds.size() > 1;
    }

    public String getTenantId() {
        return this.tenantId;
    }
}
