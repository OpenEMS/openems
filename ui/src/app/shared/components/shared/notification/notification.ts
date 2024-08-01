import { Component, Input, OnChanges, OnInit } from "@angular/core";
import { ToastController } from "@ionic/angular";

@Component({
  selector: 'oe-notification',
  template: '',
})
export class NotificationComponent implements OnInit, OnChanges {

  private static readonly PREFIX = 'hide-notification-';

  @Input() private text: string | null = null;
  @Input() private id: string | number | null = null;

  private hideMessage: boolean = true;

  constructor(private toastie: ToastController) { }

  ngOnInit() {
    const note = localStorage.getItem(NotificationComponent.PREFIX + this.id);
    this.hideMessage = note != null ? note === 'true' : false;

    this.createToast();
  }

  ngOnChanges() {
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

    const popover = await this.toastie.create({
      translucent: false,
      message: this.text,
      position: 'bottom',
      buttons: [
        { icon: 'close-outline', role: 'cancel' },
      ],
    });

    popover.present();

    await popover.onDidDismiss().then(() => {
      localStorage.setItem(NotificationComponent.PREFIX + this.id, 'true');
    });
  }
}
