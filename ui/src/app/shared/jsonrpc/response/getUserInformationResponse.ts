import { JsonrpcResponseSuccess } from "../base";

/**
 * Represents a JSON-RPC Response for {@link GetUserInformationRequest}.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
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
 *   }
 * }
 * </pre>
 */
export class GetUserInformationResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
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
                },
                company: {
                    name: string
                }
            }
        }
    ) {
        super(id, result);
    }

}