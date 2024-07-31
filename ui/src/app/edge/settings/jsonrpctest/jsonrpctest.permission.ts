// @ts-strict-ignore
import { Edge } from "src/app/shared/components/edge/edge";
import { User } from "src/app/shared/jsonrpc/shared";
import { Role } from "src/app/shared/type/role";

export namespace JsonrpcTestPermission {

    export function canSee(user: User, edge: Edge): boolean {
       return true;
    }

}
