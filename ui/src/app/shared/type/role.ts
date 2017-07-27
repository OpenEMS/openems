export class Role {

    constructor(
        public readonly name: string,
        public readonly level: number
    ) { };

    public isHigherAs(role: Role | string): boolean {
        if (typeof role === 'string') {
            role = ROLES.getRole(role);
        }
        if (role) {
            return this.level >= role.level;
        } else {
            return false;
        }
    }
}

export const ROLES = {
    guest: new Role("guest", 0),
    owner: new Role("owner", 1),
    installer: new Role("installer", 2),
    admin: new Role("admin", 3),

    getRole(name: string): Role {
        if (name in ROLES) {
            return ROLES[name];
        }
    }
};