// @ts-strict-ignore
import { Edge } from "src/app/shared/edge/edge";
import { User } from "src/app/shared/jsonrpc/shared";
import { Role } from "src/app/shared/type/role";

export namespace JsonrpcTestPermission {

    export function canSee(user: User, edge: Edge): boolean {
        if (!edge.isVersionAtLeast('2024.4.1')) {
            return false;
        }
        if (!edge.roleIsAtLeast(Role.ADMIN)) {
            return false;
        }
        return user.settings['jsonrpcTest'];
    }

}
