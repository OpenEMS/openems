import { Component, Input, OnInit, inject } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../../../shared/shared";

@Component({
  selector: AdministrationComponent.SELECTOR,
  templateUrl: "./administration.component.html",
  standalone: false,
})
export class AdministrationComponent implements OnInit {
  private route = inject(ActivatedRoute);
  modalCtrl = inject(ModalController);
  service = inject(Service);
  private websocket = inject(Websocket);
  translate = inject(TranslateService);


  private static readonly SELECTOR = "administration";

  @Input({ required: true }) public evcsComponent!: EdgeConfig.Component;
  @Input({ required: true }) public edge!: Edge;

  // used for ion-toggle in html
  public isCheckedZoe: boolean | null = null;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() { }

  ngOnInit() {
    if (this.evcsComponent.properties["minHwCurrent"] == 6000) {
      this.isCheckedZoe = false;
    } else if (this.evcsComponent.properties["minHwCurrent"] == 10000) {
      this.isCheckedZoe = true;
    }
  }

  updateZoeMode(event: CustomEvent) {
    let newValue = this.evcsComponent.properties["minHwCurrent"];
    const oldValue = this.evcsComponent.properties["minHwCurrent"];

    if (event.detail.checked == true) {
      newValue = 10000;
    } else {
      newValue = 6000;
    }

    if (this.edge != null && oldValue != newValue) {
      this.edge.updateComponentConfig(this.websocket, this.evcsComponent.id, [
        { name: "minHwCurrent", value: newValue },
      ]).then(() => {
        this.evcsComponent.properties.minHwCurrent = newValue;
        this.service.toast(this.translate.instant("General.changeAccepted"), "success");
      }).catch(reason => {
        this.evcsComponent.properties.minHwCurrent = oldValue;
        this.service.toast(this.translate.instant("General.changeFailed") + "\n" + reason, "danger");
        console.warn(reason);
      });
    }
  }
}
