import { JsonrpcRequest } from "../base";

export type SetupProtocol = {
    edge: {
        id: string
    },
    installer: {
        firstname: string,
        lastname: string
    },
    customer: {
        firstname: string,
        lastname: string,
        email: string,
        phone: string,
        address: {
            street: string,
            city: string,
            zip: string,
            country: string
        },
        company?: {
            name: string
        }
    },
    location?: {
        firstname: string,
        lastname: string,
        email: string,
        phone: string,
        address: {
            street: string,
            city: string,
            zip: string,
            country: string
        },
        company?: {
            name: string

        }
    },
    lots?: {
        category: string,
        name: string,
        serialNumber: string
    }[],
    items?: {
        category: string,
        name: string,
        value: string
    }[]
};

/**
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "submitSetupProtocol",
 *   "params": {
 *      protocol: {
 *          edge: {
 *              id: string
 *          },
 *          customer: {
 *              firstname: string,
 *              lastname: string,
 *              email: string,
 *              phone: string,
 *              address: {
 *                  street: string,
 *                  city: string,
 *                  zip: string,
 *                  country: string
 *              },
 *              company?: {
 *                  name: string
 *              }
 *          },
 *          location?: {
 *              firstname: string,
 *              lastname: string,
 *              email: string,
 *              phone: string,
 *              address: {
 *                  street: string,
 *                  city: string,
 *                  zip: string,
 *                  country: string
 *              },
 *              company?: {
 *                  name: string
 *              }
 *          },
 *          lots?: {
 *              category: string,
 *              name: string,
 *              serialNumber: string
 *          }[],
 *          items?: {
 *              category: string,
 *              name: string,
 *              value: string
 *          }[]
 *      }
 *  }
 * </pre>
 */
export class SubmitSetupProtocolRequest extends JsonrpcRequest {

    static METHOD: string = "submitSetupProtocol";

    public constructor(
        public readonly params: {
            protocol: SetupProtocol
        }
    ) {
        super(SubmitSetupProtocolRequest.METHOD, params);
    }
}