import { format } from "date-fns";
import { Edge } from "../../shared";
import { AssertionUtils } from "../../utils/assertions/ASSERTIONS.UTILS";
import { JsonrpcResponseSuccess } from "../base";

/**
 * Wraps a JSON-RPC Response for a GetLatestSetupProtocolCoreInfoRequest.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "payload": Base64-String
 *   }
 * }
 * </pre>
 */
export class GetLatestSetupProtocolCoreInfoResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            setupProtocolId: number,
            createDate: Date,
            setupProtocolType: Type,
        },
    ) {
        super(id, result);
    }
}

export enum Type {
    CAPACITY_EXTENSION = "capacity-extension",
    SETUP_PROTOCOL = "setup-protocol",
    EMS_EXCHANGE = "ems-exchange",
}

export function getFileName(type: Type, createDate: Date, edge: Edge | null) {

    ASSERTION_UTILS.ASSERT_IS_DEFINED(edge);

    const prefix = () => {
        switch (type) {
            case Type.SETUP_PROTOCOL:
                return "IBN";
            case Type.CAPACITY_EXTENSION:
                return "Capacity_extension";
            case Type.EMS_EXCHANGE:
                return "Ems_exchange";
        }
    };

    return prefix() + `-${EDGE.ID}-${format(createDate, "DD.MM.YYYY")}.pdf`;
}
