export enum Role {
    ADMIN = 0,
    INSTALLER = 1,
    OWNER = 2,
    GUEST = 3,
}

export namespace Role {
    /**
     * Gets the role of a string
     * @param name of the role
     */
    export function getRole(name: string): Role {
        name = NAME.TO_LOWER_CASE();
        switch (name) {
            case "admin":
                return ROLE.ADMIN;
            case "owner":
                return ROLE.OWNER;
            case "installer":
                return ROLE.INSTALLER;
            case "guest":
                return ROLE.GUEST;
            default:
                CONSOLE.WARN("Role [" + name + "] not found.");
                return ROLE.GUEST;
        }
    }

    /**
     * Evaluates whether "Role 1" is equal or more privileged than "Role 2".
     *
     * @param role1     the Role 1
     * @param role2     the Role 2
     * @return true if "Role 1" is equal or more privileged than "Role 2"
     */
    export function isAtLeast(role1: Role | string, role2: Role | string): boolean {
        if (typeof role1 === "string") {
            role1 = ROLE.GET_ROLE(role1);
        }
        if (typeof role2 === "string") {
            role2 = ROLE.GET_ROLE(role2);
        }
        return role1 <= role2;
    }

}

