// @ts-strict-ignore
import { ChangeDetectorRef, Component, Inject } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController, PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { EvcsUtils } from "src/app/shared/components/edge/utils/evcs-utils";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, CurrentData, EdgeConfig, Service, Utils, Websocket } from "src/app/shared/shared";

import { AdministrationComponent } from "../administration/administration.component";
import { PopoverComponent } from "../popover/popover";

type ChargeMode = "FORCE_CHARGE" | "EXCESS_POWER";
@Component({
  templateUrl: "./modal.html",
})
export class ModalComponent extends AbstractModal {

  public readonly CONVERT_MANUAL_ON_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(this.translate);
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(this.translate);
  protected controller: EdgeConfig.Component;
  protected evcsComponent: EdgeConfig.Component;
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
  protected helpKey: string;
  protected awaitingHysteresis: boolean;
  protected isReadWrite: boolean = true;

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

  public static getHelpKey(factoryId: string): string {
    switch (factoryId) {
      case "Evcs.Keba.KeContact":
        return "EVCS_KEBA_KECONTACT";
      case "Evcs.HardyBarth":
        return "EVCS_KEBA_KECONTACT";
      case "Evcs.IesKeywattSingle":
        return "EVCS_OCPP_IESKEYWATTSINGLE";
      default:
        return null;
    }
  }

  async presentPopover() {
    const popover = await this.popoverctrl.create({
      component: PopoverComponent,
      componentProps: {
        chargeMode: this.formGroup.controls["chargeMode"].value,
      },
    });
    return await popover.present();
  }

