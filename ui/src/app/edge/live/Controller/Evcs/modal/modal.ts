// @ts-strict-ignore
import { ChangeDetectorRef, Component, Inject } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonRange, ModalController, PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { EvcsUtils } from "src/app/shared/components/edge/utils/evcs-utils";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { ChannelAddress, CurrentData, EdgeConfig, Service, Utils, Websocket } from "src/app/shared/shared";

import { AdministrationComponent } from "../administration/ADMINISTRATION.COMPONENT";
import { PopoverComponent } from "../popover/popover";

type ChargeMode = "FORCE_CHARGE" | "EXCESS_POWER";
@Component({
  templateUrl: "./MODAL.HTML",
  standalone: false,
})
export class ModalComponent extends AbstractModal {

  public readonly CONVERT_MANUAL_ON_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(THIS.TRANSLATE);
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(THIS.TRANSLATE);
  protected controller: EDGE_CONFIG.COMPONENT;
  protected evcsComponent: EDGE_CONFIG.COMPONENT;
  protected isConnectionSuccessful: boolean = false;
  protected readonly emptyValue: string = "-";
  protected status: string;
  protected chargePowerLimit: string;
  protected chargePower: { name: string; value: number; };
  protected state: string = "";
  protected energySession: string;
  protected minChargePower: number;
  protected maxChargePower: number;
  protected numberOfPhases: number = 3; // Defaults to three phases
  protected defaultChargeMinPower: number;
  protected energyLimit: boolean;
  protected chargeMode: ChargeMode | null = null;
  protected isEnergySinceBeginningAllowed: boolean = false;
  protected isChargingEnabled: boolean = false;
  protected sessionLimit: number;
  protected awaitingHysteresis: boolean;
  protected isReadWrite: boolean = true;

  protected readonly useDefaultPrefix: HelpButtonComponent["useDefaultPrefix"] = false;

  constructor(
    @Inject(Websocket) protected override websocket: Websocket,
    @Inject(ActivatedRoute) protected override route: ActivatedRoute,
    @Inject(Service) protected override service: Service,
    @Inject(ModalController) public override modalController: ModalController,
    @Inject(ModalController) public detailViewController: ModalController,
    @Inject(PopoverController) public popoverctrl: PopoverController,
    @Inject(TranslateService) protected override translate: TranslateService,
    @Inject(FormBuilder) public override formBuilder: FormBuilder,
    public override ref: ChangeDetectorRef) {
    super(
      websocket, route, service, modalController, translate,
      formBuilder, ref);
    REF.DETACH();
    setInterval(() => {
      THIS.REF.DETECT_CHANGES(); // manually trigger change detection
    }, 0);
  }

  protected readonly KILO_WATT_HOURS_PIN_FORMATTER: IonRange["pinFormatter"] = (val) => THIS.CONVERTER.TO_KILO_WATT_HOURS(val);
  protected readonly WATT_PIN_FORMATTER: IonRange["pinFormatter"] = (val) => THIS.CONVERTER.POWER_IN_WATT(val);

  protected async presentPopover() {
    const popover = await THIS.POPOVERCTRL.CREATE({
      component: PopoverComponent,
      componentProps: {
        chargeMode: THIS.FORM_GROUP.CONTROLS["chargeMode"].value,
      },
    });
    return await POPOVER.PRESENT();
  }

  protected async presentModal() {
    const modal = await THIS.DETAIL_VIEW_CONTROLLER.CREATE({
      component: AdministrationComponent,
      componentProps: {
        evcsComponent: THIS.EVCS_COMPONENT,
        edge: THIS.EDGE,
      },
    });
    MODAL.ON_DID_DISMISS().then(() => {
      THIS.UPDATE_RENAULT_ZOE_CONFIG();
    });
    return await MODAL.PRESENT();
  }

