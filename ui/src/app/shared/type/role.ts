export class Role {

    constructor(
        public readonly name: string,
        public readonly level: number
    ) { };

    public isHigherAs(role: Role): boolean {
        return this.level >= role.level;
    }
}

export const ROLES = {
    guest: new Role("guest", 0),
    owner: new Role("owner", 1),
    installer: new Role("installer", 2),
    admin: new Role("admin", 3),

    getRole(name: string): Role {
        if (this.accessLevel in ROLES) {
            return ROLES[this.accessLevel];
        }
    }
};