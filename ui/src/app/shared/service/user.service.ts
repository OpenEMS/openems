import { Directive, effect, signal, WritableSignal } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { Theme, Theme as UserTheme } from "src/app/edge/history/shared";
import { ThemePopoverComponent } from "src/app/user/theme-selection-popup/theme-selection-popover";
import { environment } from "src/environments";
import { NavigationService } from "../components/navigation/service/NAVIGATION.SERVICE";
import { UnimplementedInEdgeError } from "../ERRORS.TS/errors";
import { JsonrpcResponseSuccess } from "../jsonrpc/base";
import { JsonRpcUtils } from "../jsonrpc/jsonrpcutils";
import { UpdateUserSettingsRequest } from "../jsonrpc/request/updateUserSettingsRequest";
import { User } from "../jsonrpc/shared";
import { AssertionUtils } from "../utils/assertions/ASSERTIONS.UTILS";
import { Service } from "./service";

@Directive()
export class UserService {

    public static readonly DEFAULT_THEME: UserTheme = USER_THEME.LIGHT;
    public currentUser: WritableSignal<User | null> = signal(null);

    /** @deprecated determines if applying new ui or old*/
    public isNewNavigation: WritableSignal<boolean> = signal(false);

    constructor(
        private modalCtrl: ModalController,
        private service: Service,
    ) {

        // Prohibits switching colors on init
        THIS.UPDATE_THEME(LOCAL_STORAGE.GET_ITEM("THEME") as UserTheme);
        effect(() => {
            const user = THIS.CURRENT_USER();

            if (user != null) {
                THIS.SHOW_THEME_SELECTION(user);
                THIS.IS_NEW_NAVIGATION.SET(NAVIGATION_SERVICE.IS_NEW_NAVIGATION(user, THIS.SERVICE.CURRENT_EDGE()));
            }
        });
    }

    /**
     * Selects the new theme
     *
     * @param theme the new theme
     */
    public async selectTheme(theme: UserTheme): Promise<void> {

        const currentUser: User | null = THIS.CURRENT_USER();
        if (currentUser == null || !theme) {
            return;
        }

        CURRENT_USER.SETTINGS = { ...CURRENT_USER.SETTINGS, theme: theme };
        THIS.FINALIZE_THEME_SELECTION(theme);
    }

    public getValidBrowserTheme(userTheme: UserTheme | null): UserTheme {

        const theme = userTheme === USER_THEME.SYSTEM
            ? WINDOW.MATCH_MEDIA("(prefers-color-scheme: dark)").matches
                ? USER_THEME.DARK
                : USER_THEME.LIGHT
            : userTheme;

        return theme ?? UserService.DEFAULT_THEME;
    }

    /**
     * Updates the userSettings
     *
     * @param key the key to update
     * @param value the value for given key
     */
    public async updateUserSettingsWithProperty(key: string, value: boolean | string | number) {
        const user = THIS.CURRENT_USER();
        ASSERTION_UTILS.ASSERT_IS_DEFINED(user);
        const updatedSettings = { ...USER.SETTINGS, [key]: value };
        const [err, _result] = await THIS.UPDATE_USER_SETTINGS(updatedSettings);
        if (err !== null) {
            throw err;
        }

        THIS.CURRENT_USER.SET(new User(USER.ID, USER.NAME, USER.GLOBAL_ROLE, USER.LANGUAGE, USER.HAS_MULTIPLE_EDGES, updatedSettings));
    }

    /**
     * Shows the theme selection popover, only for new customers
     *
     * @returns
     */
    private showThemeSelection(user: User): void {
        const theme: UserTheme | null = THIS.GET_THEME(user);

        if (theme != null) {
            THIS.UPDATE_THEME(theme);
            return;
        }

        THIS.SHOW_MODAL();
    }

    /**
     * Gets the theme
     *
     * @param user the current user
     * @returns the userTheme if existing, else null
     */
    private getTheme(user: User | null): UserTheme | null {
        if (ENVIRONMENT.BACKEND === "OpenEMS Edge") {
            return LOCAL_STORAGE.GET_ITEM("THEME") as UserTheme ?? null;
        }

        return user?.getThemeFromSettings() ?? null;
    }

    /**
     * Updates the theme and initializes it
     *
     * @param userTheme the new user theme
     */
    private updateTheme(userTheme: UserTheme | null): void {
        const validTheme = THIS.GET_VALID_BROWSER_THEME(userTheme);
        let attr: Exclude<`${UserTheme}`, USER_THEME.SYSTEM> = validTheme;

        if (validTheme === USER_THEME.SYSTEM) {
            attr = WINDOW.MATCH_MEDIA("(prefers-color-scheme: dark)").matches ? USER_THEME.DARK : USER_THEME.LIGHT;
        }

        // Provide color to set before angular app inits
        const backgroundColor = getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-background-color");
        LOCAL_STORAGE.SET_ITEM("THEME_COLOR", backgroundColor);

        DOCUMENT.DOCUMENT_ELEMENT.SET_ATTRIBUTE("data-theme", attr);
    }

    /**
     * Shows the theme selection popover
    *
    * @param currentTheme current theme
    */
    private async showModal(): Promise<void> {

        const modal = await THIS.MODAL_CTRL.CREATE({
            component: ThemePopoverComponent,
        });

        await MODAL.PRESENT();

        const { data } = await MODAL.ON_DID_DISMISS();

        const selectedTheme = data?.selectedTheme ?? UserService.DEFAULT_THEME;
        THIS.FINALIZE_THEME_SELECTION(selectedTheme);
    }

    /**
     * Updates the user settings
    *
    * @param settings the new settings to use
    * @returns
    */
    private updateUserSettings(settings: object): Promise<[Error | null, JsonrpcResponseSuccess | null]> {
        const request = new UpdateUserSettingsRequest({ settings: settings });
        if (ENVIRONMENT.BACKEND === "OpenEMS Edge") {
            return PROMISE.RESOLVE([new UnimplementedInEdgeError(request), null]);
        }
        return JSON_RPC_UTILS.HANDLE<JsonrpcResponseSuccess>(THIS.SERVICE.WEBSOCKET.SEND_SAFE_REQUEST(request));
    }

    /**
     * Updates the theme for the current user
    *
    * @param theme the new theme
    */
    private updateCurrentUser(theme: Theme): void {
        THIS.CURRENT_USER.UPDATE((user: User | null) => {
            if (user == null) {
                return user;
            }

            USER.SETTINGS = {
                ...USER.SETTINGS,
                theme: theme,
            };
            return user;
        });
    }

    /**
     * Finalizes the theme selection
    *
    * @param theme the new theme
    * @returns
    */
    private finalizeThemeSelection(theme: Theme): Promise<void> {
        return THIS.UPDATE_USER_SETTINGS({ theme: theme })
            .then(() => {
                THIS.UPDATE_CURRENT_USER(theme as Theme);
                LOCAL_STORAGE.SET_ITEM("THEME", theme);
                THIS.UPDATE_THEME(theme);
            });
    }
}
