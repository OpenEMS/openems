import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../jsonrpc/base";
import { User } from "../../jsonrpc/shared";

export class AuthenticateWithOAuthRequest extends JsonrpcRequest {
    public static METHOD: string = "authenticateWithOAuth";
    public constructor(
        public override readonly params: {
            payload: JsonrpcRequest,
        },
    ) {
        super(AuthenticateWithOAuthRequest.METHOD, params);
    }
}
export class AuthenticateWithOAuthResponse extends JsonrpcResponseSuccess {
    public constructor(
        public override readonly id: string,
        public override readonly result: {
            refreshToken: string,
            accessToken: string,
            user: User,
        },
    ) {
        super(id, result);
    }
}
export class AuthenticateWithOAuth2Response extends JsonrpcResponseSuccess {
    public constructor(
        public override readonly id: string,
        public override readonly result: {
            identifier: string, loginUrl: string, state: string,
        },
    ) {
        super(id, result);
    }
}

export class OAuthLogoutRequest extends JsonrpcRequest {
    public static METHOD: string = "logout";
    public constructor(
        public readonly oem: string,
        public readonly refreshToken: string | null,
    ) {
        super(OAuthLogoutRequest.METHOD, { oem, refreshToken });
    }
}

export class OAuthLogoutResponse extends JsonrpcResponseSuccess {
    public constructor(
        public override readonly id: string,
        public override readonly result: {
            success: boolean,
        },
    ) {
        super(id, result);
    }
}
