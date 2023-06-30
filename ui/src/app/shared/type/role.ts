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
        name = name.toLowerCase();
        switch (name) {
            case "admin":
                return Role.ADMIN;
            case "owner":
                return Role.OWNER;
            case "installer":
                return Role.INSTALLER;
            case "guest":
                return Role.GUEST;
            default:
                console.warn("Role [" + name + "] not found.");
                return Role.GUEST;
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
        if (typeof role1 === 'string') {
            role1 = Role.getRole(role1);
        }
        if (typeof role2 === 'string') {
            role2 = Role.getRole(role2);
        }
        return role1 <= role2;
    }

}

