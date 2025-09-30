import { TranslateService } from "@ngx-translate/core";
import { Theme } from "src/app/edge/history/shared";
import { environment } from "src/environments";

import { NavigationId, NavigationTree } from "../components/navigation/shared";
import { EdgeConfig } from "../shared";
import { Role } from "../type/role";
import { ArrayUtils } from "../utils/array/ARRAY.UTILS";
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
        public language: string,
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
        if (!user || !(ARRAY_UTILS.CONTAINS_ALL_STRINGS(OBJECT.KEYS(user), USER.GET_PROPERTY_KEYS()))) {
            return null;
        }
        return new User(USER.ID, USER.NAME, USER.GLOBAL_ROLE, USER.LANGUAGE, USER.HAS_MULTIPLE_EDGES, USER.SETTINGS ?? {});
    }

    /**
     * Gets the user properties
     *
     * @returns all keys
     */
    private static getPropertyKeys(): string[] {
        return OBJECT.KEYS(new this("", "", "admin", "", false, {}));
    }

    /**
     * Gets the current theme from user settings
     *
     * @returns the theme if existing, else null
     */
    public getThemeFromSettings(): Theme | null {

        if (ENVIRONMENT.BACKEND === "OpenEMS Edge") {
            return LOCAL_STORAGE.GET_ITEM("THEME") as Theme ?? null;
        }

        if ("theme" in THIS.SETTINGS) {
            return THIS.SETTINGS["theme"] as Theme;
        }

        return null;
    }

    /**
     * Gets the current theme from user settings
     *
     * @returns the theme if existing, else null
     */
    public getUseNewUIFromSettings(): boolean {

        if (UserSettings.USE_NEW_UI in THIS.SETTINGS) {
            return THIS.SETTINGS[UserSettings.USE_NEW_UI] as boolean;
        }

        return false;
    }

    public isAtLeast(role: Role) {
        return ROLE.IS_AT_LEAST(THIS.GLOBAL_ROLE, role);
    }

    public getNavigationTree(navigationTree: NavigationTree, translate: TranslateService, components: { [id: string]: EDGE_CONFIG.COMPONENT; }) {

        const showNewUI = navigationTree != null || THIS.GET_USE_NEW_UIFROM_SETTINGS();
        if (!showNewUI) {
            return;
        }
        NAVIGATION_TREE.SET_CHILD(NAVIGATION_ID.LIVE, new NavigationTree(NAVIGATION_ID.HISTORY, "history", { name: "stats-chart-outline" }, TRANSLATE.INSTANT("GENERAL.HISTORY"), "label", [], null));
    }

};
