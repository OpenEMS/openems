import { Role } from "src/app/shared/type/role";
import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";
import { Flag } from "./flag/flag";

/**
 * Represents a JSON-RPC Request for 'getApps'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getApps",
 *   "params": {}
 * }
 * </pre>
 *
 * <p>
 * Response:
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "apps": [{
 *       "appId": string,
 *       "category": {
 *          "name": string,
 *          "readableName": string
 *       },
 *       "name": string,
 *       "cardinality": string,
 *       "status": {
 *          "status": string,
 *          "errorCompatibleMessages": string[],
 *          "errorInstallableMessages": string[]
 *       },
 *       "permissions": {
 *          "canSee": Role,
 *          "canDelete": Role
 *       },
 *       "image: string (base64),
 *       "instanceIds": UUID[]
 *     }]
 *   }
 * }
 * </pre>
 */
export namespace GetApps {

    export const METHOD: string = "getApps";

    export class Request extends JsonrpcRequest {

        public constructor(
        ) {
            super(METHOD, {});
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                apps: App[]
            },
        ) {
            super(id, result);
        }
    }

    export interface App {
        categorys: Category[],
        cardinality: "SINGLE" | "SINGLE_IN_CATEGORY" | "MULTIPLE",
        appId: string,
        name: string,
        shortName?: string,
        image?: string,
        imageUrl?: string,
        permissions?: Permissions,
        status: Status,
        instanceIds: string[],
        flags: Flag[]
    }

    export interface Permissions {
        canSee: Role,
        canDelete: Role,
    }

    export interface Status {
        name: "INCOMPATIBLE" | "COMPATIBLE" | "INSTALLABLE",
        errorCompatibleMessages: string[],
        errorInstallableMessages: string[]
    }

    export interface Category {
        name: string,
        readableName: string
    }
}
