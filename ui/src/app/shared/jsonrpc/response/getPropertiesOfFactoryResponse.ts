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
 *     "factory": EdgeConfig.Factory,
 *     "properties": EdgeConfig.FactoryProperty[]
 *   }
 * }
 * </pre>
 */
export class GetPropertiesOfFactoryResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            factory: EdgeConfig.Factory,
            properties: EdgeConfig.FactoryProperty[],
        },
    ) {
        super(id, result);
    }

}

