import { Directive, effect, signal, WritableSignal } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { Theme, Theme as UserTheme } from "src/app/edge/history/shared";
import { ThemePopoverComponent } from "src/app/user/theme-selection-popup/theme-selection-popover";
import { environment } from "src/environments";
import { NavigationService } from "../components/navigation/service/navigation.service";
import { UnimplementedInEdgeError } from "../errors.ts/errors";
import { JsonrpcResponseSuccess } from "../jsonrpc/base";
import { JsonRpcUtils } from "../jsonrpc/jsonrpcutils";
import { UpdateUserSettingsRequest } from "../jsonrpc/request/updateUserSettingsRequest";
import { User } from "../jsonrpc/shared";
import { AssertionUtils } from "../utils/assertions/assertions.utils";
import { Service } from "./service";

@Directive()
export class UserService {

    public static readonly DEFAULT_THEME: UserTheme = UserTheme.LIGHT;
    public currentUser: WritableSignal<User | null> = signal(null);

    /** @deprecated determines if applying new ui or old*/
    public isNewNavigation: WritableSignal<boolean> = signal(false);

    constructor(
        private modalCtrl: ModalController,
        private service: Service,
    ) {

        // Prohibits switching colors on init
        this.updateTheme(localStorage.getItem("THEME") as UserTheme);
        effect(() => {
            const user = this.currentUser();

            if (user != null) {
                this.showThemeSelection(user);
                this.isNewNavigation.set(NavigationService.isNewNavigation(user, this.service.currentEdge()));
            }
        });
    }

    /**
     * Selects the new theme
     *
     * @param theme the new theme
     */
    public async selectTheme(theme: UserTheme): Promise<void> {

        const currentUser: User | null = this.currentUser();
        if (currentUser == null || !theme) {
            return;
        }

        currentUser.settings = { ...currentUser.settings, theme: theme };
        this.finalizeThemeSelection(theme);
    }

    public getValidBrowserTheme(userTheme: UserTheme | null): UserTheme {

        const theme = userTheme === UserTheme.SYSTEM
            ? window.matchMedia("(prefers-color-scheme: dark)").matches
                ? UserTheme.DARK
                : UserTheme.LIGHT
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
        const user = this.currentUser();
        AssertionUtils.assertIsDefined(user);
        const updatedSettings = { ...user.settings, [key]: value };
        const [err, _result] = await this.updateUserSettings(updatedSettings);
        if (err !== null) {
            throw err;
        }

        this.currentUser.set(new User(user.id, user.name, user.globalRole, user.language, user.hasMultipleEdges, updatedSettings));
    }

    /**
     * Shows the theme selection popover, only for new customers
     *
     * @returns
     */
    private showThemeSelection(user: User): void {
        const theme: UserTheme | null = this.getTheme(user);

        if (theme != null) {
            this.updateTheme(theme);
            return;
        }

        this.showModal();
    }

    /**
     * Gets the theme
     *
     * @param user the current user
     * @returns the userTheme if existing, else null
     */
    private getTheme(user: User | null): UserTheme | null {
        if (environment.backend === "OpenEMS Edge") {
            return localStorage.getItem("THEME") as UserTheme ?? null;
        }

        return user?.getThemeFromSettings() ?? null;
    }

    /**
     * Updates the theme and initializes it
     *
     * @param userTheme the new user theme
     */
    private updateTheme(userTheme: UserTheme | null): void {
        const validTheme = this.getValidBrowserTheme(userTheme);
        let attr: Exclude<`${UserTheme}`, UserTheme.SYSTEM> = validTheme;

        if (validTheme === UserTheme.SYSTEM) {
            attr = window.matchMedia("(prefers-color-scheme: dark)").matches ? UserTheme.DARK : UserTheme.LIGHT;
        }

        // Provide color to set before angular app inits
        const backgroundColor = getComputedStyle(document.documentElement).getPropertyValue("--ion-background-color");
        localStorage.setItem("THEME_COLOR", backgroundColor);

        document.documentElement.setAttribute("data-theme", attr);
    }

    /**
     * Shows the theme selection popover
    *
    * @param currentTheme current theme
    */
    private async showModal(): Promise<void> {

        const modal = await this.modalCtrl.create({
            component: ThemePopoverComponent,
        });

        await modal.present();

        const { data } = await modal.onDidDismiss();

        const selectedTheme = data?.selectedTheme ?? UserService.DEFAULT_THEME;
        this.finalizeThemeSelection(selectedTheme);
    }

    /**
     * Updates the user settings
    *
    * @param settings the new settings to use
    * @returns
    */
    private updateUserSettings(settings: object): Promise<[Error | null, JsonrpcResponseSuccess | null]> {
        const request = new UpdateUserSettingsRequest({ settings: settings });
        if (environment.backend === "OpenEMS Edge") {
            return Promise.resolve([new UnimplementedInEdgeError(request), null]);
        }
        return JsonRpcUtils.handle<JsonrpcResponseSuccess>(this.service.websocket.sendSafeRequest(request));
    }

    /**
     * Updates the theme for the current user
    *
    * @param theme the new theme
    */
    private updateCurrentUser(theme: Theme): void {
        this.currentUser.update((user: User | null) => {
            if (user == null) {
                return user;
            }

            user.settings = {
                ...user.settings,
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
        return this.updateUserSettings({ theme: theme })
            .then(() => {
                this.updateCurrentUser(theme as Theme);
                localStorage.setItem("THEME", theme);
                this.updateTheme(theme);
            });
    }
}
