import { Pipe, PipeTransform } from '@angular/core';

import { Role } from '../../type/role';

@Pipe({
    name: 'version'
})
export class VersionPipe implements PipeTransform {

    constructor() { }

    transform(version: string, role: Role | string): string {
        if (typeof role === 'string') {
            role = Role.getRole(role);
        }
        switch (role) {
            case Role.OWNER:
            case Role.GUEST:
            case Role.INSTALLER:
                if (version.includes("-")) {
                    return version.replace(/^(.*)-.*$/, '$1');
                }
                return version;
            case Role.ADMIN:
                return version;
        }
    }
}