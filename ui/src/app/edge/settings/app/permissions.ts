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

export function hasKeyModel(edge: Edge): boolean {
    return edge.isVersionAtLeast('2023.1.2')
}