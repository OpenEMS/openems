import { Pipe, PipeTransform } from "@angular/core";

import { Role } from "../../type/role";

@Pipe({
    name: "version",
    standalone: false,
})
export class VersionPipe implements PipeTransform {

    constructor() { }

    transform(version: string, role: Role | string): string {
        if (typeof role === "string") {
            role = Role.getRole(role);
        }
        switch (role) {
            case Role.OWNER:
            case Role.GUEST:
            case Role.INSTALLER: {
                const hyphenIndex = version.indexOf("-");
                if (hyphenIndex >= 0) {
                    return version.substring(0, hyphenIndex);
                }
                return version;
            }
            case Role.ADMIN:
                return version;
        }
    }
}
