import { format } from "date-fns";
import { Edge } from "../../shared";
import { AssertionUtils } from "../../utils/assertions/assertions.utils";
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

export class GetSetupProtocolCoreInfoResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            setupProtocols: {
                setupProtocolId: number,
                createDate: Date,
                setupProtocolType: Type,
            }[],
        }
    ) {
        super(id, result);
    }
}

export enum Type {
    CAPACITY_EXTENSION = "CAPACITY_EXTENSION",
    SETUP_PROTOCOL = "SETUP_PROTOCOL",
    EMS_EXCHANGE = "EMS_EXCHANGE",
}

export function getFileName(type: Type, createDate: Date, edge: Edge | null) {

    AssertionUtils.assertIsDefined(edge);

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

    return prefix() + `-${edge.id}-${format(createDate, "dd.MM.yyyy")}.pdf`;
}
