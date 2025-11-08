import { ChangeDetectorRef, Component, Inject } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonRange, ModalController, PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { EvcsComponent } from "src/app/shared/components/edge/components/evcsComponent";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { ChannelAddress, CurrentData, EdgeConfig, Service, Utils, Websocket } from "src/app/shared/shared";

import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { AdministrationComponent } from "../administration/administration.component";
import { PopoverComponent } from "../popover/popover";

type ChargeMode = "FORCE_CHARGE" | "EXCESS_POWER";
@Component({
  templateUrl: "./modal.html",
  standalone: false,
})
export class ModalComponent extends AbstractModal {

  public readonly CONVERT_MANUAL_ON_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(this.translate);
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(this.translate);
  protected controller: EdgeConfig.Component | null = null;
  protected evcsComponent: EdgeConfig.Component | null = null;
  protected isConnectionSuccessful: boolean = false;
  protected readonly emptyValue: string = "-";
  protected status: string | null = null;
  protected chargePowerLimit: string | null = null;
  protected chargePower: { name: string; value: number; } | null = null;
  protected state: string = "";
  protected energySession: string | null = null;
  protected minChargePower: number | null = null;
  protected maxChargePower: number | null = null;
  protected numberOfPhases: number = 3; // Defaults to three phases
  protected defaultChargeMinPower: number | null = null;
  protected energyLimit: boolean | null = null;
  protected chargeMode: ChargeMode | null = null;
  protected isEnergySinceBeginningAllowed: boolean = false;
  protected isChargingEnabled: boolean = false;
  protected sessionLimit: number | null = null;
  protected awaitingHysteresis: boolean | null = null;
  protected isReadWrite: boolean = true;
  protected readonly useDefaultPrefix: HelpButtonComponent["useDefaultPrefix"] = false;
  protected helpKey: string | null = null;
  private chargePoint: EvcsComponent | null = null;

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
    ref.detach();
    setInterval(() => {
      this.ref.detectChanges(); // manually trigger change detection
    }, 0);
  }

  protected static getHelpKey(factoryId: string): string | null {
    const map: Record<string, string> = {
      "Evcs.Keba.P40": "EVCS_KEBA",
      "Evcs.Keba.KeContact": "EVCS_KEBA",
      "Evcs.HardyBarth": "EVCS_HARDY_BARTH",
      "Evcs.Mennekes": "EVCS_MENNEKES",
      "Evcs.Goe.Http": "EVCS_GO_E",
      "Evcs.Ocpp.IesKeywattSingle": "EVCS_IES",
      "Evcs.AlpitronicHypercharger": "EVCS_ALPITRONIC_HYPER",
    };

    const key = map[factoryId];
    return key ? `REDIRECT.${key}` : null;
  }


  protected readonly KILO_WATT_HOURS_PIN_FORMATTER: IonRange["pinFormatter"] = (val) => this.Converter.TO_KILO_WATT_HOURS(val);
  protected readonly WATT_PIN_FORMATTER: IonRange["pinFormatter"] = (val) => this.Converter.POWER_IN_WATT(val);

  protected async presentPopover() {
    const popover = await this.popoverctrl.create({
      component: PopoverComponent,
      componentProps: {
        chargeMode: this.formGroup?.controls["chargeMode"].value ?? null,
      },
    });
    return await popover.present();
  }

  protected async presentModal() {
    const modal = await this.detailViewController.create({
      component: AdministrationComponent,
      componentProps: {
        evcsComponent: this.evcsComponent,
        edge: this.edge,
      },
    });
    modal.onDidDismiss().then(() => {
      this.updateRenaultZoeConfig();
    });
    return await modal.present();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    if (this.component == null || this.edge == null || this.config == null) {
      return [];
    }
    this.chargePoint = EvcsComponent.from(this.component, this.edge.getCurrentConfig(), this.edge);
    this.controller = this.config.getComponentsByFactory("Controller.Evcs")
      .find(element => "evcs.id" in element.properties && element.properties["evcs.id"] == this.component?.id) || null;

    this.evcsComponent = this.config.getComponent(this.component.id);

    const channels: ChannelAddress[] = [];

    if (this.chargePoint != null) {
      channels.push(this.chargePoint.powerChannel);
    }

    channels.push(
      new ChannelAddress(this.component.id, "Phases"),
      new ChannelAddress(this.component.id, "Plug"),
      new ChannelAddress(this.component.id, "Status"),
      new ChannelAddress(this.component.id, "State"),
      new ChannelAddress(this.component.id, "EnergySession"),
      new ChannelAddress(this.component.id, "MinimumHardwarePower"),
      new ChannelAddress(this.component.id, "MaximumHardwarePower"),
      new ChannelAddress(this.component.id, "SetChargePowerLimit"),
      new ChannelAddress(this.component.id, "_PropertyReadOnly"),
    );

    if (this.controller == null) {
      return channels;
    }

    channels.push(
      new ChannelAddress(this.controller.id, "_PropertyChargeMode"),
      new ChannelAddress(this.controller.id, "_PropertyEnabledCharging"),
      new ChannelAddress(this.controller.id, "_PropertyDefaultChargeMinPower"),
      new ChannelAddress(this.controller.id, "AwaitingHysteresis"),
    );

    return channels;
  }

  protected override onCurrentData(currentData: CurrentData) {
    if (this.component == null || this.controller == null) {
      return;
    }

    this.isConnectionSuccessful = currentData.allComponents[this.component.id + "/State"] !== 3 ? true : false;
    this.awaitingHysteresis = currentData.allComponents[this.controller?.id + "/AwaitingHysteresis"];
    this.isReadWrite = this.component.hasPropertyValue<boolean>("readOnly", true) === false;
    // Do not change values after touching formControls
    if (!this.formGroup?.pristine) {
      return;
    }

    if (this.chargePoint != null) {
      this.chargePower = Utils.convertChargeDischargePower(this.translate, currentData.allComponents[this.chargePoint.powerChannel.toString()]);
    }

    this.status = this.getState(this.controller ? currentData.allComponents[this.controller.id + "/_PropertyEnabledCharging"] === 1 : false, currentData.allComponents[this.component.id + "/Status"], currentData.allComponents[this.component.id + "/Plug"]);
    this.chargePowerLimit = Utils.CONVERT_TO_WATT(this.formatNumber(currentData.allComponents[this.component.id + "/SetChargePowerLimit"]));
    this.state = currentData.allComponents[this.component.id + "/Status"];
    this.energySession = Utils.CONVERT_TO_WATTHOURS(currentData.allComponents[this.component.id + "/EnergySession"]);
    this.minChargePower = this.formatNumber(currentData.allComponents[this.component.id + "/MinimumHardwarePower"]);
    this.maxChargePower = this.formatNumber(currentData.allComponents[this.component.id + "/MaximumHardwarePower"]);
    this.numberOfPhases = currentData.allComponents[this.component.id + "/Phases"] ? currentData.allComponents[this.component.id + "/Phases"] : 3;
    this.defaultChargeMinPower = currentData.allComponents[this.controller?.id + "/_PropertyDefaultChargeMinPower"];
  }

  protected override onIsInitialized(): void {
    if (this.formGroup == null || this.component == null || this.edge == null) {
      return;
    }

    this.chargePoint = EvcsComponent.from(this.component, this.edge.getCurrentConfig(), this.edge);

    const energyLimitCtrl = FormUtils.findFormControlSafely(this.formGroup as FormGroup, "energyLimit") as FormControl;
    const energySessionLimitCtrl = FormUtils.findFormControlSafely(this.formGroup as FormGroup, "energySessionLimit") as FormControl;
    const energySessionLimitKwhCtrl = FormUtils.findFormControlSafely(this.formGroup as FormGroup, "energySessionLimitKwh") as FormControl;
    const chargeModeCtrl = FormUtils.findFormControlSafely(this.formGroup as FormGroup, "chargeMode") as FormControl;
    const enabledChargingCtrl = FormUtils.findFormControlSafely(this.formGroup as FormGroup, "enabledCharging") as FormControl;
    const minGuaranteeCtrl = FormUtils.findFormControlSafely(this.formGroup as FormGroup, "minGuarantee") as FormControl;
    const defaultChargeMinPowerCtrl = FormUtils.findFormControlSafely(this.formGroup as FormGroup, "defaultChargeMinPower") as FormControl;

    if (energyLimitCtrl == null || energySessionLimitCtrl == null || energySessionLimitKwhCtrl == null || chargeModeCtrl == null || enabledChargingCtrl == null || minGuaranteeCtrl == null || defaultChargeMinPowerCtrl == null) {
      return;
    }

    const DEFAULT_ENERGY_SESSION_LIMIT = 20000;

    this.subscription.add(
      energyLimitCtrl.valueChanges.subscribe((isEnergyLimit: boolean) => {
        const newValue = isEnergyLimit
          ? (energySessionLimitCtrl.value === 0
            ? DEFAULT_ENERGY_SESSION_LIMIT
            : energySessionLimitCtrl.value)
          : 0;

        energySessionLimitCtrl.setValue(newValue);
        energySessionLimitCtrl.markAsDirty();
      })
    );

    this.subscription.add(
      chargeModeCtrl.valueChanges.subscribe((chargeMode: "OFF" | string) => {
        if (chargeMode === "OFF") {
          enabledChargingCtrl.setValue(false);
          chargeModeCtrl.markAsPristine();
        } else {
          enabledChargingCtrl.setValue(true);
        }
        enabledChargingCtrl.markAsDirty();
      })
    );

    if (this.subscription == null) {
      return;
    }

    this.subscription.add(
      minGuaranteeCtrl.valueChanges.subscribe((minGuarantee: boolean) => {
        const minPerPhase = 1400;
        defaultChargeMinPowerCtrl.setValue(minGuarantee ? minPerPhase * this.numberOfPhases : 0);
        defaultChargeMinPowerCtrl.markAsDirty();
      })
    );

    this.subscription.add(
      energySessionLimitKwhCtrl.valueChanges.subscribe((newValue: number) => {
        energySessionLimitCtrl.setValue(newValue * 1000);
        energySessionLimitCtrl.markAsDirty();
        energySessionLimitKwhCtrl.markAsPristine();
      })
    );

    this.helpKey = ModalComponent.getHelpKey(this.component.factoryId);
  }

  protected override getFormGroup(): FormGroup {
    return this.formBuilder.group({
      chargeMode: new FormControl(this.controller?.properties.enabledCharging == false ? "OFF" : this.controller?.properties.chargeMode),
      energyLimit: new FormControl(this.controller?.properties.energySessionLimit > 0),
      minGuarantee: new FormControl(this.controller?.properties.defaultChargeMinPower > 0),
      defaultChargeMinPower: new FormControl(this.controller?.properties.defaultChargeMinPower),
      forceChargeMinPower: new FormControl(this.controller?.properties.forceChargeMinPower),
      priority: new FormControl(this.controller?.properties.priority),
      // EnergySessionLimit as Wh value
      energySessionLimit: new FormControl(this.controller?.properties.energySessionLimit),
      // EnergySessionLimit as kWh value, for ion-range
      energySessionLimitKwh: new FormControl(Math.round(this.controller?.properties.energySessionLimit / 1000)),
      enabledCharging: new FormControl(this.isChargingEnabled),
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
  protected updateForceMinPower(event: CustomEvent, currentController: EdgeConfig.Component, numberOfPhases: number) {
    if (this.formGroup == null) {
      return;
    }
    const newMinChargePower = event.detail.value / numberOfPhases;

    const forceChargeMinPowerCtrl = this.formGroup.get("forceChargeMinPower");
    if (forceChargeMinPowerCtrl == null) {
      return;
    }

    forceChargeMinPowerCtrl.markAsDirty();
    forceChargeMinPowerCtrl.setValue(newMinChargePower);
  }

  /**
 * Updates the MinChargePower for Renault Zoe Charging Mode if activated in administration component
 */
  protected updateRenaultZoeConfig() {
    if (this.controller && this.evcsComponent?.properties["minHwCurrent"] == 10000) {

      const oldMinChargePower = this.controller.properties.forceChargeMinPower;
      const maxAllowedChargePower = 10 /* Ampere */ * 230; /* Volt */

      if (oldMinChargePower >= maxAllowedChargePower) {
        return;
      }

      const newMinChargePower = maxAllowedChargePower;

      if (this.edge == null) {
        return;
      }

      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: "forceChargeMinPower", value: newMinChargePower },
      ]).then(() => {
        this.setProperties(newMinChargePower);
      }).catch(reason => {
        this.setProperties(oldMinChargePower);
        console.warn(reason);
      });
    }
  }

  protected setProperties(value: number) {
    if (this.controller != null && this.controller.properties != null && this.controller.properties.forceChargeMinPower != null) {
      this.controller.properties.forceChargeMinPower = value;
    }
  }

  protected formatNumber(i: number) {
    const round = Math.ceil(i / 100) * 100;
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
      return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGING_STATION_DEACTIVATED");
    }

    if (plug == null) {
      if (state == null) {
        return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");
      }
    } else if (plug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED && this.chargePower != null && this.chargePower?.value > 450) {
      return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CABLE_NOT_CONNECTED");
    }
    switch (state) {
      case ChargeState.STARTING:
        return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.STARTING");
      case ChargeState.UNDEFINED:
      case ChargeState.ERROR:
        return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.ERROR");
      case ChargeState.READY_FOR_CHARGING:
        return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.READY_FOR_CHARGING");
      case ChargeState.NOT_READY_FOR_CHARGING:
        return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.NOT_READY_FOR_CHARGING");
      case ChargeState.AUTHORIZATION_REJECTED:
        return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");
      case ChargeState.CHARGING:
        return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGING");
      case ChargeState.ENERGY_LIMIT_REACHED:
        return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGE_LIMIT_REACHED");
      case ChargeState.CHARGING_FINISHED:
        return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CAR_FULL");
      default:
        return "";
    }
  }
}


enum ChargeState {
  UNDEFINED = -1,           //Undefined
  STARTING,                 //Starting
  NOT_READY_FOR_CHARGING,   //Not ready for Charging e.g. unplugged, X1 or "ena" not enabled, RFID not enabled,...
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
