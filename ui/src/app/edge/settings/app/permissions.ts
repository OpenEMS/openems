import { User } from "src/app/shared/jsonrpc/shared";
import { Edge } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

const keyTestUsers: string[] = [
];

function isTestUser(user: User): boolean {
    return keyTestUsers.some((id) => {
        return user.id === id;
    });
}

export function canSeeAppCenter(edge: Edge): boolean {
    return edge.roleIsAtLeast(Role.ADMIN)
        && edge.isVersionAtLeast('2022.1.0')
        || edge.roleIsAtLeast(Role.OWNER)
        && edge.isVersionAtLeast('2023.3.6');
}

export function canEnterKey(edge: Edge, user: User): boolean {
    if (isTestUser(user)) {
        return true;
    }
    if (edge.roleIsAtLeast(Role.OWNER)) {
        return true;
    }
    return false;
}

export function hasPredefinedKey(edge: Edge, user: User): boolean {
    if (isTestUser(user)) {
        return false;
    }
    return edge.roleIsAtLeast(Role.ADMIN);
}

export function hasKeyModel(edge: Edge): boolean {
    return edge.isVersionAtLeast('2023.1.2');
}