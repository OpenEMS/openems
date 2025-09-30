// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

type ManualMode = "FORCE_ON" | "RECOMMENDATION" | "REGULAR" | "LOCK";
type AutomaticEnableMode = "automaticRecommendationCtrlEnabled" | "automaticForceOnCtrlEnabled" | "automaticLockCtrlEnabled";

@Component({
  selector: "heatpump-modal",
  templateUrl: "./MODAL.COMPONENT.HTML",
  standalone: false,
})
export class Controller_Io_HeatpumpModalComponent implements OnInit {

  @Input() public edge: Edge | null = null;
  @Input() public component: EDGE_CONFIG.COMPONENT | null = null;

  public formGroup: FormGroup | null = null;
  public loading: boolean = false;

  constructor(
    private websocket: Websocket,
    protected translate: TranslateService,
    public formBuilder: FormBuilder,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    THIS.FORM_GROUP = THIS.FORM_BUILDER.GROUP({
      // Manual
      manualState: new FormControl(THIS.COMPONENT.PROPERTIES.MANUAL_STATE),
      // Automatic
      automaticForceOnCtrlEnabled: new FormControl(THIS.COMPONENT.PROPERTIES.AUTOMATIC_FORCE_ON_CTRL_ENABLED),
      automaticForceOnSoc: new FormControl(THIS.COMPONENT.PROPERTIES.AUTOMATIC_FORCE_ON_SOC),
      automaticForceOnSurplusPower: new FormControl(THIS.COMPONENT.PROPERTIES.AUTOMATIC_FORCE_ON_SURPLUS_POWER),
      automaticLockCtrlEnabled: new FormControl(THIS.COMPONENT.PROPERTIES.AUTOMATIC_LOCK_CTRL_ENABLED),
      automaticLockGridBuyPower: new FormControl(THIS.COMPONENT.PROPERTIES.AUTOMATIC_LOCK_GRID_BUY_POWER),
      automaticLockSoc: new FormControl(THIS.COMPONENT.PROPERTIES.AUTOMATIC_LOCK_SOC),
      automaticRecommendationCtrlEnabled: new FormControl(THIS.COMPONENT.PROPERTIES.AUTOMATIC_RECOMMENDATION_CTRL_ENABLED),
      automaticRecommendationSurplusPower: new FormControl(THIS.COMPONENT.PROPERTIES.AUTOMATIC_RECOMMENDATION_SURPLUS_POWER),
      minimumSwitchingTime: new FormControl(THIS.COMPONENT.PROPERTIES.MINIMUM_SWITCHING_TIME),
    });
  }

  public updateControllerMode(event: CustomEvent) {
    const oldMode = THIS.COMPONENT.PROPERTIES["mode"];
    const newMode = EVENT.DETAIL.VALUE;

    if (THIS.EDGE != null) {
      THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, [
        { name: "mode", value: newMode },
      ]).then(() => {
        THIS.COMPONENT.PROPERTIES.MODE = newMode;
        THIS.FORM_GROUP.MARK_AS_PRISTINE();
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
      }).catch(reason => {
        THIS.COMPONENT.PROPERTIES.MODE = oldMode;
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
        CONSOLE.WARN(reason);
      });
    }
  }

  public updateAutomaticEnableMode(isTrue: boolean, state: AutomaticEnableMode) {
    THIS.FORM_GROUP.CONTROLS[state].setValue(isTrue);
    THIS.FORM_GROUP.CONTROLS[state].markAsDirty();
  }

  public updateManualMode(state: ManualMode) {
    THIS.FORM_GROUP.CONTROLS["manualState"].setValue(state);
    THIS.FORM_GROUP.CONTROLS["manualState"].markAsDirty();
  }

  public applyChanges() {
    if (THIS.EDGE != null) {
      if (THIS.EDGE.ROLE_IS_AT_LEAST("owner")) {
        if (THIS.FORM_GROUP.CONTROLS["automaticRecommendationSurplusPower"].value < THIS.FORM_GROUP.CONTROLS["automaticForceOnSurplusPower"].value) {
          const updateComponentArray = [];
          OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS).forEach((element, index) => {
            if (THIS.FORM_GROUP.CONTROLS[element].dirty) {
              UPDATE_COMPONENT_ARRAY.PUSH({ name: OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index], value: THIS.FORM_GROUP.CONTROLS[element].value });
            }
          });
          THIS.LOADING = true;
          THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, updateComponentArray).then(() => {
            THIS.COMPONENT.PROPERTIES.MANUAL_STATE = THIS.FORM_GROUP.VALUE.MANUAL_STATE;
            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            THIS.LOADING = false;
          }).catch(reason => {
            THIS.FORM_GROUP.CONTROLS["minTime"].setValue(THIS.COMPONENT.PROPERTIES.MANUAL_STATE);
            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + reason, "danger");
            THIS.LOADING = false;
            CONSOLE.WARN(reason);
          });
          THIS.FORM_GROUP.MARK_AS_PRISTINE();
        } else {
          THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.RELATION_ERROR"), "danger");
        }
      } else {
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.INSUFFICIENT_RIGHTS"), "danger");
      }
    }
  }
}
