package io.openems.backend.authentication.api.model.response;

public record InitiateConnectResponse(
        String identifier, //
        String state, //
        String loginUrl //
) {
}
