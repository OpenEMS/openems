import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';

type mode = 'ON' | 'AUTOMATIC' | 'OFF';
type inputMode = 'SOC' | 'GRIDSELL' | 'GRIDBUY' | 'PRODUCTION' | 'OTHER'

@Component({
  selector: 'Io_ChannelSingleThresholdModalComponent',
  templateUrl: './modal.component.html',
})
export class Controller_Io_ChannelSingleThresholdModalComponent implements OnInit {

  @Input() public edge: Edge;
  @Input() public config: EdgeConfig;
  @Input() public component: EdgeConfig.Component;
  @Input() public outputChannel: ChannelAddress | null = null;
  @Input() public inputChannel: ChannelAddress;

  public formGroup: FormGroup;

  public loading: boolean = false;

  public minimumSwitchingTime = null;
  public threshold = null;
  public switchedLoadPower = null;
  public inputMode = null;
  public invert = null;

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
    public translate: TranslateService,
    public websocket: Websocket,
    public formBuilder: FormBuilder,
  ) {
  }

  ngOnInit() {
    this.formGroup = this.formBuilder.group({
      minimumSwitchingTime: new FormControl(this.component.properties.minimumSwitchingTime, Validators.compose([
        Validators.min(5),
        Validators.pattern('^[1-9][0-9]*$'),
        Validators.required,
      ])),
      switchedLoadPower: new FormControl(this.component.properties.switchedLoadPower, Validators.compose([
        Validators.pattern('^(?:[1-9][0-9]*|0)$'),
        Validators.required,
      ])),
      threshold: new FormControl(this.getInputMode() == 'GRIDSELL' ? this.component.properties.threshold * -1 : this.component.properties.threshold, Validators.compose([
        Validators.min(1),
        Validators.pattern('^[1-9][0-9]*$'),
        Validators.required,
      ])),
      inputMode: new FormControl(this.getInputMode()),
      invert: new FormControl(this.component.properties.invert, Validators.requiredTrue),
    });
    this.minimumSwitchingTime = this.formGroup.controls['minimumSwitchingTime'];
    this.threshold = this.formGroup.controls['threshold'];
    this.switchedLoadPower = this.formGroup.controls['switchedLoadPower'];
    this.inputMode = this.formGroup.controls['inputMode'];
    this.invert = this.formGroup.controls['invert'];
  }

  private getInputMode(): inputMode {
    if (this.component.properties.inputChannelAddress == '_sum/GridActivePower' && this.component.properties.threshold < 0) {
      return 'GRIDSELL';
    } else if (this.component.properties.inputChannelAddress == '_sum/GridActivePower' && this.component.properties.threshold > 0) {
      return 'GRIDBUY';
    } else if (this.component.properties.inputChannelAddress == '_sum/ProductionActivePower') {
      return 'PRODUCTION';
    } else if (this.component.properties.inputChannelAddress == '_sum/EssSoc') {
      return 'SOC';
    } else if (this.component.properties.inputChannelAddress != null) {
      return 'OTHER';
    }
  }

  public updateInputMode(event: CustomEvent) {
    let newThreshold: number = this.component.properties.threshold;

    switch (event.detail.value) {
      case "SOC":
        this.inputMode.setValue('SOC');
        this.switchedLoadPower.setValue(0);
        this.switchedLoadPower.markAsDirty();
        if (Math.abs(this.component.properties.threshold) < 0 || Math.abs(this.component.properties.threshold) > 100) {
          newThreshold = 50;
          this.threshold.setValue(newThreshold);
          this.threshold.markAsDirty();
        } else if (this.component.properties.threshold < 0) {
          newThreshold = newThreshold;
          this.threshold.setValue(newThreshold);
          this.threshold.markAsDirty();
        }
        break;
      case "GRIDSELL":
        this.inputMode.setValue('GRIDSELL');
        this.threshold.markAsDirty();
        this.switchedLoadPower.markAsDirty();
        break;
      case "GRIDBUY":
        this.inputMode.setValue('GRIDBUY');
        this.switchedLoadPower.markAsDirty();
        if (this.component.properties.threshold < 0) {
          newThreshold = this.formGroup.value.threshold;
          this.threshold.setValue(newThreshold);
          this.threshold.markAsDirty();
        }
        break;
      case "PRODUCTION":
        this.inputMode.setValue('PRODUCTION');
        this.switchedLoadPower.setValue(0);
        this.switchedLoadPower.markAsDirty();
        if (this.component.properties.threshold < 0) {
          newThreshold = this.threshold.value;
          this.threshold.setValue(newThreshold);
          this.threshold.markAsDirty();
        }
        break;
    }
  }

  public updateMode(event: CustomEvent) {
    const oldMode = this.component.properties.mode;
    let newMode: mode;

    switch (event.detail.value) {
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
        { name: 'mode', value: newMode },
      ]).then(() => {
        this.component.properties.mode = newMode;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        this.component.properties.mode = oldMode;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
        console.warn(reason);
      });
    }
  }

  private convertToChannelAddress(inputMode: inputMode): string {
    switch (inputMode) {
      case 'SOC':
        return '_sum/EssSoc';
      case 'GRIDBUY':
        return '_sum/GridActivePower';
      case 'GRIDSELL':
        return '_sum/GridActivePower';
      case 'PRODUCTION':
        return '_sum/ProductionActivePower';
    }
  }

  private convertToInputMode(inputChannelAddress: string, threshold: number): inputMode {
    switch (inputChannelAddress) {
      case '_sum/EssSoc':
        return 'SOC';
      case '_sum/ProductionActivePower':
        return 'PRODUCTION';
      case '_sum/GridActivePower':
        if (threshold > 0) {
          return 'GRIDBUY';
        } else if (threshold < 0) {
          return 'GRIDSELL';
        }
    }
  }

  public applyChanges(): void {
    if (this.edge != null) {
      if (this.edge.roleIsAtLeast('owner')) {
        if (this.minimumSwitchingTime.valid && this.threshold.valid && this.switchedLoadPower.valid) {
          if (this.threshold.value > this.switchedLoadPower.value) {
            const updateComponentArray = [];
            Object.keys(this.formGroup.controls).forEach((element, index) => {
              if (this.formGroup.controls[element].dirty) {
                // catch inputMode and convert it to inputChannelAddress
                if (Object.keys(this.formGroup.controls)[index] == 'inputMode') {
                  updateComponentArray.push({ name: 'inputChannelAddress', value: this.convertToChannelAddress(this.formGroup.controls[element].value) });
                } else if (this.inputMode.value == 'GRIDSELL' && Object.keys(this.formGroup.controls)[index] == 'threshold') {
                  this.formGroup.controls[element].setValue(this.formGroup.controls[element].value * -1);
                  updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value });
                } else {
                  updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value });
                }
              }
            });
            this.loading = true;
            this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
              this.component.properties.minimumSwitchingTime = this.minimumSwitchingTime.value;
              this.component.properties.threshold = this.inputMode.value == 'GRIDSELL' ? this.threshold.value * -1 : this.threshold.value;
              this.component.properties.switchedLoadPower = this.switchedLoadPower.value;
              this.component.properties.inputChannelAddress = this.convertToChannelAddress(this.inputMode.value) != this.component.properties.inputChannelAddress ? this.convertToChannelAddress(this.inputMode.value) : this.component.properties.inputChannelAddress;
              this.component.properties.invert = this.invert.value;
              this.loading = false;
              this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
            }).catch(reason => {
              this.loading = false;
              this.minimumSwitchingTime.setValue(this.component.properties.minimumSwitchingTime);
              this.threshold.setValue(this.component.properties.threshold);
              this.switchedLoadPower.setValue(this.component.properties.switchedLoadPower);
              this.inputMode.setValue(this.convertToInputMode(this.component.properties.inputChannelAddress, this.component.properties.threshold));
              this.invert.setValue(this.component.properties.invert);
              this.loading = false;
              this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
              console.warn(reason);
            });
            if (this.inputMode.value == 'GRIDSELL') {
              if (this.inputMode.dirty || this.threshold.dirty) {
                this.threshold.setValue(this.threshold.value * -1);
              }
            }
            this.formGroup.markAsPristine();
          } else {
            this.service.toast(this.translate.instant('Edge.Index.Widgets.Singlethreshold.relationError'), 'danger');
          }
        } else {
          this.service.toast(this.translate.instant('General.inputNotValid'), 'danger');
        }
      } else {
        this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
      }
    }
  }
}
