import { JsonrpcRequest } from "../jsonrpc/base";

export class UnimplementedError<T extends JsonrpcRequest> extends Error {
    constructor(request: T, msg?: string) {
        super(request.constructor.name);
        if (msg) {
            this.message = this.message + " " + msg;
        }
    }
}

export class UnimplementedInEdgeError<T extends JsonrpcRequest> extends UnimplementedError<T> {
    constructor(request: T) {
        super(request, "not available with edge as backend");
    }
}

export class EdgeNotSetError extends Error {
    constructor() {
        super("edge not set");
    }
}

/**
 * Edge's non-specific fallback error code (see OpenemsError.GENERIC), used for every error not raising a dedicated
 * code.
 */
export class GenericError extends Error {
    public static readonly id: number = 1;
}

export class AuthenticationFailedError extends Error {
    public static readonly id: number = 1003;
}

/**
 * Reported when the App instance to update/remove could no longer be found on Edge (see
 * OpenemsError.EDGE_APP_INSTANCE_NOT_FOUND).
 */
export class EdgeAppInstanceNotFoundError extends Error {
    public static readonly id: number = 2006;
}

/**
 * Reported when Edge failed to update the components of an already-installed App (see
 * OpenemsError.EDGE_APP_COMPONENTS_UPDATE_FAILED).
 */
export class EdgeAppComponentsUpdateFailedError extends Error {
    public static readonly id: number = 2007;
}

/**
 * Reported when the validation failed because one or more required Apps are not
 * installed on the Edge (see OpenemsError.EDGE_APP_VALIDATION_APPS_NOT_INSTALLED).
 */
export class EdgeAppValidationAppsNotInstalledError extends Error {
    public static readonly id: number = 2100;
}

/**
 * Reported when the validation failed because an App may only be installed a single
 * time in total (see OpenemsError.EDGE_APP_VALIDATION_CHECK_CARDINALITY_SINGLE).
 */
export class EdgeAppValidationCheckCardinalitySingleError extends Error {
    public static readonly id: number = 2101;
}

/**
 * Reported when the validation failed because an App may only be installed a single
 * time within its category
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_CARDINALITY_SINGLE_IN_CATEGORY).
 */
export class EdgeAppValidationCheckCardinalitySingleInCategoryError extends Error {
    public static readonly id: number = 2102;
}

/**
 * Reported when the validation failed because the App requires a "Home" App/setup
 * to be present (see OpenemsError.EDGE_APP_VALIDATION_CHECK_HOME).
 */
export class EdgeAppValidationCheckHomeError extends Error {
    public static readonly id: number = 2103;
}

/**
 * Reported when the validation failed because the App requires that no "Home"
 * App/setup is present (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_HOME_INVERTED).
 */
export class EdgeAppValidationCheckHomeInvertedError extends Error {
    public static readonly id: number = 2104;
}

/**
 * Reported when the validation failed because the App requires a "Commercial 50 Gen3"
 * setup to be present (see OpenemsError.EDGE_APP_VALIDATION_CHECK_COMMERCIAL50GEN3).
 */
export class EdgeAppValidationCheckCommercial50Gen3Error extends Error {
    public static readonly id: number = 2105;
}

/**
 * Reported when the validation failed because the App requires that no
 * "Commercial 50 Gen3" setup is present (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_COMMERCIAL50GEN3_INVERTED).
 */
export class EdgeAppValidationCheckCommercial50Gen3InvertedError extends Error {
    public static readonly id: number = 2106;
}

/**
 * Reported when the validation failed because the App requires a "Commercial 92"
 * setup to be present (see OpenemsError.EDGE_APP_VALIDATION_CHECK_COMMERCIAL92).
 */
export class EdgeAppValidationCheckCommercial92Error extends Error {
    public static readonly id: number = 2107;
}

/**
 * Reported when the validation failed because the App requires that no
 * "Commercial 92" setup is present (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_COMMERCIAL92_INVERTED).
 */
