import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ModalController, PopoverController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, CurrentData, EdgeConfig, Service, Utils, Websocket } from 'src/app/shared/shared';

import { AdministrationComponent } from '../administration/administration.component';
import { PopoverComponent } from '../popover/popover';

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER';
@Component({
  templateUrl: './modal.html'
})
export class ModalComponent extends AbstractModal {

  public readonly CONVERT_MANUAL_ON_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(this.translate);
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(this.translate);
  protected controller: EdgeConfig.Component;
  protected evcsComponent: EdgeConfig.Component;
  protected isConnectionSuccessful: boolean = false;
  protected readonly emptyValue: string = '-';
  protected status: string;
  protected chargePowerLimit: string;
  protected chargePower: { name: string; value: number; };
  protected state: string = '';
  protected energySession: string;
  protected minChargePower: number;
  protected maxChargePower: number;
  protected numberOfPhases: number;
  protected defaultChargeMinPower: number;
  protected minGuarantee: boolean;
  protected energyLimit: boolean;
  protected chargeMode: ChargeMode = null;
  protected isEnergySinceBeginningAllowed: boolean = false;
  protected isChargingEnabled: boolean = false;
  protected sessionLimit: number;
  protected helpKey: string;

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

  protected override getChannelAddresses(): ChannelAddress[] {

    this.controller = this.config.getComponentsByFactory("Controller.Evcs")
      .find(element => "evcs.id" in element.properties && element.properties["evcs.id"] == this.component.id);

    this.evcsComponent = this.config.getComponent(this.component.id);
    this.helpKey = ModalComponent.getHelpKey(this.evcsComponent?.factoryId);
    return [
      // channels for modal component, subscribe here for better UX
      new ChannelAddress(this.component.id, 'ChargePower'),
      new ChannelAddress(this.component.id, 'Phases'),
      new ChannelAddress(this.component.id, 'Plug'),
      new ChannelAddress(this.component.id, 'Status'),
      new ChannelAddress(this.component.id, 'State'),
      new ChannelAddress(this.component.id, 'EnergySession'),
      new ChannelAddress(this.component.id, 'MinimumHardwarePower'),
      new ChannelAddress(this.component.id, 'MaximumHardwarePower'),
      new ChannelAddress(this.component.id, 'SetChargePowerLimit'),
      new ChannelAddress(this.controller.id, '_PropertyChargeMode'),
      new ChannelAddress(this.controller.id, '_PropertyEnabledCharging')
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.isConnectionSuccessful = currentData.allComponents[this.component.id + '/State'] !== 3 ? true : false;
    // Do not change values after touching formControls 
    if (this.formGroup?.pristine) {
      this.status = this.getState(currentData.allComponents[this.component.id + "/Status"], currentData.allComponents[this.component.id + "/Plug"]);
      this.chargePower = Utils.convertChargeDischargePower(this.translate, currentData.allComponents[this.component.id + "/ChargePower"]);
      this.chargePowerLimit = Utils.CONVERT_TO_WATT(this.formatNumber(currentData.allComponents[this.component.id + "/SetChargePowerLimit"]));
      this.state = currentData.allComponents[this.component.id + "/Status"];
      this.energySession = Utils.CONVERT_TO_WATT(currentData.allComponents[this.component.id + "/EnergySession"]);
      this.minChargePower = this.formatNumber(currentData.allComponents[this.component.id + '/MinimumHardwarePower']);
      this.maxChargePower = this.formatNumber(currentData.allComponents[this.component.id + '/MaximumHardwarePower']);
      this.numberOfPhases = currentData.allComponents[this.component.id + '/Phases'] ? currentData.allComponents[this.component.id + '/Phases'] : 3;
      this.defaultChargeMinPower = currentData.allComponents[this.controller.id + '/_PropertyDefaultChargeMinPower'];
    }

    if (this.formGroup?.controls['defaultChargeMinPower']?.dirty) {  //  update the state of the toggle which renders the minimum charge power
      if (this.defaultChargeMinPower == null || this.defaultChargeMinPower == 0) {
        this.formGroup.controls['defaultChargeMinPower'].setValue(this.numberOfPhases != undefined ? 1400 * this.numberOfPhases : 4200);
        this.formGroup.controls['defaultChargeMinPower'].markAsPristine();
      }
    }
    this.minGuarantee = this.defaultChargeMinPower > 0;

    // If EnergyLimit changes, setDefaultValue 20000 for true and 0 for false
    this.formGroup?.controls['energyLimit']?.valueChanges.subscribe(event => {
      if (event && this.formGroup.controls['energySessionLimit']?.value === 0) {
        this.formGroup.controls['energySessionLimit'].setValue(20000);
        this.formGroup.controls['energySessionLimit'].markAsDirty();
      }
      if (!event) {
        this.formGroup.controls['energySessionLimit'].setValue(0);
        this.formGroup.controls['energySessionLimit'].markAsDirty();
      }
    });

    this.formGroup?.get('chargeMode').valueChanges.subscribe((newValue) => {
      // Here, you can check the 'newValue' and update the form control accordingly
      if (newValue === 'OFF') {
        this.formGroup.get('enabledCharging').setValue(false);
        this.formGroup.get('enabledCharging').markAsDirty();
        this.formGroup.get('chargeMode').markAsPristine();
      } else {
        this.formGroup.get('enabledCharging').setValue(true);
        this.formGroup.get('enabledCharging').markAsDirty();
      }
    });
  }

  protected override getFormGroup(): FormGroup {
    return this.formBuilder.group({
      chargeMode: new FormControl(this.controller.properties.enabledCharging == false ? 'OFF' : this.controller.properties.chargeMode),
      energyLimit: new FormControl(this.controller.properties['energySessionLimit'] > 0),
      minGuarantee: new FormControl(this.minGuarantee),
      defaultChargeMinPower: new FormControl(this.controller.properties.defaultChargeMinPower),
      forceChargeMinPower: new FormControl(this.controller.properties.forceChargeMinPower),
      priority: new FormControl(this.controller.properties.priority),
      energySessionLimit: new FormControl(this.controller.properties.energySessionLimit),
      enabledCharging: new FormControl(this.isChargingEnabled)
    });
  }

  /**
     * Updates the Min-Power of force charging
     *
     * @param event
     */
  protected updateForceMinPower(event: CustomEvent, currentController: EdgeConfig.Component, numberOfPhases: number) {

    let newMinChargePower = event.detail.value / numberOfPhases;
    this.formGroup.controls['forceChargeMinPower'].markAsDirty();
    this.formGroup.controls['forceChargeMinPower'].setValue(newMinChargePower);
  }

  /**
 * Updates the MinChargePower for Renault Zoe Charging Mode if activated in administration component
 */
  protected updateRenaultZoeConfig() {
    if (this.evcsComponent.properties['minHwCurrent'] == 10000) {

      let oldMinChargePower = this.controller.properties.forceChargeMinPower;
      let maxAllowedChargePower = 10 /* Ampere */ * 230; /* Volt */

      if (oldMinChargePower < maxAllowedChargePower) {
        if (this.edge != null) {
          let newMinChargePower = maxAllowedChargePower;
          this.edge.updateComponentConfig(this.websocket, this.controller.id, [
            { name: 'forceChargeMinPower', value: newMinChargePower }
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

  /**
  * Returns the state of the EVCS
  * 
  * @param state the state
  * @param plug the plug
  * 
  */
  private getState(state: number, plug: number): string {
    if (this.controller != null) {
      if (this.controller.properties.enabledCharging != null && this.controller.properties.enabledCharging == false) {
        return this.translate.instant('Edge.Index.Widgets.EVCS.chargingStationDeactivated');
      }
    }

    if (plug == null) {
      if (state == null) {
        return this.translate.instant('Edge.Index.Widgets.EVCS.notCharging');
      }
    } else if (plug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED) {
      return this.translate.instant('Edge.Index.Widgets.EVCS.cableNotConnected');
    }
    switch (state) {
      case ChargeState.STARTING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.starting');
      case ChargeState.UNDEFINED:
      case ChargeState.ERROR:
        return this.translate.instant('Edge.Index.Widgets.EVCS.error');
      case ChargeState.READY_FOR_CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.readyForCharging');
      case ChargeState.NOT_READY_FOR_CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.notReadyForCharging');
      case ChargeState.AUTHORIZATION_REJECTED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.notCharging');
      case ChargeState.CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.charging');
      case ChargeState.ENERGY_LIMIT_REACHED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.chargeLimitReached');
      case ChargeState.CHARGING_FINISHED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.carFull');
    }
  }

  protected formatNumber(i: number) {
    let round = Math.ceil(i / 100) * 100;
    return round;
  }

  async presentPopover() {
    const popover = await this.popoverctrl.create({
      component: PopoverComponent,
      componentProps: {
        chargeMode: this.formGroup.controls['chargeMode'].value
      }
    });
    return await popover.present();
  }

  async presentModal() {
    const modal = await this.detailViewController.create({
      component: AdministrationComponent,
      componentProps: {
        evcsComponent: this.evcsComponent,
        edge: this.edge
      }
    });
    modal.onDidDismiss().then(() => {
      this.updateRenaultZoeConfig();
    });
    return await modal.present();
  }

  public static getHelpKey(factoryId: string): string {
    switch (factoryId) {
      case 'Evcs.Keba.KeContact':
        return 'EVCS_KEBA_KECONTACT';
      case 'Evcs.HardyBarth':
        return 'EVCS_KEBA_KECONTACT';
      case 'Evcs.IesKeywattSingle':
        return 'EVCS_OCPP_IESKEYWATTSINGLE';
      default:
        return null;
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
  CHARGING_FINISHED         //Charging has finished
}

enum ChargePlug {
  UNDEFINED = -1,                           //Undefined
  UNPLUGGED,                                //Unplugged
  PLUGGED_ON_EVCS,                          //Plugged on EVCS
  PLUGGED_ON_EVCS_AND_LOCKED = 3,           //Plugged on EVCS and locked
  PLUGGED_ON_EVCS_AND_ON_EV = 5,            //Plugged on EVCS and on EV
  PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED = 7  //Plugged on EVCS and on EV and locked
}
