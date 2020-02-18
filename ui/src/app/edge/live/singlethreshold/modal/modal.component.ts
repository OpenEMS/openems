import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge, EdgeConfig, ChannelAddress, Websocket } from '../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { Validators, FormBuilder, FormGroup, FormControl } from '@angular/forms';

type mode = 'ON' | 'AUTOMATIC' | 'OFF';
type inputMode = 'SOC' | 'GRIDSELL' | 'GRIDBUY' | 'PRODUCTION' | 'OTHER'

@Component({
  selector: SinglethresholdModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class SinglethresholdModalComponent {

  @Input() public edge: Edge;
  @Input() public config: EdgeConfig;
  @Input() public controller: EdgeConfig.Component;
  @Input() public outputChannel: ChannelAddress;
  @Input() public inputChannel: ChannelAddress;

  private static readonly SELECTOR = "singlethreshold-modal";

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
    public formBuilder: FormBuilder
  ) {
  }

  ngOnInit() {
    this.formGroup = this.formBuilder.group({
      minimumSwitchingTime: new FormControl(this.controller.properties.minimumSwitchingTime, Validators.compose([
        Validators.min(5),
        Validators.pattern('^[1-9][0-9]*$'),
        Validators.required
      ])),
      switchedLoadPower: new FormControl(this.controller.properties.switchedLoadPower, Validators.compose([
        Validators.pattern('^(?:[1-9][0-9]*|0)$'),
        Validators.required
      ])),
      threshold: new FormControl(this.getInputMode() == 'GRIDSELL' ? this.controller.properties.threshold * -1 : this.controller.properties.threshold, Validators.compose([
        Validators.min(1),
        Validators.pattern('^[1-9][0-9]*$'),
        Validators.required
      ])),
      inputMode: new FormControl(this.getInputMode()),
      invert: new FormControl(this.controller.properties.invert, Validators.requiredTrue)
    })
    this.minimumSwitchingTime = this.formGroup.controls['minimumSwitchingTime'];
    this.threshold = this.formGroup.controls['threshold'];
    this.switchedLoadPower = this.formGroup.controls['switchedLoadPower'];
    this.inputMode = this.formGroup.controls['inputMode'];
    this.invert = this.formGroup.controls['invert'];
  }

  getInputMode(): inputMode {
    if (this.controller.properties.inputChannelAddress == '_sum/GridActivePower' && this.controller.properties.threshold < 0) {
      return 'GRIDSELL';
    } else if (this.controller.properties.inputChannelAddress == '_sum/GridActivePower' && this.controller.properties.threshold > 0) {
      return 'GRIDBUY';
    } else if (this.controller.properties.inputChannelAddress == '_sum/ProductionActivePower') {
      return 'PRODUCTION';
    } else if (this.controller.properties.inputChannelAddress == '_sum/EssSoc') {
      return 'SOC';
    } else if (this.controller.properties.inputChannelAddress != null) {
      return 'OTHER';
    }
  }

  updateInputMode(event: CustomEvent) {
    let newThreshold: number = this.controller.properties.threshold;

    switch (event.detail.value) {
      case "SOC":
        this.inputMode.setValue('SOC');
        this.switchedLoadPower.setValue(0);
        this.switchedLoadPower.markAsDirty()
        if (Math.abs(this.controller.properties.threshold) < 0 || Math.abs(this.controller.properties.threshold) > 100) {
          newThreshold = 50;
          this.threshold.setValue(newThreshold);
          this.threshold.markAsDirty()
        } else if (this.controller.properties.threshold < 0) {
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
        if (this.controller.properties.threshold < 0) {
          newThreshold = this.formGroup.value.threshold;
          this.threshold.setValue(newThreshold);
          this.threshold.markAsDirty()
        }
        break;
      case "PRODUCTION":
        this.inputMode.setValue('PRODUCTION');
        this.switchedLoadPower.setValue(0);
        this.switchedLoadPower.markAsDirty()
        if (this.controller.properties.threshold < 0) {
          newThreshold = this.threshold.value;
          this.threshold.setValue(newThreshold);
          this.threshold.markAsDirty()
        }
        break;
    }
  }

  updateMode(event: CustomEvent) {
    let oldMode = this.controller.properties.mode;
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
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'mode', value: newMode }
      ]).then(() => {
        this.controller.properties.mode = newMode;
        this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
      }).catch(reason => {
        this.controller.properties.mode = oldMode;
        this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
        console.warn(reason);
      });
    }
  }

  convertToChannelAddress(inputMode: inputMode) {
    switch (inputMode) {
      case 'SOC':
        return '_sum/EssSoc'
      case 'GRIDBUY':
        return '_sum/GridActivePower'
      case 'GRIDSELL':
        return '_sum/GridActivePower'
      case 'PRODUCTION':
        return '_sum/ProductionActivePower'
    }
  }

  convertToInputMode(inputChannelAddress: string, threshold: number): inputMode {
    switch (inputChannelAddress) {
      case '_sum/EssSoc':
        return 'SOC'
      case '_sum/ProductionActivePower':
        return 'PRODUCTION'
      case '_sum/GridActivePower':
        if (threshold > 0) {
          return 'GRIDBUY'
        } else if (threshold < 0) {
          return 'GRIDSELL'
        }
    }
  }

  applyChanges() {
    if (this.minimumSwitchingTime.valid && this.threshold.valid && this.switchedLoadPower.valid) {
      if (this.threshold.value > this.switchedLoadPower.value) {
        let updateComponentArray = [];
        Object.keys(this.formGroup.controls).forEach((element, index) => {
          if (this.formGroup.controls[element].dirty) {
            // catch inputMode and convert it to inputChannelAddress
            if (Object.keys(this.formGroup.controls)[index] == 'inputMode') {
              updateComponentArray.push({ name: 'inputChannelAddress', value: this.convertToChannelAddress(this.formGroup.controls[element].value) })
            } else if (this.inputMode.value == 'GRIDSELL' && Object.keys(this.formGroup.controls)[index] == 'threshold') {
              this.formGroup.controls[element].setValue(this.formGroup.controls[element].value * -1);
              updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
            } else {
              updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
            }
          }
        });
        if (this.edge != null) {
          this.loading = true;
          this.edge.updateComponentConfig(this.websocket, this.controller.id, updateComponentArray).then(() => {
            this.controller.properties.minimumSwitchingTime = this.minimumSwitchingTime.value;
            this.controller.properties.threshold = this.inputMode.value == 'GRIDSELL' ? this.threshold.value * -1 : this.threshold.value;
            this.controller.properties.switchedLoadPower = this.switchedLoadPower.value;
            this.controller.properties.inputChannelAddress = this.convertToChannelAddress(this.inputMode.value) != this.controller.properties.inputChannelAddress ? this.convertToChannelAddress(this.inputMode.value) : this.controller.properties.inputChannelAddress;
            this.controller.properties.invert = this.invert.value;
            this.loading = false;
            this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
          }).catch(reason => {
            this.loading = false;
            this.minimumSwitchingTime.setValue(this.controller.properties.minimumSwitchingTime);
            this.threshold.setValue(this.controller.properties.threshold);
            this.switchedLoadPower.setValue(this.controller.properties.switchedLoadPower);
            this.inputMode.setValue(this.convertToInputMode(this.controller.properties.inputChannelAddress, this.controller.properties.treshold));
            this.invert.setValue(this.controller.properties.invert);
            this.loading = false;
            this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
            console.warn(reason);
          });
        }
        if (this.inputMode.value == 'GRIDSELL') {
          if (this.inputMode.dirty || this.threshold.dirty) {
            this.threshold.setValue(this.threshold.value * -1);
          }
        }
        this.formGroup.markAsPristine()
      } else {
        this.service.toast(this.translate.instant('Edge.Index.Widgets.Singlethreshold.relationError'), 'danger');
      }
    } else {
      this.service.toast(this.translate.instant('General.InputNotValid'), 'danger');
    }
  }
}