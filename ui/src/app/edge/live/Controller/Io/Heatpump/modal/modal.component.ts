// @ts-strict-ignore
import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';

type ManualMode = 'FORCE_ON' | 'RECOMMENDATION' | 'REGULAR' | 'LOCK';
type AutomaticEnableMode = 'automaticRecommendationCtrlEnabled' | 'automaticForceOnCtrlEnabled' | 'automaticLockCtrlEnabled'

@Component({
  selector: 'heatpump-modal',
  templateUrl: './modal.component.html',
})
export class Controller_Io_HeatpumpModalComponent implements OnInit {

  @Input() public edge: Edge | null = null;
  @Input() public component: EdgeConfig.Component | null = null;

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
    this.formGroup = this.formBuilder.group({
      // Manual
      manualState: new FormControl(this.component.properties.manualState),
      // Automatic
      automaticForceOnCtrlEnabled: new FormControl(this.component.properties.automaticForceOnCtrlEnabled),
      automaticForceOnSoc: new FormControl(this.component.properties.automaticForceOnSoc),
      automaticForceOnSurplusPower: new FormControl(this.component.properties.automaticForceOnSurplusPower),
      automaticLockCtrlEnabled: new FormControl(this.component.properties.automaticLockCtrlEnabled),
      automaticLockGridBuyPower: new FormControl(this.component.properties.automaticLockGridBuyPower),
      automaticLockSoc: new FormControl(this.component.properties.automaticLockSoc),
      automaticRecommendationCtrlEnabled: new FormControl(this.component.properties.automaticRecommendationCtrlEnabled),
      automaticRecommendationSurplusPower: new FormControl(this.component.properties.automaticRecommendationSurplusPower),
      minimumSwitchingTime: new FormControl(this.component.properties.minimumSwitchingTime),
    });
  }

  public updateControllerMode(event: CustomEvent) {
    const oldMode = this.component.properties['mode'];
    const newMode = event.detail.value;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.component.id, [
        { name: 'mode', value: newMode },
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

  public updateAutomaticEnableMode(isTrue: boolean, state: AutomaticEnableMode) {
    this.formGroup.controls[state].setValue(isTrue);
    this.formGroup.controls[state].markAsDirty();
  }

  public updateManualMode(state: ManualMode) {
    this.formGroup.controls['manualState'].setValue(state);
    this.formGroup.controls['manualState'].markAsDirty();
  }

  public applyChanges() {
    if (this.edge != null) {
      if (this.edge.roleIsAtLeast('owner')) {
        if (this.formGroup.controls['automaticRecommendationSurplusPower'].value < this.formGroup.controls['automaticForceOnSurplusPower'].value) {
          const updateComponentArray = [];
          Object.keys(this.formGroup.controls).forEach((element, index) => {
            if (this.formGroup.controls[element].dirty) {
              updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value });
            }
          });
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
        } else {
          this.service.toast(this.translate.instant('Edge.Index.Widgets.HeatPump.relationError'), 'danger');
        }
      } else {
        this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
      }
    }
  }
}