export class EdgeAppValidationCheckCommercial92InvertedError extends Error {
    public static readonly id: number = 2108;
}

/**
 * Reported when the validation failed because the App requires a "Commercial 92
 * Master" setup to be present
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_COMMERCIAL92_MASTER).
 */
export class EdgeAppValidationCheckCommercial92MasterError extends Error {
    public static readonly id: number = 2109;
}

/**
 * Reported when the validation failed because the App requires that no
 * "Commercial 92 Master" setup is present (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_COMMERCIAL92_MASTER_INVERTED).
 */
export class EdgeAppValidationCheckCommercial92MasterInvertedError extends Error {
    public static readonly id: number = 2110;
}

/**
 * Reported when the validation failed because the App requires a "Commercial 100"
 * setup to be present (see OpenemsError.EDGE_APP_VALIDATION_CHECK_COMMERCIAL100).
 */
export class EdgeAppValidationCheckCommercial100Error extends Error {
    public static readonly id: number = 2111;
}

/**
 * Reported when the validation failed because the App requires that no
 * "Commercial 100" setup is present (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_COMMERCIAL100_INVERTED).
 */
export class EdgeAppValidationCheckCommercial100InvertedError extends Error {
    public static readonly id: number = 2112;
}

/**
 * Reported when the validation failed because the App requires an "Industrial L"
 * setup to be present (see OpenemsError.EDGE_APP_VALIDATION_CHECK_INDUSTRIAL_L).
 */
export class EdgeAppValidationCheckIndustrialLError extends Error {
    public static readonly id: number = 2113;
}

/**
 * Reported when the validation failed because the App requires that no
 * "Industrial L" setup is present (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_INDUSTRIAL_L_INVERTED).
 */
export class EdgeAppValidationCheckIndustrialLInvertedError extends Error {
    public static readonly id: number = 2114;
}

/**
 * Reported when the validation failed because the App requires an "Industrial XL"
 * setup to be present (see OpenemsError.EDGE_APP_VALIDATION_CHECK_INDUSTRIAL_XL).
 */
export class EdgeAppValidationCheckIndustrialXLError extends Error {
    public static readonly id: number = 2115;
}

/**
 * Reported when the validation failed because the App requires that no
 * "Industrial XL" setup is present (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_INDUSTRIAL_XL_INVERTED).
 */
export class EdgeAppValidationCheckIndustrialXLInvertedError extends Error {
    public static readonly id: number = 2116;
}

/**
 * Reported when the validation failed because the App requires an "Industrial"
 * setup to be present (see OpenemsError.EDGE_APP_VALIDATION_CHECK_INDUSTRIAL).
 */
export class EdgeAppValidationCheckIndustrialError extends Error {
    public static readonly id: number = 2117;
}

/**
 * Reported when the validation failed because the App requires that no
 * "Industrial" setup is present (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_INDUSTRIAL_INVERTED).
 */
export class EdgeAppValidationCheckIndustrialInvertedError extends Error {
    public static readonly id: number = 2118;
}

/**
 * Reported when the validation failed because a configured host could not be
 * reached (see OpenemsError.EDGE_APP_VALIDATION_CHECK_HOST_NOT_REACHABLE).
 */
export class EdgeAppValidationCheckHostNotReachableError extends Error {
    public static readonly id: number = 2119;
}

/**
 * Reported when the validation failed because a configured host has an invalid or
 * unexpected IP address (see OpenemsError.EDGE_APP_VALIDATION_CHECK_HOST_WRONG_IP).
 */
export class EdgeAppValidationCheckHostWrongIpError extends Error {
    public static readonly id: number = 2120;
}

/**
 * Reported when the validation failed because no Component of the required Factory-ID
 * is installed
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_NO_COMPONENT_INSTALLED_OF_FACTORY_ID).
 */
export class EdgeAppValidationCheckNoComponentInstalledOfFactoryIdError extends Error {
    public static readonly id: number = 2121;
}

/**
 * Reported when the validation failed because none of the combined ("OR") checks
 * passed (see OpenemsError.EDGE_APP_VALIDATION_CHECK_OR).
 */
