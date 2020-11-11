import { Component, Input } from '@angular/core';
import { Edge, EdgeConfig, Service, Websocket } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';

type ManualMode = 'FORCE_ON' | 'RECOMMENDATION' | 'REGULAR' | 'LOCK';

@Component({
  selector: HeatPumpModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class HeatPumpModalComponent {

  private static readonly SELECTOR = "heatpump-modal";

  @Input() public edge: Edge = null;
  @Input() public component: EdgeConfig.Component = null;

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
    console.log("COMPONENT", this.component)
    this.formGroup = this.formBuilder.group({
      // Manual
      manualState: new FormControl(this.component.properties.manualState),
      // Automatic

      // Modes
      recommendation: new FormControl(this.component.properties.automaticRecommendationCtrlEnabled),
      forceOn: new FormControl(this.component.properties.automaticForceOnCtrlEnabled),
      lock: new FormControl(this.component.properties.automaticLockCtrlEnabled),
      // Variables
      minimumSwitchingTime: new FormControl(this.component.properties.minimumSwitchingTime),
      gridBuyLock: new FormControl(this.component.properties.automaticLockGridBuyPower),
      socForceOn: new FormControl(this.component.properties.automaticForceOnSoc),
      gridSellForceOn: new FormControl(this.component.properties.automaticForceOnSurplusPower),
      gridSellRecommendation: new FormControl(this.component.properties.automaticRecommendationSurplusPower),
    })
  };

  updateControllerMode(event: CustomEvent) {
    let oldMode = this.component.properties['mode'];
    let newMode = event.detail.value;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.component.id, [
        { name: 'mode', value: newMode }
      ]).then(() => {
        this.component.properties.mode = newMode;
        this.formGroup.markAsPristine();
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        this.component.properties.mode = oldMode;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
        console.warn(reason);
      });
    }
  }

  public updateManualMode(state: ManualMode) {
    this.formGroup.controls['manualState'].setValue(state);
    this.formGroup.controls['manualState'].markAsDirty()
  }

  public applyChanges() {
    let updateComponentArray = [];
    Object.keys(this.formGroup.controls).forEach((element, index) => {
      if (this.formGroup.controls[element].dirty) {
        updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
      }
    });

    if (this.edge != null) {
      this.loading = true;
      this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
        this.component.properties.manualState = this.formGroup.value.manualState;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
        this.loading = false;
      }).catch(reason => {
        this.formGroup.controls['minTime'].setValue(this.component.properties.manualState);
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason, 'danger');
        this.loading = false;
        console.warn(reason);
      });
      this.formGroup.markAsPristine();
    }
  }

}