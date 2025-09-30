import { User } from "src/app/shared/jsonrpc/shared";
import { Edge } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

const keyTestUsers: string[] = [
];

function isTestUser(user: User): boolean {
    return KEY_TEST_USERS.SOME((id) => {
        return USER.ID === id;
    });
}

export function canEnterKey(edge: Edge, user: User): boolean {
    if (isTestUser(user)) {
        return true;
    }
    if (EDGE.ROLE_IS_AT_LEAST(ROLE.OWNER)) {
        return true;
    }
    return false;
}

export function hasPredefinedKey(edge: Edge, user: User): boolean {
    if (isTestUser(user)) {
        return false;
    }
    return EDGE.ROLE_IS_AT_LEAST(ROLE.ADMIN);
}

export function hasKeyModel(edge: Edge): boolean {
    return EDGE.IS_VERSION_AT_LEAST("2023.1.2");
}

/**
 * Checks if the edge has a version that has the UpdateAppConfig jsonrpc request.
 *
 * @param edge the edge to be checked.
 * @returns true if the version is atleast '2025.1.2', false otherwise
 */
export function hasUpdateAppVersion(edge: Edge): boolean {
    return EDGE.IS_VERSION_AT_LEAST("2025.1.2");
}
