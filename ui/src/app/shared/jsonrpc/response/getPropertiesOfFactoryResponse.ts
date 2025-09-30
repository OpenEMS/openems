import { EdgeConfig } from "../../components/edge/edgeconfig";
import { JsonrpcResponseSuccess } from "../base";

/**
 * Represents a JSON-RPC Response for a {@link GetPropertiesOfFactoryResponse}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "result": {
 *     "factory": EDGE_CONFIG.FACTORY,
 *     "properties": EDGE_CONFIG.FACTORY_PROPERTY[]
 *   }
 * }
 * </pre>
 */
export class GetPropertiesOfFactoryResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            factory: EDGE_CONFIG.FACTORY,
            properties: EDGE_CONFIG.FACTORY_PROPERTY[],
        },
    ) {
        super(id, result);
    }

}

