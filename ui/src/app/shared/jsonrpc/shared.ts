import { TranslateService } from "@ngx-translate/core";
import { Theme } from "src/app/edge/history/shared";
import { environment } from "src/environments";

import { NavigationId, NavigationTree } from "../components/navigation/shared";
import { EdgeConfig } from "../shared";
import { Role } from "../type/role";
import { ArrayUtils } from "../utils/array/array.utils";
import { AuthenticateResponse } from "./response/authenticateResponse";

export type Edges = [{
    id: string,
    comment: string,
    producttype: string,
    version: string
    role: "admin" | "installer" | "owner" | "guest",
    isOnline: boolean,
    lastmessage: Date,
    firstSetupProtocol: Date
}];

export enum UserSettings {
    JSON_RPC_TEST = "jsonrpcTest",
    THEME = "theme",
    CAPACITOR_TEST = "capacitorTest",
    USE_NEW_UI = "useNewUI",
}

export class User {

    constructor(
        public id: string,
        public name: string,
        public globalRole: "admin" | "installer" | "owner" | "guest",
        public language: string | null,
        public hasMultipleEdges: boolean,
        public settings: Partial<{ [k in UserSettings]: number | boolean | string }>,
    ) { }

    /**
     * Converts the authenticate response user to a real user
     *
     * @param user the user
     * @returns the user if passed User is valid, else null
     */
    public static from(user: AuthenticateResponse["result"]["user"]): User | null {
        if (!user || !(ArrayUtils.containsAll({ strings: Object.keys(user), arr: User.getPropertyKeys() }))) {
            return null;
        }
        return new User(user.id, user.name, user.globalRole, user.language, user.hasMultipleEdges, user.settings ?? {});
    }

    /**
     * Gets the user properties
     *
     * @returns all keys
     */
    private static getPropertyKeys(): string[] {
        return Object.keys(new this("", "", "admin", "", false, {}));
    }

    /**
     * Gets the current theme from user settings
     *
     * @returns the theme if existing, else null
     */
    public getThemeFromSettings(): Theme | null {

        if (environment.backend === "OpenEMS Edge") {
            return localStorage.getItem("THEME") as Theme ?? null;
        }

        if ("theme" in this.settings) {
            return this.settings["theme"] as Theme;
        }

        return null;
    }

    /**
     * Gets the current theme from user settings
     *
     * @returns the theme if existing, else null
     */
    public getUseNewUIFromSettings(): boolean {

        if (UserSettings.USE_NEW_UI in this.settings) {
            return this.settings[UserSettings.USE_NEW_UI] as boolean;
        }

        return false;
    }

    public isAtLeast(role: Role) {
        return Role.isAtLeast(this.globalRole, role);
    }

    public getNavigationTree(navigationTree: NavigationTree, translate: TranslateService, components: { [id: string]: EdgeConfig.Component; }) {

        const showNewUI = navigationTree != null || this.getUseNewUIFromSettings();
        if (!showNewUI) {
            return;
        }
        navigationTree.setChild(NavigationId.LIVE, new NavigationTree(NavigationId.HISTORY, { baseString: "history" }, { name: "stats-chart-outline" }, translate.instant("GENERAL.HISTORY"), "label", [], null));
    }

};
