import { Component, Input, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../../../shared/shared";

@Component({
  selector: ADMINISTRATION_COMPONENT.SELECTOR,
  templateUrl: "./ADMINISTRATION.COMPONENT.HTML",
  standalone: false,
})
export class AdministrationComponent implements OnInit {

  private static readonly SELECTOR = "administration";

  @Input({ required: true }) public evcsComponent!: EDGE_CONFIG.COMPONENT;
  @Input({ required: true }) public edge!: Edge;

  // used for ion-toggle in html
  public isCheckedZoe: boolean | null = null;

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
    private websocket: Websocket,
    public translate: TranslateService,
  ) { }

  ngOnInit() {
    if (THIS.EVCS_COMPONENT.PROPERTIES["minHwCurrent"] == 6000) {
      THIS.IS_CHECKED_ZOE = false;
    } else if (THIS.EVCS_COMPONENT.PROPERTIES["minHwCurrent"] == 10000) {
      THIS.IS_CHECKED_ZOE = true;
    }
  }

  updateZoeMode(event: CustomEvent) {
    let newValue = THIS.EVCS_COMPONENT.PROPERTIES["minHwCurrent"];
    const oldValue = THIS.EVCS_COMPONENT.PROPERTIES["minHwCurrent"];

    if (EVENT.DETAIL.CHECKED == true) {
      newValue = 10000;
    } else {
      newValue = 6000;
    }

    if (THIS.EDGE != null && oldValue != newValue) {
      THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.EVCS_COMPONENT.ID, [
        { name: "minHwCurrent", value: newValue },
      ]).then(() => {
        THIS.EVCS_COMPONENT.PROPERTIES.MIN_HW_CURRENT = newValue;
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
      }).catch(reason => {
        THIS.EVCS_COMPONENT.PROPERTIES.MIN_HW_CURRENT = oldValue;
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + reason, "danger");
        CONSOLE.WARN(reason);
      });
    }
  }
}
