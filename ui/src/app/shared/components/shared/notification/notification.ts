import { Component, Input, OnInit } from "@angular/core";
import { ToastController } from "@ionic/angular";
import { isAfter } from "date-fns";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";

@Component({
    selector: "oe-notification",
    template: "",
    standalone: false,
})
export class NotificationComponent implements OnInit {

    private static readonly PREFIX = "hide-notification-";
    private static readonly MAX_SHOW_DATE = new Date(2025, 9, 8);
    private static THREE_DAYS_IN_S: number = 60 * 60 * 24 * 3;

    @Input() private text: string | null = null;
    @Input() private id: string | number | null = null;

    private hideMessage: boolean = true;

    constructor(private toastCtrl: ToastController) { }

    ngOnInit() {
        const note = localStorage.getItem(NotificationComponent.PREFIX + this.id);
        const hasMaxShowDateBeenReached = isAfter(new Date(), NotificationComponent.MAX_SHOW_DATE);
        if (hasMaxShowDateBeenReached == false && note == null) {
            this.hideMessage = false;
            this.createToast();
            return;
        }

        const hasNotBeenShown = DateTimeUtils.isDifferenceInSecondsGreaterThan(
            NotificationComponent.THREE_DAYS_IN_S, new Date(), DateUtils.stringToDate(note)) == false;

        this.hideMessage = hasMaxShowDateBeenReached === true ? true : hasNotBeenShown;
        this.createToast();
    }

    /**
   * Creates a toast and hides it, if it has been seen.
   */
    async createToast(): Promise<void> {
        if (!this.text) {
            return;
        }

        if (!this.id) {
            console.error("Id needs to be provided");
            return;
        }

        if (this.hideMessage) {
            return;
        }

        const popover = await this.toastCtrl.create({
            translucent: false,
            message: this.text,
            position: "bottom",
            buttons: [
                {
                    icon: "close-outline", role: "cancel", side: "end",
                },
            ],
            color: "primary",
            cssClass: "toast-top-close",
        });

        popover.present();
        this.styleToast();

        await popover.onDidDismiss().then(() => {
            localStorage.setItem(NotificationComponent.PREFIX + this.id, new Date().toISOString());
        });
    }

    /**
   * Styles the toast notification.
   *
   * Needed due to shadow DOM not directly accessible.
   */
    private styleToast() {
        const toastEl = document.querySelector("ion-toast");

        if (toastEl == null) {
            return;
        }

        const shadow = toastEl!.shadowRoot;
        const buttonGroup: HTMLElement | null = shadow?.querySelector(".toast-button-group") ?? null;
        if (buttonGroup == null) {
            return;
        }

        buttonGroup.style.alignSelf = "flex-start";
        buttonGroup.style.display = "flex !important;";
        const toast: HTMLElement | null = shadow?.querySelector(".toast-container") ?? null;

        if (toast == null) {
            return;
        }

        toast.style.backgroundColor = "rgba(var(--ion-background-color-rgb), 0.2)";
    }
}
