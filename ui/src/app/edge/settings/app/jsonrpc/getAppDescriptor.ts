import { DomSanitizer, SafeUrl } from "@angular/platform-browser";
import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request for 'getAppDescriptor'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getAppDescriptor",
 *   "params": {
 *      "appId": string
 *   }
 * }
 * </pre>
 * 
 * <p>
 * Response:
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "websiteURL": "..."
 *   }
 * }
 * </pre>
 */
export namespace GetAppDescriptor {

    export const METHOD: string = "getAppDescriptor";

    export class Request extends JsonrpcRequest {

        public constructor(
            public readonly params: {
                appId: string
            }
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public readonly id: string,
            public readonly result: AppDescriptor
        ) {
            super(id, result);
        }
    }

    export interface AppDescriptor {
        websiteUrl: string,
        sanitizedWebsiteUrl: SafeUrl,
    }

    export function postprocess(appAssistant: AppDescriptor, sanitizer: DomSanitizer): AppDescriptor {
        if (appAssistant.websiteUrl) {
            appAssistant.sanitizedWebsiteUrl = sanitizer.bypassSecurityTrustResourceUrl(appAssistant.websiteUrl);
        }
        return appAssistant;
    }

}