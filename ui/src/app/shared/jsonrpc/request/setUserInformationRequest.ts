import { JsonrpcRequest } from "../base";

/**
 * <pre>
 * {
 *  "jsonrpc": "2.0",
 *  "id": UUID,
 *  "method": "setUserInformation",
 *  "params": {
 *      "user": {
 *          "firstname": string,
 *          "lastname": string,
 *          "email": string,
 *          "phone": string,
 *          "address": {
 *              "street": string,
 *              "zip": string,
 *              "city": string,
 *              "country": string
 *          },
 *          "company": {
 *              "name": string
 *          }
 *      }
 *  }
 * }
 * </pre>
 */
export class SetUserInformationRequest extends JsonrpcRequest {

    private static METHOD: string = "setUserInformation";

    public constructor(
        public readonly params: {
            user: {
                firstname: string,
                lastname: string,
                email: string,
                phone: string,
                address: {
                    street: string,
                    zip: string,
                    city: string,
                    country: string
                }
            }
        }
    ) {
        super(SetUserInformationRequest.METHOD, params);
    }

}