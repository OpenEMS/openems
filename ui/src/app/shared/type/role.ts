export enum Role {
    GUEST = "guest",
    OWNER = "owner",
    INSTALLER = "installer",
    ADMIN = "admin"
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
                console.warn("Role '" + name + "' not found.")
                return Role.GUEST;
        }
    }
}