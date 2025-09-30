// @ts-strict-ignore
import { Component, effect, Input, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { FormUtils } from "src/app/shared/utils/form/FORM.UTILS";

type mode = "ON" | "AUTOMATIC" | "OFF";
type inputMode = "SOC" | "GRIDSELL" | "GRIDBUY" | "PRODUCTION" | "OTHER";

@Component({
  selector: "oe-controller-io-channelsinglethreshold-modal",
  templateUrl: "./MODAL.COMPONENT.HTML",
  standalone: false,
  providers: [
    { provide: DataService, useClass: LiveDataService },
  ],
})
export class Controller_Io_ChannelSingleThresholdModalComponent implements OnInit {

  @Input({ required: true }) public edge!: Edge;
  @Input({ required: true }) public config!: EdgeConfig;
  @Input({ required: true }) public component!: EDGE_CONFIG.COMPONENT;
  @Input() public outputChannel: ChannelAddress | null = null;
  @Input({ required: true }) public inputChannel!: ChannelAddress;
  @Input() public inputChannelUnit: string | null = null;

  public formGroup: FormGroup;

  public loading: boolean = false;

  public minimumSwitchingTime = null;
  public threshold = null;
  public switchedLoadPower = null;
  public inputMode = null;
  public invert: number | null = null;

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
    public translate: TranslateService,
    public websocket: Websocket,
    public formBuilder: FormBuilder,
    private dataService: DataService,
  ) {

    effect(() => {
      const currValue = DATA_SERVICE.CURRENT_VALUE();
      const invert = CURR_VALUE.ALL_COMPONENTS[new ChannelAddress(THIS.COMPONENT.ID, "_PropertyInvert").toString()] == 1;

      const formControl = FORM_UTILS.FIND_FORM_CONTROL_SAFELY(THIS.FORM_GROUP, "invert");
      if (!FORM_CONTROL.DIRTY) {
        THIS.FORM_GROUP.CONTROLS["invert"].setValue(invert);
      }
    });
  }

  ngOnInit() {
    THIS.FORM_GROUP = THIS.FORM_BUILDER.GROUP({
      minimumSwitchingTime: new FormControl(THIS.COMPONENT.PROPERTIES.MINIMUM_SWITCHING_TIME, VALIDATORS.COMPOSE([
        VALIDATORS.MIN(5),
        VALIDATORS.PATTERN("^[1-9][0-9]*$"),
        VALIDATORS.REQUIRED,
      ])),
      switchedLoadPower: new FormControl(THIS.COMPONENT.PROPERTIES.SWITCHED_LOAD_POWER, VALIDATORS.COMPOSE([
        VALIDATORS.PATTERN("^(?:[1-9][0-9]*|0)$"),
        VALIDATORS.REQUIRED,
      ])),
      threshold: new FormControl(THIS.GET_INPUT_MODE() == "GRIDSELL" ? THIS.COMPONENT.PROPERTIES.THRESHOLD * -1 : THIS.COMPONENT.PROPERTIES.THRESHOLD, VALIDATORS.COMPOSE([
        VALIDATORS.MIN(1),
        VALIDATORS.PATTERN("^[1-9][0-9]*$"),
        VALIDATORS.REQUIRED,
      ])),
      inputMode: new FormControl(THIS.GET_INPUT_MODE()),
      invert: new FormControl(null, VALIDATORS.REQUIRED_TRUE),
    });
    THIS.MINIMUM_SWITCHING_TIME = THIS.FORM_GROUP.CONTROLS["minimumSwitchingTime"];
    THIS.THRESHOLD = THIS.FORM_GROUP.CONTROLS["threshold"];
    THIS.SWITCHED_LOAD_POWER = THIS.FORM_GROUP.CONTROLS["switchedLoadPower"];
    THIS.INPUT_MODE = THIS.FORM_GROUP.CONTROLS["inputMode"];
    THIS.INVERT = THIS.COMPONENT.PROPERTIES["invert"];

    THIS.DATA_SERVICE.GET_VALUES([new ChannelAddress(THIS.COMPONENT.ID, "_PropertyInvert")], THIS.EDGE, "");
  }

  public updateInputMode(event: CustomEvent) {
    let newThreshold: number = THIS.COMPONENT.PROPERTIES.THRESHOLD;

    switch (EVENT.DETAIL.VALUE) {
      case "SOC":
        THIS.INPUT_MODE.SET_VALUE("SOC");
        THIS.SWITCHED_LOAD_POWER.SET_VALUE(0);
        THIS.SWITCHED_LOAD_POWER.MARK_AS_DIRTY();
        if (MATH.ABS(THIS.COMPONENT.PROPERTIES.THRESHOLD) < 0 || MATH.ABS(THIS.COMPONENT.PROPERTIES.THRESHOLD) > 100) {
          newThreshold = 50;
          THIS.THRESHOLD.SET_VALUE(newThreshold);
          THIS.THRESHOLD.MARK_AS_DIRTY();
        } else if (THIS.COMPONENT.PROPERTIES.THRESHOLD < 0) {
          THIS.THRESHOLD.SET_VALUE(newThreshold);
          THIS.THRESHOLD.MARK_AS_DIRTY();
        }
        break;
      case "GRIDSELL":
        THIS.INPUT_MODE.SET_VALUE("GRIDSELL");
        THIS.THRESHOLD.MARK_AS_DIRTY();
        THIS.SWITCHED_LOAD_POWER.MARK_AS_DIRTY();
        break;
      case "GRIDBUY":
        THIS.INPUT_MODE.SET_VALUE("GRIDBUY");
        THIS.SWITCHED_LOAD_POWER.MARK_AS_DIRTY();
        if (THIS.COMPONENT.PROPERTIES.THRESHOLD < 0) {
          newThreshold = THIS.FORM_GROUP.VALUE.THRESHOLD;
          THIS.THRESHOLD.SET_VALUE(newThreshold);
          THIS.THRESHOLD.MARK_AS_DIRTY();
        }
        break;
      case "PRODUCTION":
        THIS.INPUT_MODE.SET_VALUE("PRODUCTION");
        THIS.SWITCHED_LOAD_POWER.SET_VALUE(0);
        THIS.SWITCHED_LOAD_POWER.MARK_AS_DIRTY();
        if (THIS.COMPONENT.PROPERTIES.THRESHOLD < 0) {
          newThreshold = THIS.THRESHOLD.VALUE;
          THIS.THRESHOLD.SET_VALUE(newThreshold);
          THIS.THRESHOLD.MARK_AS_DIRTY();
        }
        break;
    }
  }

  public updateMode(event: CustomEvent) {
    const oldMode = THIS.COMPONENT.PROPERTIES.MODE;
    let newMode: mode;

    switch (EVENT.DETAIL.VALUE) {
      case "ON":
        newMode = "ON";
        break;
      case "OFF":
        newMode = "OFF";
        break;
      case "AUTOMATIC":
        newMode = "AUTOMATIC";
        break;
    }

    if (THIS.EDGE != null) {
      THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, [
        { name: "mode", value: newMode },
      ]).then(() => {
        THIS.COMPONENT.PROPERTIES.MODE = newMode;
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
      }).catch(reason => {
        THIS.COMPONENT.PROPERTIES.MODE = oldMode;
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
        CONSOLE.WARN(reason);
      });
    }
  }

  public applyChanges(): void {
    if (THIS.EDGE != null) {
      if (THIS.EDGE.ROLE_IS_AT_LEAST("owner")) {
        if (THIS.MINIMUM_SWITCHING_TIME.VALID && THIS.THRESHOLD.VALID && THIS.SWITCHED_LOAD_POWER.VALID) {
          if (THIS.THRESHOLD.VALUE > THIS.SWITCHED_LOAD_POWER.VALUE) {
            const updateComponentArray = [];
            OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS).forEach((element, index) => {
              if (THIS.FORM_GROUP.CONTROLS[element].dirty) {
                // catch inputMode and convert it to inputChannelAddress
                if (OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index] == "inputMode") {
                  UPDATE_COMPONENT_ARRAY.PUSH({ name: "inputChannelAddress", value: THIS.CONVERT_TO_CHANNEL_ADDRESS(THIS.FORM_GROUP.CONTROLS[element].value) });
                } else if (THIS.INPUT_MODE.VALUE == "GRIDSELL" && OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index] == "threshold") {
                  THIS.FORM_GROUP.CONTROLS[element].setValue(THIS.FORM_GROUP.CONTROLS[element].value * -1);
                  UPDATE_COMPONENT_ARRAY.PUSH({ name: OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index], value: THIS.FORM_GROUP.CONTROLS[element].value });
                } else {
                  UPDATE_COMPONENT_ARRAY.PUSH({ name: OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index], value: THIS.FORM_GROUP.CONTROLS[element].value });
                }
              }
            });
            THIS.LOADING = true;
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, updateComponentArray).then(() => {
              THIS.COMPONENT.PROPERTIES.MINIMUM_SWITCHING_TIME = THIS.MINIMUM_SWITCHING_TIME.VALUE;
              THIS.COMPONENT.PROPERTIES.THRESHOLD = THIS.INPUT_MODE.VALUE == "GRIDSELL" ? THIS.THRESHOLD.VALUE * -1 : THIS.THRESHOLD.VALUE;
              THIS.COMPONENT.PROPERTIES.SWITCHED_LOAD_POWER = THIS.SWITCHED_LOAD_POWER.VALUE;
              THIS.COMPONENT.PROPERTIES.INPUT_CHANNEL_ADDRESS = THIS.CONVERT_TO_CHANNEL_ADDRESS(THIS.INPUT_MODE.VALUE) != THIS.COMPONENT.PROPERTIES.INPUT_CHANNEL_ADDRESS ? THIS.CONVERT_TO_CHANNEL_ADDRESS(THIS.INPUT_MODE.VALUE) : THIS.COMPONENT.PROPERTIES.INPUT_CHANNEL_ADDRESS;
              THIS.LOADING = false;
              THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            }).catch(reason => {
              THIS.LOADING = false;
              THIS.MINIMUM_SWITCHING_TIME.SET_VALUE(THIS.COMPONENT.PROPERTIES.MINIMUM_SWITCHING_TIME);
              THIS.THRESHOLD.SET_VALUE(THIS.COMPONENT.PROPERTIES.THRESHOLD);
              THIS.SWITCHED_LOAD_POWER.SET_VALUE(THIS.COMPONENT.PROPERTIES.SWITCHED_LOAD_POWER);
              THIS.INPUT_MODE.SET_VALUE(THIS.CONVERT_TO_INPUT_MODE(THIS.COMPONENT.PROPERTIES.INPUT_CHANNEL_ADDRESS, THIS.COMPONENT.PROPERTIES.THRESHOLD));
              THIS.LOADING = false;
              THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
              CONSOLE.WARN(reason);
            });
            if (THIS.INPUT_MODE.VALUE == "GRIDSELL") {
              if (THIS.INPUT_MODE.DIRTY || THIS.THRESHOLD.DIRTY) {
                THIS.THRESHOLD.SET_VALUE(THIS.THRESHOLD.VALUE * -1);
              }
            }
            THIS.FORM_GROUP.MARK_AS_PRISTINE();
          } else {
            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.RELATION_ERROR"), "danger");
          }
        } else {
          THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.INPUT_NOT_VALID"), "danger");
        }
      } else {
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.INSUFFICIENT_RIGHTS"), "danger");
      }
    }
  }

  private getInputMode(): inputMode {
    if (THIS.COMPONENT.PROPERTIES.INPUT_CHANNEL_ADDRESS == "_sum/GridActivePower" && THIS.COMPONENT.PROPERTIES.THRESHOLD < 0) {
      return "GRIDSELL";
    } else if (THIS.COMPONENT.PROPERTIES.INPUT_CHANNEL_ADDRESS == "_sum/GridActivePower" && THIS.COMPONENT.PROPERTIES.THRESHOLD > 0) {
      return "GRIDBUY";
    } else if (THIS.COMPONENT.PROPERTIES.INPUT_CHANNEL_ADDRESS == "_sum/ProductionActivePower") {
      return "PRODUCTION";
    } else if (THIS.COMPONENT.PROPERTIES.INPUT_CHANNEL_ADDRESS == "_sum/EssSoc") {
      return "SOC";
    } else if (THIS.COMPONENT.PROPERTIES.INPUT_CHANNEL_ADDRESS != null) {
      return "OTHER";
    }
  }

  private convertToChannelAddress(inputMode: inputMode): string | null {
    switch (inputMode) {
      case "SOC":
        return "_sum/EssSoc";
      case "GRIDBUY":
        return "_sum/GridActivePower";
      case "GRIDSELL":
        return "_sum/GridActivePower";
      case "PRODUCTION":
        return "_sum/ProductionActivePower";
      default:
        return null;
    }
  }

  private convertToInputMode(inputChannelAddress: string, threshold: number): inputMode {
    switch (inputChannelAddress) {
      case "_sum/EssSoc":
        return "SOC";
      case "_sum/ProductionActivePower":
        return "PRODUCTION";
      case "_sum/GridActivePower":
        if (threshold > 0) {
          return "GRIDBUY";
        } else if (threshold < 0) {
          return "GRIDSELL";
        }
    }
  }

}
