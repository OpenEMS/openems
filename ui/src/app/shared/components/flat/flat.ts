// @ts-strict-ignore
import { Component, Input } from "@angular/core";
import { Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { Icon, ImageIcon } from "src/app/shared/type/widget";

@Component({
  selector: "oe-flat-widget",
  templateUrl: "./flat.html",
  standalone: false,
})
export class FlatWidgetComponent {

  /** Image in Header */
  @Input() public img?: ImageIcon;

  /** Icon in Header */
  @Input() public icon: Icon | null = null;

  /** BackgroundColor of the Header (light or dark) */
  @Input() public color?: string;

  /** Title in Header */
  @Input() public title?: string;

  /** Provide absolute link */
  @Input() public link: string | null = null;

  private _modalComponent: typeof this.modalComponent | null = null;
  constructor(
    private modalController: ModalController,
    private router: Router,
  ) { }

  @Input() public set modalComponent(val: { component: ModalComponent | null, componentProps?: ModalComponentProperties }) {
    this._modalComponent = val;
  };


  protected async onCallback() {
    if (this._modalComponent != null) {
      const modal = await this.modalController.create({
        component: this._modalComponent.component,
        componentProps: this._modalComponent.componentProps,
      });
      return await modal.present();
    }

    if (this.link != null) {
      this.router.navigateByUrl(this.link);
      return;
    }

  }
}


type ModalComponent = Pick<Parameters<typeof ModalController["prototype"]["create"]>, "0">["0"]["component"];
type ModalComponentProperties = Pick<Parameters<typeof ModalController["prototype"]["create"]>, "0">["0"]["componentProps"];

export type Modal = Pick<typeof FlatWidgetComponent, "prototype">["prototype"]["_modalComponent"];
