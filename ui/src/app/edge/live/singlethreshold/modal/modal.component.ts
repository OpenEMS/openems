import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge, EdgeConfig, ChannelAddress, Websocket } from '../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { Validators, FormBuilder, FormGroup, FormControl, AbstractControl } from '@angular/forms';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

type Mode = 'ON' | 'AUTOMATIC' | 'OFF';
type InputMode = 'SOC' | 'GRIDSELL' | 'GRIDBUY' | 'PRODUCTION' | 'OTHER'

@Component({
  selector: SinglethresholdModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class SinglethresholdModalComponent {

  @Input() public edge: Edge | null = null;
  @Input() public config: EdgeConfig | null = null;
  @Input() public component: EdgeConfig.Component | null = null;
  @Input() public outputChannel: ChannelAddress | null = null;
  @Input() public inputChannel: ChannelAddress | null = null;

  private static readonly SELECTOR = "singlethreshold-modal";

  public formGroup: FormGroup | null = null;

  public loading: boolean = false;

  public minimumSwitchingTime: AbstractControl | null = null;
  public threshold: AbstractControl | null = null;
  public switchedLoadPower: AbstractControl | null = null;
  public inputMode: AbstractControl | null = null;
  public invert: AbstractControl | null = null;

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
    public translate: TranslateService,
    public websocket: Websocket,
    public formBuilder: FormBuilder
  ) {
  }

  ngOnInit() {
    if (this.component != null) {
      this.formGroup = this.formBuilder.group({
        minimumSwitchingTime: new FormControl(this.component.properties.minimumSwitchingTime, Validators.compose([
          Validators.min(5),
          Validators.pattern('^[1-9][0-9]*$'),
          Validators.required
        ])),
        switchedLoadPower: new FormControl(this.component.properties.switchedLoadPower, Validators.compose([
          Validators.pattern('^(?:[1-9][0-9]*|0)$'),
          Validators.required
        ])),
        threshold: new FormControl(this.getInputMode() == 'GRIDSELL' ? this.component.properties.threshold * -1 : this.component.properties.threshold, Validators.compose([
          Validators.min(1),
          Validators.pattern('^[1-9][0-9]*$'),
          Validators.required
        ])),
        inputMode: new FormControl(this.getInputMode()),
        invert: new FormControl(this.component.properties.invert, Validators.requiredTrue)
      })
      this.minimumSwitchingTime = this.formGroup.controls['minimumSwitchingTime'];
      this.threshold = this.formGroup.controls['threshold'];
      this.switchedLoadPower = this.formGroup.controls['switchedLoadPower'];
      this.inputMode = this.formGroup.controls['inputMode'];
      this.invert = this.formGroup.controls['invert'];
    }
  }

  private getInputMode(): InputMode | null {
    let response: InputMode | null = null;
    if (this.component != null) {
      if (this.component.properties.inputChannelAddress == '_sum/GridActivePower' && this.component.properties.threshold < 0) {
        response = 'GRIDSELL';
      } else if (this.component.properties.inputChannelAddress == '_sum/GridActivePower' && this.component.properties.threshold > 0) {
        response = 'GRIDBUY';
      } else if (this.component.properties.inputChannelAddress == '_sum/ProductionActivePower') {
        response = 'PRODUCTION';
      } else if (this.component.properties.inputChannelAddress == '_sum/EssSoc') {
        response = 'SOC';
      } else if (this.component.properties.inputChannelAddress != null) {
        response = 'OTHER';
      }
    }
    return response;
  }

  public updateInputMode(event: CustomEvent) {
    if (this.component && this.inputMode && this.switchedLoadPower && this.threshold && this.formGroup != null) {
      let newThreshold: number = this.component.properties.threshold;
      switch (event.detail.value as InputMode) {
        case "SOC":
          this.inputMode.setValue('SOC');
          this.switchedLoadPower.setValue(0);
          this.switchedLoadPower.markAsDirty()
          if (Math.abs(this.component.properties.threshold) < 0 || Math.abs(this.component.properties.threshold) > 100) {
            newThreshold = 50;
            this.threshold.setValue(newThreshold);
            this.threshold.markAsDirty()
          } else if (this.component.properties.threshold < 0) {
            newThreshold = newThreshold;
            this.threshold.setValue(newThreshold);
            this.threshold.markAsDirty()
          }
          break;
        case "GRIDSELL":
          this.inputMode.setValue('GRIDSELL');
          this.threshold.markAsDirty()
          this.switchedLoadPower.markAsDirty()
          break;
        case "GRIDBUY":
          this.inputMode.setValue('GRIDBUY');
          this.switchedLoadPower.markAsDirty()
          if (this.component.properties.threshold < 0) {
            newThreshold = this.formGroup.value.threshold;
            this.threshold.setValue(newThreshold);
            this.threshold.markAsDirty()
          }
          break;
        case "PRODUCTION":
          this.inputMode.setValue('PRODUCTION');
          this.switchedLoadPower.setValue(0);
          this.switchedLoadPower.markAsDirty()
          if (this.component.properties.threshold < 0) {
            newThreshold = this.threshold.value;
            this.threshold.setValue(newThreshold);
            this.threshold.markAsDirty()
          }
          break;
      }
    }
  }

  public updateMode(event: CustomEvent) {
    if (this.component != null) {
      let oldMode = this.component.properties.mode;
      let newMode: Mode;

      switch (event.detail.value as Mode) {
        case 'ON':
          newMode = 'ON';
          break;
        case 'OFF':
          newMode = 'OFF';
          break;
        case 'AUTOMATIC':
          newMode = 'AUTOMATIC';
          break;
      }

      if (this.edge != null) {
        this.edge.updateComponentConfig(this.websocket, this.component.id, [
          { name: 'mode', value: newMode }
        ]).then(() => {
          if (this.component != null) {
            this.component.properties.mode = newMode;
          }
          this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
        }).catch(reason => {
          if (this.component != null) {
            this.component.properties.mode = oldMode;
          } this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
          console.warn(reason);
        });
      }
    }
  }

  private convertToChannelAddress(inputMode: InputMode): string {
    let response: string = '';
    switch (inputMode) {
      case 'SOC':
        response = '_sum/EssSoc';
        break;
      case 'GRIDBUY':
        response = '_sum/GridActivePower';
        break;
      case 'GRIDSELL':
        response = '_sum/GridActivePower';
        break;
      case 'PRODUCTION':
        response = '_sum/ProductionActivePower';
        break;
    }
    return response;
  }

  private convertToInputMode(inputChannelAddress: string, threshold: number): InputMode | null {
    let response: InputMode | null = null;
    switch (inputChannelAddress) {
      case '_sum/EssSoc':
        response = 'SOC'
        break;
      case '_sum/ProductionActivePower':
        response = 'PRODUCTION'
        break;
      case '_sum/GridActivePower':
        if (threshold > 0) {
          response = 'GRIDBUY'
          break;
        } else if (threshold < 0) {
          response = 'GRIDSELL'
          break;
        }
    }
    return response;
  }

  applyChanges() {
    if (this.minimumSwitchingTime && this.threshold && this.switchedLoadPower) {
      if (this.minimumSwitchingTime.valid && this.threshold.valid && this.switchedLoadPower.valid) {
        if (this.threshold.value > this.switchedLoadPower.value) {
          let updateComponentArray: DefaultTypes.UpdateComponentObject[] = [];
          if (this.formGroup != null) {
            Object.keys(this.formGroup.controls).forEach((element, index) => {
              if (this.formGroup != null && this.formGroup.controls[element].dirty) {
                // catch inputMode and convert it to inputChannelAddress
                if (Object.keys(this.formGroup.controls)[index] == 'inputMode') {
                  updateComponentArray.push({ name: 'inputChannelAddress', value: this.convertToChannelAddress(this.formGroup.controls[element].value) })
                } else if (this.inputMode != null && this.inputMode.value == 'GRIDSELL' && Object.keys(this.formGroup.controls)[index] == 'threshold') {
                  this.formGroup.controls[element].setValue(this.formGroup.controls[element].value * -1);
                  updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                } else {
                  updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                }
              }
            });
            if (this.edge && this.component != null) {
              this.loading = true;
              this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                if (this.component && this.minimumSwitchingTime && this.inputMode
                  && this.threshold && this.inputMode && this.switchedLoadPower && this.invert != null) {
                  this.component.properties.minimumSwitchingTime = this.minimumSwitchingTime.value;
                  this.component.properties.threshold = this.inputMode.value == 'GRIDSELL' ? this.threshold.value * -1 : this.threshold.value;
                  this.component.properties.switchedLoadPower = this.switchedLoadPower.value;
                  this.component.properties.inputChannelAddress = this.convertToChannelAddress(this.inputMode.value) != this.component.properties.inputChannelAddress ? this.convertToChannelAddress(this.inputMode.value) : this.component.properties.inputChannelAddress;
                  this.component.properties.invert = this.invert.value;
                }
                this.loading = false;
                this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
              }).catch(reason => {
                if (this.component && this.minimumSwitchingTime && this.inputMode
                  && this.threshold && this.inputMode && this.switchedLoadPower && this.invert != null) {
                  this.minimumSwitchingTime.setValue(this.component.properties.minimumSwitchingTime);
                  this.threshold.setValue(this.component.properties.threshold);
                  this.switchedLoadPower.setValue(this.component.properties.switchedLoadPower);
                  this.inputMode.setValue(this.convertToInputMode(this.component.properties.inputChannelAddress, this.component.properties.treshold));
                  this.invert.setValue(this.component.properties.invert);
                }
                this.loading = false;
                this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                console.warn(reason);
              });
            }
            if (this.inputMode != null && this.inputMode.value == 'GRIDSELL') {
              if (this.inputMode.dirty || this.threshold.dirty) {
                this.threshold.setValue(this.threshold.value * -1);
              }
            }
            this.formGroup.markAsPristine()
          } else {
            this.service.toast(this.translate.instant('Edge.Index.Widgets.Singlethreshold.relationError'), 'danger');
          }
        } else {
          this.service.toast(this.translate.instant('General.inputNotValid'), 'danger');
        }
      }
    }
  }
}