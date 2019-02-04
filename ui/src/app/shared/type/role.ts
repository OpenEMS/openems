export type Role = "guest" | "owner" | "installer" | "admin";
// TODO Works with typescript 2.4:
// export enum Role {
//     GUEST = "guest",
//     OWNER = "owner",
//     INSTALLER = "installer",
//     ADMIN = "admin"
// }

export namespace Role {
    /**
     * Gets the role of a string
     * @param name of the role
     */
    export function getRole(name: string): Role {
        name = name.toLowerCase();
        switch (name) {
            case "admin":
                return "admin";
            // return Role.ADMIN;
            case "owner":
                return "owner";
            // return Role.OWNER;
            case "installer":
                return "installer";
            // return Role.INSTALLER;
            case "guest":
                return "guest";
            // return Role.GUEST;
            default:
                console.warn("Role '" + name + "' not found.")
                return "guest";
            // return Role.GUEST;
        }
    }
}