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
            role = ROLE.GET_ROLE(role);
        }
        switch (role) {
            case ROLE.OWNER:
            case ROLE.GUEST:
            case ROLE.INSTALLER:
                if (VERSION.INCLUDES("-")) {
                    return VERSION.REPLACE(/^(.*)-.*$/, "$1");
                }
                return version;
            case ROLE.ADMIN:
                return version;
        }
    }
}
