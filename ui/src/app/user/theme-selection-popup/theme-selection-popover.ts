import { Component, Input } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
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
  @Input() public userTheme: UserTheme = "light" as UserTheme; // Current theme (light, dark, system)

  protected readonly displayThemes = [
    { key: "light", label: this.translate.instant("General.LIGHT"), img: "assets/img/Light-Mode-preview.jpg" },
    { key: "dark", label: this.translate.instant("General.DARK"), img: "assets/img/Dark-Mode-preview.jpg" },
    { key: "system", label: this.translate.instant("General.SYSTEM_THEME"), img: "assets/img/System-Mode-preview.jpg" },
  ];

  constructor(
    protected modalCtrl: ModalController,
    private translate: TranslateService,
  ) {
    this.userTheme = this.userTheme || "light" as UserTheme;
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