  async presentModal() {
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

    this.controller = this.config.getComponentsByFactory("Controller.Evcs")
      .find(element => "evcs.id" in element.properties && element.properties["evcs.id"] == this.component.id);

    this.evcsComponent = this.config.getComponent(this.component.id);
    this.helpKey = ModalComponent.getHelpKey(this.evcsComponent?.factoryId);

    return [
      // channels for modal component, subscribe here for better UX
      new ChannelAddress(this.component.id, this.getPowerChannelId()),
      new ChannelAddress(this.component.id, "Phases"),
      new ChannelAddress(this.component.id, "Plug"),
      new ChannelAddress(this.component.id, "Status"),
      new ChannelAddress(this.component.id, "State"),
      new ChannelAddress(this.component.id, "EnergySession"),
      new ChannelAddress(this.component.id, "MinimumHardwarePower"),
      new ChannelAddress(this.component.id, "MaximumHardwarePower"),
      new ChannelAddress(this.component.id, "SetChargePowerLimit"),
      new ChannelAddress(this.component.id, "_PropertyReadOnly"),
      new ChannelAddress(this.controller?.id, "_PropertyChargeMode"),
      new ChannelAddress(this.controller?.id, "_PropertyEnabledCharging"),
      new ChannelAddress(this.controller?.id, "_PropertyDefaultChargeMinPower"),
      new ChannelAddress(this.controller?.id, "AwaitingHysteresis"),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.isConnectionSuccessful = currentData.allComponents[this.component.id + "/State"] !== 3 ? true : false;
    this.awaitingHysteresis = currentData.allComponents[this.controller?.id + "/AwaitingHysteresis"];
    this.isReadWrite = !this.component.properties["readOnly"];
    // Do not change values after touching formControls
    if (this.formGroup?.pristine) {
      this.status = this.getState(this.controller ? currentData.allComponents[this.controller.id + "/_PropertyEnabledCharging"] === 1 : null, currentData.allComponents[this.component.id + "/Status"], currentData.allComponents[this.component.id + "/Plug"]);
      this.chargePower = Utils.convertChargeDischargePower(this.translate, currentData.allComponents[this.component.id + "/" + this.getPowerChannelId()]);
      this.chargePowerLimit = Utils.CONVERT_TO_WATT(this.formatNumber(currentData.allComponents[this.component.id + "/SetChargePowerLimit"]));
      this.state = currentData.allComponents[this.component.id + "/Status"];
      this.energySession = Utils.CONVERT_TO_WATTHOURS(currentData.allComponents[this.component.id + "/EnergySession"]);
      this.minChargePower = this.formatNumber(currentData.allComponents[this.component.id + "/MinimumHardwarePower"]);
      this.maxChargePower = this.formatNumber(currentData.allComponents[this.component.id + "/MaximumHardwarePower"]);
      this.numberOfPhases = currentData.allComponents[this.component.id + "/Phases"] ? currentData.allComponents[this.component.id + "/Phases"] : 3;
      this.defaultChargeMinPower = currentData.allComponents[this.controller?.id + "/_PropertyDefaultChargeMinPower"];
    }
  }

  protected override onIsInitialized(): void {
    this.subscription.add(this.formGroup?.controls["energyLimit"]?.valueChanges.subscribe(isEnergyLimit => {
      if (isEnergyLimit) {
        if (this.formGroup.controls["energySessionLimit"]?.value === 0) {
          this.formGroup.controls["energySessionLimit"].setValue(20000);
          this.formGroup.controls["energySessionLimit"].markAsDirty();
        } else {
          // energySessionLimit is already valid -> do nothing
        }
      } else {
        this.formGroup.controls["energySessionLimit"].setValue(0);
        this.formGroup.controls["energySessionLimit"].markAsDirty();
      }
    }));

    this.subscription.add(this.formGroup?.get("chargeMode").valueChanges.subscribe(chargeMode => {
      if (chargeMode === "OFF") {
        this.formGroup.get("enabledCharging").setValue(false);
        this.formGroup.get("chargeMode").markAsPristine(); // do not send chargeMode=OFF to Edge
      } else {
        this.formGroup.get("enabledCharging").setValue(true);
      }
      this.formGroup.get("enabledCharging").markAsDirty();
    }));

    this.subscription.add(this.formGroup?.get("minGuarantee").valueChanges.subscribe(minGuarantee => {
      if (minGuarantee) {
        this.formGroup.controls["defaultChargeMinPower"].setValue(1400 /* approx min power per phase */ * this.numberOfPhases);
      } else {
        this.formGroup.controls["defaultChargeMinPower"].setValue(0);
      }
      this.formGroup.controls["defaultChargeMinPower"].markAsDirty();
    }));

    // Convert FormGroup value in kWh to Wh for Component config
    this.subscription.add(this.formGroup?.get("energySessionLimitKwh").valueChanges.subscribe((newValue) => {
      this.formGroup.controls["energySessionLimit"].setValue(newValue * 1000);
      this.formGroup.controls["energySessionLimit"].markAsDirty();
      this.formGroup.controls["energySessionLimitKwh"].markAsPristine();
    }));
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
     * Updates the Min-Power of force charging
     *
     * @param event
     */
  protected updateForceMinPower(event: CustomEvent, currentController: EdgeConfig.Component, numberOfPhases: number) {

    const newMinChargePower = event.detail.value / numberOfPhases;
    this.formGroup.controls["forceChargeMinPower"].markAsDirty();
    this.formGroup.controls["forceChargeMinPower"].setValue(newMinChargePower);
  }

  /**
 * Updates the MinChargePower for Renault Zoe Charging Mode if activated in administration component
 */
  protected updateRenaultZoeConfig() {
    if (this.controller && this.evcsComponent.properties["minHwCurrent"] == 10000) {

      const oldMinChargePower = this.controller.properties.forceChargeMinPower;
      const maxAllowedChargePower = 10 /* Ampere */ * 230; /* Volt */

      if (oldMinChargePower < maxAllowedChargePower) {
        if (this.edge != null) {
          const newMinChargePower = maxAllowedChargePower;
          this.edge.updateComponentConfig(this.websocket, this.controller.id, [
            { name: "forceChargeMinPower", value: newMinChargePower },
          ]).then(() => {
            this.controller.properties.forceChargeMinPower = newMinChargePower;
          }).catch(reason => {
            this.controller.properties.forceChargeMinPower = oldMinChargePower;
            console.warn(reason);
          });
        }
      }
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
      return this.translate.instant("Edge.Index.Widgets.EVCS.chargingStationDeactivated");
    }

    if (plug == null) {
      if (state == null) {
        return this.translate.instant("Edge.Index.Widgets.EVCS.notCharging");
      }
    } else if (plug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED) {
      return this.translate.instant("Edge.Index.Widgets.EVCS.cableNotConnected");
    }
    switch (state) {
      case ChargeState.STARTING:
        return this.translate.instant("Edge.Index.Widgets.EVCS.starting");
      case ChargeState.UNDEFINED:
      case ChargeState.ERROR:
        return this.translate.instant("Edge.Index.Widgets.EVCS.error");
      case ChargeState.READY_FOR_CHARGING:
        return this.translate.instant("Edge.Index.Widgets.EVCS.readyForCharging");
      case ChargeState.NOT_READY_FOR_CHARGING:
        return this.translate.instant("Edge.Index.Widgets.EVCS.notReadyForCharging");
      case ChargeState.AUTHORIZATION_REJECTED:
        return this.translate.instant("Edge.Index.Widgets.EVCS.notCharging");
      case ChargeState.CHARGING:
        return this.translate.instant("Edge.Index.Widgets.EVCS.charging");
      case ChargeState.ENERGY_LIMIT_REACHED:
        return this.translate.instant("Edge.Index.Widgets.EVCS.chargeLimitReached");
      case ChargeState.CHARGING_FINISHED:
        return this.translate.instant("Edge.Index.Widgets.EVCS.carFull");
    }
  }

  private getPowerChannelId(): string {
    return EvcsUtils.getEvcsPowerChannelId(this.component, this.config, this.edge);
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
  CHARGING_FINISHED,         //Charging has finished
}

enum ChargePlug {
  UNDEFINED = -1,                           //Undefined
  UNPLUGGED,                                //Unplugged
  PLUGGED_ON_EVCS,                          //Plugged on EVCS
  PLUGGED_ON_EVCS_AND_LOCKED = 3,           //Plugged on EVCS and locked
  PLUGGED_ON_EVCS_AND_ON_EV = 5,            //Plugged on EVCS and on EV
  PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED = 7,  //Plugged on EVCS and on EV and locked
}
