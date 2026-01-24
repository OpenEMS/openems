import { Component } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { UserService } from "src/app/shared/service/user.service";
import { Theme as UserTheme } from "../../edge/history/shared";


@Component({
    selector: "oe-theme-selection-popover",
    templateUrl: "./theme-selection-popover.html",
    styles: [`
  .modal-wrapper{
    transform: translate3d(0px, 40px, 0px);
    opacity: 0.9;
    border-radius: 10em !important;
  }
    `],
    standalone: false,
})
export class ThemePopoverComponent {
    protected userTheme: UserTheme = UserService.DEFAULT_THEME; // Current theme (light, dark, system)

    protected readonly displayThemes = [
        { key: UserTheme.LIGHT, label: this.translate.instant("GENERAL.LIGHT"), img: "assets/img/light-mode-preview.jpg" },
        { key: UserTheme.DARK, label: this.translate.instant("GENERAL.DARK"), img: "assets/img/dark-mode-preview.jpg" },
        { key: UserTheme.SYSTEM, label: this.translate.instant("GENERAL.SYSTEM_THEME"), img: "assets/img/system-mode-preview.jpg" },
    ];

    constructor(
        protected modalCtrl: ModalController,
        private translate: TranslateService,
    ) {
        this.userTheme = this.userTheme || UserService.DEFAULT_THEME;
    }

    /**
   * Emits the selected theme and closes the popover.
   *
   * @param theme Selected theme.
   */
    protected saveTheme(): void {
        this.modalCtrl.dismiss({ selectedTheme: this.userTheme });
    }

    /**
   * Closes the popover without making a selection.
   */
    protected closePopover(): void {
        this.modalCtrl.dismiss();
    }

    /**
   * Set the selected theme when an option is clicked.
   *
   * @param event The change event from the ion-radio-group.
   */
    protected setCurrentTheme(theme: string): void {
        this.userTheme = theme as UserTheme;
    }
}
