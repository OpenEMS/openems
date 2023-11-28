import { JsonrpcRequest } from "../base";

/**
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "addEdgeToUser",
 *   "params": {
 *     "user": {
 *       "companyName": string,
 *       "firstname": string,
 *       "lastname": string,
 *       "street": string,
 *       "zip": string,
 *       "city": string,
 *       "country": string,
 *       "phone": string,
 *       "email": string,
 *       "password": string,
 *       "confirmPassword": string,
 *     },
 *     "oem" : string
 *   }
 * }
 * </pre>
 */
export class RegisterUserRequest extends JsonrpcRequest {

    private static METHOD: string = "registerUser";

    public constructor(
        public override readonly params: {
            user: {
                firstname: string,
                lastname: string,
                phone: string,
                email: string,
                password: string,
                confirmPassword: string,
                address: {
                    street: string,
                    zip: string,
                    city: string,
                    country: string
                },
                company?: {
                    name: string
                },
                role: string
            },
            oem: string
        },
    ) {
        super(RegisterUserRequest.METHOD, params);
    }

}
