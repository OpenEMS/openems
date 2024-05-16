import { Edge } from "src/app/shared/edge/edge";
import { User } from "src/app/shared/jsonrpc/shared";

export function canSeeJsonrpcTest(user: User, edge: Edge): boolean {
    // TODO check for certain users
    return true;
}
