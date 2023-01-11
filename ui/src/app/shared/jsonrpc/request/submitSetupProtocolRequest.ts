import { TranslateService } from "@ngx-translate/core";
import { Category } from "src/app/edge/installation/shared/category";
import { Utils } from "../../shared";
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
        category: Category,
        name: string,
        value: string
    }[],
    oem: string
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
 *              category: Category,
 *              name: string,
 *              value: string
 *          }[],
 *          oem: string
 *      }
 *  }
 * </pre>
 */
export class SubmitSetupProtocolRequest extends JsonrpcRequest {

    static METHOD: string = "submitSetupProtocol";

    public static translateFrom(protocol: SetupProtocol, translate: TranslateService): SubmitSetupProtocolRequest {
        // protocol.items are type category in the protocol recieved and need to be translated before the request being sent.
        var items: {
            category: string,
            name: string,
            value: string
        }[] = protocol.items.map((element) => {
            return {
                category: Category.toTranslatedString(element.category, translate),
                name: element.name,
                value: element.value
            }
        })

        // 'Deep copy' to copy the object values from protocol recieved.
        // To avoid type issues from category to string.
        var protocolTranslated = Utils.deepCopy(protocol);
        protocolTranslated.items = items;

        return new SubmitSetupProtocolRequest({ protocol: protocolTranslated });
    }

    private constructor(
        public readonly params: {
            protocol: any
        }
    ) {
        super(SubmitSetupProtocolRequest.METHOD, params);
    }
}