  protected override getChannelAddresses(): ChannelAddress[] {

    THIS.CONTROLLER = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.EVCS")
      .find(element => "EVCS.ID" in ELEMENT.PROPERTIES && ELEMENT.PROPERTIES["EVCS.ID"] == THIS.COMPONENT.ID);

    THIS.EVCS_COMPONENT = THIS.CONFIG.GET_COMPONENT(THIS.COMPONENT.ID);

    return [
      // channels for modal component, subscribe here for better UX
      new ChannelAddress(THIS.COMPONENT.ID, THIS.GET_POWER_CHANNEL_ID()),
      new ChannelAddress(THIS.COMPONENT.ID, "Phases"),
      new ChannelAddress(THIS.COMPONENT.ID, "Plug"),
      new ChannelAddress(THIS.COMPONENT.ID, "Status"),
      new ChannelAddress(THIS.COMPONENT.ID, "State"),
      new ChannelAddress(THIS.COMPONENT.ID, "EnergySession"),
      new ChannelAddress(THIS.COMPONENT.ID, "MinimumHardwarePower"),
      new ChannelAddress(THIS.COMPONENT.ID, "MaximumHardwarePower"),
      new ChannelAddress(THIS.COMPONENT.ID, "SetChargePowerLimit"),
      new ChannelAddress(THIS.COMPONENT.ID, "_PropertyReadOnly"),
      new ChannelAddress(THIS.CONTROLLER?.id, "_PropertyChargeMode"),
      new ChannelAddress(THIS.CONTROLLER?.id, "_PropertyEnabledCharging"),
      new ChannelAddress(THIS.CONTROLLER?.id, "_PropertyDefaultChargeMinPower"),
      new ChannelAddress(THIS.CONTROLLER?.id, "AwaitingHysteresis"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    THIS.IS_CONNECTION_SUCCESSFUL = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/State"] !== 3 ? true : false;
    THIS.AWAITING_HYSTERESIS = CURRENT_DATA.ALL_COMPONENTS[THIS.CONTROLLER?.id + "/AwaitingHysteresis"];
    THIS.IS_READ_WRITE = THIS.COMPONENT.HAS_PROPERTY_VALUE<boolean>("readOnly", true) === false;
    // Do not change values after touching formControls
    if (THIS.FORM_GROUP?.pristine) {
      THIS.STATUS = THIS.GET_STATE(THIS.CONTROLLER ? CURRENT_DATA.ALL_COMPONENTS[THIS.CONTROLLER.ID + "/_PropertyEnabledCharging"] === 1 : null, CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Status"], CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Plug"]);
      THIS.CHARGE_POWER = UTILS.CONVERT_CHARGE_DISCHARGE_POWER(THIS.TRANSLATE, CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + THIS.GET_POWER_CHANNEL_ID()]);
      THIS.CHARGE_POWER_LIMIT = Utils.CONVERT_TO_WATT(THIS.FORMAT_NUMBER(CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SetChargePowerLimit"]));
      THIS.STATE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Status"];
      THIS.ENERGY_SESSION = Utils.CONVERT_TO_WATTHOURS(CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/EnergySession"]);
      THIS.MIN_CHARGE_POWER = THIS.FORMAT_NUMBER(CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/MinimumHardwarePower"]);
      THIS.MAX_CHARGE_POWER = THIS.FORMAT_NUMBER(CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/MaximumHardwarePower"]);
      THIS.NUMBER_OF_PHASES = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Phases"] ? CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Phases"] : 3;
      THIS.DEFAULT_CHARGE_MIN_POWER = CURRENT_DATA.ALL_COMPONENTS[THIS.CONTROLLER?.id + "/_PropertyDefaultChargeMinPower"];
    }
  }

  protected override onIsInitialized(): void {
    THIS.SUBSCRIPTION.ADD(THIS.FORM_GROUP?.controls["energyLimit"]?.VALUE_CHANGES.SUBSCRIBE(isEnergyLimit => {
      if (isEnergyLimit) {
        if (THIS.FORM_GROUP.CONTROLS["energySessionLimit"]?.value === 0) {
          THIS.FORM_GROUP.CONTROLS["energySessionLimit"].setValue(20000);
          THIS.FORM_GROUP.CONTROLS["energySessionLimit"].markAsDirty();
        } else {
          // energySessionLimit is already valid -> do nothing
        }
      } else {
        THIS.FORM_GROUP.CONTROLS["energySessionLimit"].setValue(0);
        THIS.FORM_GROUP.CONTROLS["energySessionLimit"].markAsDirty();
      }
    }));

    THIS.SUBSCRIPTION.ADD(THIS.FORM_GROUP?.get("chargeMode").VALUE_CHANGES.SUBSCRIBE(chargeMode => {
      if (chargeMode === "OFF") {
        THIS.FORM_GROUP.GET("enabledCharging").setValue(false);
        THIS.FORM_GROUP.GET("chargeMode").markAsPristine(); // do not send chargeMode=OFF to Edge
      } else {
        THIS.FORM_GROUP.GET("enabledCharging").setValue(true);
      }
      THIS.FORM_GROUP.GET("enabledCharging").markAsDirty();
    }));

    THIS.SUBSCRIPTION.ADD(THIS.FORM_GROUP?.get("minGuarantee").VALUE_CHANGES.SUBSCRIBE(minGuarantee => {
      if (minGuarantee) {
        THIS.FORM_GROUP.CONTROLS["defaultChargeMinPower"].setValue(1400 /* approx min power per phase */ * THIS.NUMBER_OF_PHASES);
      } else {
        THIS.FORM_GROUP.CONTROLS["defaultChargeMinPower"].setValue(0);
      }
      THIS.FORM_GROUP.CONTROLS["defaultChargeMinPower"].markAsDirty();
    }));

    // Convert FormGroup value in kWh to Wh for Component config
    THIS.SUBSCRIPTION.ADD(THIS.FORM_GROUP?.get("energySessionLimitKwh").VALUE_CHANGES.SUBSCRIBE((newValue) => {
      THIS.FORM_GROUP.CONTROLS["energySessionLimit"].setValue(newValue * 1000);
      THIS.FORM_GROUP.CONTROLS["energySessionLimit"].markAsDirty();
      THIS.FORM_GROUP.CONTROLS["energySessionLimitKwh"].markAsPristine();
    }));
  }

  protected override getFormGroup(): FormGroup {
    return THIS.FORM_BUILDER.GROUP({
      chargeMode: new FormControl(THIS.CONTROLLER?.PROPERTIES.ENABLED_CHARGING == false ? "OFF" : THIS.CONTROLLER?.PROPERTIES.CHARGE_MODE),
      energyLimit: new FormControl(THIS.CONTROLLER?.PROPERTIES.ENERGY_SESSION_LIMIT > 0),
      minGuarantee: new FormControl(THIS.CONTROLLER?.PROPERTIES.DEFAULT_CHARGE_MIN_POWER > 0),
      defaultChargeMinPower: new FormControl(THIS.CONTROLLER?.PROPERTIES.DEFAULT_CHARGE_MIN_POWER),
      forceChargeMinPower: new FormControl(THIS.CONTROLLER?.PROPERTIES.FORCE_CHARGE_MIN_POWER),
      priority: new FormControl(THIS.CONTROLLER?.PROPERTIES.PRIORITY),
      // EnergySessionLimit as Wh value
      energySessionLimit: new FormControl(THIS.CONTROLLER?.PROPERTIES.ENERGY_SESSION_LIMIT),
      // EnergySessionLimit as kWh value, for ion-range
      energySessionLimitKwh: new FormControl(MATH.ROUND(THIS.CONTROLLER?.PROPERTIES.ENERGY_SESSION_LIMIT / 1000)),
      enabledCharging: new FormControl(THIS.IS_CHARGING_ENABLED),
    });
  }

  /**
   * Formats the pin value
   *
   * @param value the value
   * @returns a formatted value
   */
  protected pinFormatter(value: number): string {
    return Formatter.FORMAT_WATT(value);
  }

  /**
     * Updates the Min-Power of force charging
     *
     * @param event
     */
  protected updateForceMinPower(event: CustomEvent, currentController: EDGE_CONFIG.COMPONENT, numberOfPhases: number) {

    const newMinChargePower = EVENT.DETAIL.VALUE / numberOfPhases;
    THIS.FORM_GROUP.CONTROLS["forceChargeMinPower"].markAsDirty();
    THIS.FORM_GROUP.CONTROLS["forceChargeMinPower"].setValue(newMinChargePower);
  }

  /**
 * Updates the MinChargePower for Renault Zoe Charging Mode if activated in administration component
 */
  protected updateRenaultZoeConfig() {
    if (THIS.CONTROLLER && THIS.EVCS_COMPONENT.PROPERTIES["minHwCurrent"] == 10000) {

      const oldMinChargePower = THIS.CONTROLLER.PROPERTIES.FORCE_CHARGE_MIN_POWER;
      const maxAllowedChargePower = 10 /* Ampere */ * 230; /* Volt */

      if (oldMinChargePower < maxAllowedChargePower) {
        if (THIS.EDGE != null) {
          const newMinChargePower = maxAllowedChargePower;
          THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.CONTROLLER.ID, [
            { name: "forceChargeMinPower", value: newMinChargePower },
          ]).then(() => {
            THIS.CONTROLLER.PROPERTIES.FORCE_CHARGE_MIN_POWER = newMinChargePower;
          }).catch(reason => {
            THIS.CONTROLLER.PROPERTIES.FORCE_CHARGE_MIN_POWER = oldMinChargePower;
            CONSOLE.WARN(reason);
          });
        }
      }
    }
  }

  protected formatNumber(i: number) {
    const round = MATH.CEIL(i / 100) * 100;
    return round;
  }

  /**
  * Returns the state of the EVCS
  *
  * @param state the state
  * @param plug the plug
  *
  */
  private getState(enabledCharging: boolean, state: number, plug: number): string {

    if (enabledCharging === false) {
      return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CHARGING_STATION_DEACTIVATED");
    }

    if (plug == null) {
      if (state == null) {
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");
      }
    } else if (plug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED && THIS.CHARGE_POWER?.value > 450) {
      return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CABLE_NOT_CONNECTED");
    }
    switch (state) {
      case CHARGE_STATE.STARTING:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.STARTING");
      case CHARGE_STATE.UNDEFINED:
      case CHARGE_STATE.ERROR:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.ERROR");
      case ChargeState.READY_FOR_CHARGING:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.READY_FOR_CHARGING");
      case ChargeState.NOT_READY_FOR_CHARGING:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.NOT_READY_FOR_CHARGING");
      case ChargeState.AUTHORIZATION_REJECTED:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");
      case CHARGE_STATE.CHARGING:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CHARGING");
      case ChargeState.ENERGY_LIMIT_REACHED:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CHARGE_LIMIT_REACHED");
      case ChargeState.CHARGING_FINISHED:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CAR_FULL");
    }
  }

  private getPowerChannelId(): string {
    return EVCS_UTILS.GET_EVCS_POWER_CHANNEL_ID(THIS.COMPONENT, THIS.CONFIG, THIS.EDGE);
  }
}


enum ChargeState {
  UNDEFINED = -1,           //Undefined
  STARTING,                 //Starting
  NOT_READY_FOR_CHARGING,   //Not ready for Charging E.G. unplugged, X1 or "ena" not enabled, RFID not enabled,...
  READY_FOR_CHARGING,       //Ready for Charging waiting for EV charging request
  CHARGING,                 //Charging
  ERROR,                    //Error
  AUTHORIZATION_REJECTED,   //Authorization rejected
  ENERGY_LIMIT_REACHED,     //Energy limit reached
  CHARGING_FINISHED,        //Charging has finished
}

enum ChargePlug {
  UNDEFINED = -1,                           //Undefined
  UNPLUGGED,                                //Unplugged
  PLUGGED_ON_EVCS,                          //Plugged on EVCS
  PLUGGED_ON_EVCS_AND_LOCKED = 3,           //Plugged on EVCS and locked
  PLUGGED_ON_EVCS_AND_ON_EV = 5,            //Plugged on EVCS and on EV
  PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED = 7, //Plugged on EVCS and on EV and locked
}
