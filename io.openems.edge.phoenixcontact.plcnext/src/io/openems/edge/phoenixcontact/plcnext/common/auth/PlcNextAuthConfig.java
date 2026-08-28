package io.openems.edge.phoenixcontact.plcnext.common.auth;

import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextUrlStringHelper;

/**
 * Covering configuration to authorize REST-API access.
 */
public record PlcNextAuthConfig(String baseUrl, String pathAuthApi, String username, String password) {

    /**
     * Assembles URL of authentication endpoint.
     *
     * @return URL of authentication endpoint
     */
    public String authUrl() {
        return PlcNextUrlStringHelper.buildUrlString(this.baseUrl, this.pathAuthApi);
    }
}
