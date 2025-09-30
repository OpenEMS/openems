import { Component, Input, OnChanges, OnInit } from "@angular/core";
import { ToastController } from "@ionic/angular";

@Component({
  selector: "oe-notification",
  template: "",
  standalone: false,
})
export class NotificationComponent implements OnInit, OnChanges {

  private static readonly PREFIX = "hide-notification-";

  @Input() private text: string | null = null;
  @Input() private id: string | number | null = null;

  private hideMessage: boolean = true;

  constructor(private toastie: ToastController) { }

  ngOnInit() {
    const note = LOCAL_STORAGE.GET_ITEM(NOTIFICATION_COMPONENT.PREFIX + THIS.ID);
    THIS.HIDE_MESSAGE = note != null ? note === "true" : false;

    THIS.CREATE_TOAST();
  }

  ngOnChanges() {
    THIS.CREATE_TOAST();
  }

  /**
   * Creates a toast and hides it, if it has been seen.
   */
  async createToast(): Promise<void> {
    if (!THIS.TEXT) {
      return;
    }

    if (!THIS.ID) {
      CONSOLE.ERROR("Id needs to be provided");
      return;
    }

    if (THIS.HIDE_MESSAGE) {
      return;
    }

    const popover = await THIS.TOASTIE.CREATE({
      translucent: false,
      message: THIS.TEXT,
      position: "bottom",
      buttons: [
        { icon: "close-outline", role: "cancel" },
      ],
    });

    POPOVER.PRESENT();

    await POPOVER.ON_DID_DISMISS().then(() => {
      LOCAL_STORAGE.SET_ITEM(NOTIFICATION_COMPONENT.PREFIX + THIS.ID, "true");
    });
  }
}
