// @ts-strict-ignore
import { Directive, effect, signal, WritableSignal } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { Theme, Theme as UserTheme } from "src/app/edge/history/shared";
import { ThemePopoverComponent } from "src/app/user/theme-selection-popup/theme-selection-popover";
import { JsonrpcResponseSuccess } from "../jsonrpc/base";
import { UpdateUserSettingsRequest } from "../jsonrpc/request/updateUserSettingsRequest";
import { User } from "../jsonrpc/shared";
import { Service } from "./service";


@Directive()
export class UserService {

    private static readonly DEFAULT_THEME: UserTheme = UserTheme.LIGHT;
    public currentUser: WritableSignal<User | null> = signal(null);

    constructor(
        private modalCtrl: ModalController,
        private service: Service,
    ) {

        // Prohibits switching colors on init
        this.updateTheme(localStorage.getItem("THEME") as UserTheme);
        effect(() => {
            this.showThemeSelection();
        });
    }

    /**
     * Selects the new theme
     *
     * @param theme the new theme
     */
    public async selectTheme(theme: UserTheme): Promise<void> {
        await this.service.websocket.sendSafeRequest(new UpdateUserSettingsRequest({
            settings: { theme: theme },
        })).then(() => {
            const currentUser = this.currentUser();

            if (!currentUser || !theme) {
                return;
            }

            currentUser.settings["theme"] = theme;
            this.finalizeThemeSelection(theme);
        });
    }

    /**
     * Shows the theme selection popover, only for new customers
     *
     * @returns
     */
    private showThemeSelection(): void {
        const user = this.currentUser();
        const isThemeSet = user?.settings ? "theme" in user.settings : false;

        if (!user || isThemeSet) {
            this.updateTheme(user.settings["theme"]);
            return;
        }

        this.showModal(user.settings["theme"] as UserTheme);
    }

    /**
     * Updates the theme and initializes it
     *
     * @param userTheme the new user theme
     */
    private updateTheme(userTheme: UserTheme): void {
        const validTheme = this.getValidBrowserTheme(userTheme);
        let attr: Exclude<`${UserTheme}`, UserTheme.SYSTEM> = validTheme;

        if (validTheme === UserTheme.SYSTEM) {
            attr = window.matchMedia("(prefers-color-scheme: dark)").matches ? UserTheme.DARK : UserTheme.LIGHT;
        }
        document.documentElement.setAttribute("data-theme", attr);
    }

    /**
     * Shows the theme selection popover
    *
    * @param currentTheme current theme
    */
    private async showModal(currentTheme: UserTheme): Promise<void> {

        const modal = await this.modalCtrl.create({
            component: ThemePopoverComponent,
            componentProps: {
                userTheme: currentTheme, // Pass user theme (light, dark, or system)
            },
        });

        await modal.present();

        const { data } = await modal.onDidDismiss();
        if (data?.selectedTheme) {
            this.finalizeThemeSelection(data.selectedTheme);
        }
    }

    /**
     * Updates the user settings
    *
    * @param settings the new settings to use
    * @returns
    */
    private updateUserSettings(settings: object): Promise<JsonrpcResponseSuccess> {
        return this.service.websocket.sendSafeRequest(new UpdateUserSettingsRequest({ settings: settings }));
    }

    /**
     * Updates the theme for the current user
    *
    * @param theme the new theme
    */
    private updateCurrentUser(theme: Theme): void {
        this.currentUser.update((user) => {
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

    private getValidBrowserTheme(userTheme: UserTheme): UserTheme {
        return userTheme ?? UserService.DEFAULT_THEME;
    }
}
