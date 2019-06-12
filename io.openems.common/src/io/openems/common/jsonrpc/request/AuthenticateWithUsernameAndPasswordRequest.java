package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonKeys;
import io.openems.common.utils.JsonUtils;

import java.util.UUID;

/**
 * Represents a JSON-RPC Request to authenticate with a Password.
 * <p>
 * This is used by UI to login with password-only at the Edge.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "authenticateWithUserNameAndPassword",
 *   "params": {
 *     "username" : string,
 *     "password": string
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithUsernameAndPasswordRequest extends JsonrpcRequest {

    public final static String METHOD = "authenticateWithUserNameAndPassword";

    public static AuthenticateWithUsernameAndPasswordRequest from(JsonrpcRequest r) throws OpenemsNamedException {
        JsonObject p = r.getParams();
        String password = JsonUtils.getAsString(p, JsonKeys.PASSWORD.value());
        String username = JsonUtils.getAsString(p, JsonKeys.USER_NAME.value());
        return new AuthenticateWithUsernameAndPasswordRequest(r.getId(), username, password);
    }

    public static AuthenticateWithUsernameAndPasswordRequest from(JsonObject j) throws OpenemsNamedException {
        return from(GenericJsonrpcRequest.from(j));
    }

    private final String username;
    private final String password;

    public AuthenticateWithUsernameAndPasswordRequest(UUID id, String username, String password) {
        super(id, METHOD);
        this.password = password;
        this.username = username;
    }

    public AuthenticateWithUsernameAndPasswordRequest(String username, String password) {
        this(UUID.randomUUID(), username, password);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public JsonObject getParams() {
        return JsonUtils.buildJsonObject() //
                .addProperty(JsonKeys.PASSWORD.value(), this.password) //
                .addProperty(JsonKeys.USER_NAME.value(), this.username)
                .build();
    }
}
