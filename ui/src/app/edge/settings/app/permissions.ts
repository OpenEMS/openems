import { User } from "src/app/shared/jsonrpc/shared";
import { Edge } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";


export function canSeeAppCenter(edge: Edge): boolean {
    return edge.roleIsAtLeast(Role.ADMIN)
        && edge.isVersionAtLeast('2022.1.0');
}

export function canEnterKey(edge: Edge, user: User): boolean {
    return false
}

export function hasPredefinedKey(edge: Edge): boolean {
    return edge.roleIsAtLeast(Role.ADMIN);
}

export function getPredefinedKey(edge: Edge): string {
    // TODO this feature will be removed in the future when the keys got rolled out completely
    if (edge.roleIsAtLeast(Role.ADMIN)) {
        return "8fyk-Gma9-EUO3-j3gi";  // TODO set key before release
    }
    return "";
}

export function hasKeyModel(edge: Edge): boolean {
    // TODO change version before release
    return edge.isVersionAtLeast('2023.1.1')
}