export class EdgeAppValidationCheckOrError extends Error {
    public static readonly id: number = 2122;
}

/**
 * Reported when the validation failed because the required acceptance of 3rd party
 * access was not given
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_3RD_PARTY_ACCESS_ACCEPTED).
 */
export class EdgeAppValidationCheckThirdPartyAccessAcceptedError extends Error {
    public static readonly id: number = 2123;
}

/**
 * Reported when the validation failed because the required coordinates
 * are not set
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_COORDINATES_SET).
 */
export class EdgeAppValidationCheckCoordinatesSetError extends Error {
    public static readonly id: number = 2124;
}

/**
 * Reported when the validation failed because an EVSE is required to not be
 * installed, but is (see OpenemsError.EDGE_APP_VALIDATION_CHECK_EVSE_NOT_INSTALLED).
 */
export class EdgeAppValidationCheckEvseNotInstalledError extends Error {
    public static readonly id: number = 2125;
}

/**
 * Reported when the validation failed because an EVSE is required to be installed,
 * but is not (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_EVSE_NOT_INSTALLED_INVERTED).
 */
export class EdgeAppValidationCheckEvseNotInstalledInvertedError extends Error {
    public static readonly id: number = 2126;
}

/**
 * Reported when the validation failed because an EVCS is required to not be
 * installed, but is (see OpenemsError.EDGE_APP_VALIDATION_CHECK_EVCS_NOT_INSTALLED).
 */
export class EdgeAppValidationCheckEvcsNotInstalledError extends Error {
    public static readonly id: number = 2127;
}

/**
 * Reported when the validation failed because an EVCS is required to be installed,
 * but is not (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_EVCS_NOT_INSTALLED_INVERTED).
 */
export class EdgeAppValidationCheckEvcsNotInstalledInvertedError extends Error {
    public static readonly id: number = 2128;
}

/**
 * Reported when the validation failed because the App is not available/allowed for
 * the configured country (see OpenemsError.EDGE_APP_VALIDATION_CHECK_COUNTRY).
 */
export class EdgeAppValidationCheckCountryError extends Error {
    public static readonly id: number = 2129;
}

/**
 * Reported when the validation failed because the App requires "Energy Scheduler V2"
 * to be present (see OpenemsError.EDGE_APP_VALIDATION_CHECK_ENERGY_SCHEDULER_V2).
 *
 * @deprecated Not existing on Edge after 2026.11.0.
 */
export class EdgeAppValidationCheckEnergySchedulerV2Error extends Error {
    public static readonly id: number = 2130;
}

/**
 * Reported when the validation failed because the App requires that
 * "Energy Scheduler V2" is not present (inverted check)
 * (see OpenemsError.EDGE_APP_VALIDATION_CHECK_ENERGY_SCHEDULER_V2_INVERTED).
 *
 * @deprecated Not existing on Edge after 2026.11.0.
 */
export class EdgeAppValidationCheckEnergySchedulerV2InvertedError extends Error {
    public static readonly id: number = 2131;
}

/**
 * Reported when the validation failed because the App requires a certain number of relays to be present (see OpenemsError.EDGE_APP_VALIDATION_CHECK_RELAY_COUNT).
 */
export class EdgeAppValidationCheckRelayCountError extends Error {
    public static readonly id: number = 2132;
}

/**
 * Supplements {@link EdgeAppValidationCheckRelayCountError} with a link to a product providing additional relay
 * outputs (see OpenemsError.EDGE_APP_VALIDATION_CHECK_RELAY_COUNT_ADDITIONAL_RELAY).
 */
export class EdgeAppValidationCheckRelayCountAdditionalRelayError extends Error {
    public static readonly id: number = 2133;
}

export class DuplicateAuthenticationFailureException extends Error {
    constructor() {
        super("Duplicate Authentication Failure");
    }
}

export class WebsocketTimeoutError extends Error {
    constructor() {
        super("Websocket timeout");
    }
}
