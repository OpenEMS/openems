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

  public inputMode: inputMode = null;
  public loading: boolean = false;

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
    public translate: TranslateService,
    public websocket: Websocket,
    public formBuilder: FormBuilder
  ) {
  }

  ngOnInit() {
    this.inputMode = this.getInputMode()
    this.formGroup = this.formBuilder.group({
      minimumSwitchingTime: new FormControl(this.controller.properties.minimumSwitchingTime, Validators.compose([
        Validators.min(10),
        Validators.pattern('^[0-9]*$'),
        Validators.required
      ])),
      threshold: new FormControl(this.inputMode == 'GRIDSELL' ? this.controller.properties.threshold * -1 : this.controller.properties.threshold, Validators.compose([
        Validators.pattern('^[0-9]*$'),
        Validators.required
      ])),
      switchedLoadPower: new FormControl(this.inputMode == 'GRIDSELL' ? this.controller.properties.switchedLoadPower * -1 : this.controller.properties.switchedLoadPower, Validators.compose([
        Validators.pattern('^[0-9]*$'),
        Validators.required
      ])),
      inputMode: new FormControl(this.controller.properties.inputChannelAddress),
      invert: new FormControl(this.controller.properties.invert, Validators.requiredTrue)
    })
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
        this.inputMode = 'SOC';
        this.formGroup.value.inputMode = 'SOC'
        this.formGroup.controls['switchedLoadPower'].setValue(0);
        this.formGroup.controls['switchedLoadPower'].markAsDirty()
        if (Math.abs(this.controller.properties.threshold) < 0 || Math.abs(this.controller.properties.threshold) > 100) {
          newThreshold = 50;
          this.formGroup.controls['threshold'].setValue(newThreshold);
          this.formGroup.controls['threshold'].markAsDirty()
        } else if (this.controller.properties.threshold < 0) {
          newThreshold = newThreshold;
          this.formGroup.controls['threshold'].setValue(newThreshold);
          this.formGroup.controls['threshold'].markAsDirty()
        }
        break;
      case "GRIDSELL":
        this.inputMode = 'GRIDSELL';
        this.formGroup.value.inputMode = 'GRIDSELL'
        this.formGroup.controls['threshold'].markAsDirty()
        this.formGroup.controls['switchedLoadPower'].markAsDirty()
        break;
      case "GRIDBUY":
        this.inputMode = 'GRIDBUY';
        this.formGroup.value.inputMode = 'GRIDBUY'
        this.formGroup.controls['switchedLoadPower'].markAsDirty()
        if (this.controller.properties.threshold < 0) {
          newThreshold = this.formGroup.value.threshold;
          this.formGroup.controls['threshold'].setValue(newThreshold);
          this.formGroup.controls['threshold'].markAsDirty()
        }
        break;
      case "PRODUCTION":
        this.inputMode = 'PRODUCTION';
        this.formGroup.value.inputMode = 'PRODUCTION'
        this.formGroup.controls['switchedLoadPower'].setValue(0);
        this.formGroup.controls['switchedLoadPower'].markAsDirty()
        if (this.controller.properties.threshold < 0) {
          newThreshold = this.formGroup.value.threshold;
          this.formGroup.controls['threshold'].setValue(newThreshold);
          this.formGroup.controls['threshold'].markAsDirty()
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

  showApplyChanges(): boolean {
    if (this.formGroup.dirty) {
      return true;
    } else {
      return false;
    }
  }

  applyChanges(currentController: EdgeConfig.Component) {
    // todo specific error messages depending on validation
    if (this.formGroup.controls['minimumSwitchingTime'].valid && this.formGroup.controls['threshold'].valid && this.formGroup.controls['switchedLoadPower'].valid) {
      let updateComponentArray = [];
      Object.keys(this.formGroup.controls).forEach((element, index) => {
        if (this.formGroup.controls[element].dirty) {
          // catch inputMode and convert it to inputChannelAddress
          if (Object.keys(this.formGroup.controls)[index] == 'inputMode') {
            updateComponentArray.push({ name: 'inputChannelAddress', value: this.convertToChannelAddress(this.formGroup.controls[element].value) })
          } else if (this.inputMode == 'GRIDSELL' && (Object.keys(this.formGroup.controls)[index] == 'threshold' || Object.keys(this.formGroup.controls)[index] == 'switchedLoadPower')) {
            this.formGroup.controls[element].setValue(this.formGroup.controls[element].value * -1);
            updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
          } else {
            updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
          }
        }
      });
      this.loading = true;
      if (this.edge != null) {
        this.edge.updateComponentConfig(this.websocket, this.controller.id, updateComponentArray).then(() => {
          this.controller.properties.minimumSwitchingTime = this.formGroup.value.minimumSwitchingTime;
          this.controller.properties.threshold = this.inputMode == 'GRIDSELL' ? this.formGroup.value.threshold * -1 : this.formGroup.value.threshold;
          this.controller.properties.switchedLoadPower = this.inputMode == 'GRIDSELL' ? this.formGroup.value.switchedLoadPower * -1 : this.formGroup.value.switchedLoadPower;
          this.controller.properties.inputChannelAddress = this.convertToChannelAddress(this.inputMode) != this.controller.properties.inputChannelAddress ? this.convertToChannelAddress(this.formGroup.value.inputMode) : this.controller.properties.inputChannelAddress;
          this.controller.properties.invert = this.formGroup.value.invert;
          this.loading = false;
          this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
        }).catch(reason => {
          this.loading = false;
          this.formGroup.controls['minimumSwitchingTime'].setValue(this.controller.properties.minimumSwitchingTime);
          this.formGroup.controls['threshold'].setValue(this.controller.properties.threshold);
          this.formGroup.controls['switchedLoadPower'].setValue(this.controller.properties.switchedLoadPower);
          this.formGroup.controls['inputMode'].setValue(this.convertToInputMode(this.controller.properties.inputChannelAddress, this.controller.properties.treshold));
          this.formGroup.controls['invert'].setValue(this.controller.properties.invert);
          this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
          console.warn(reason);
        });
      }
      if (this.inputMode == 'GRIDSELL') {
        if (this.formGroup.controls['inputMode'].dirty) {
          this.formGroup.controls['threshold'].setValue(this.formGroup.value.threshold * -1);
          this.formGroup.controls['switchedLoadPower'].setValue(this.formGroup.value.switchedLoadPower * -1);
        } else {
          if (this.formGroup.controls['threshold'].dirty) {
            this.formGroup.controls['threshold'].setValue(this.formGroup.value.threshold * -1);
          }
          if (this.formGroup.controls['switchedLoadPower'].dirty) {
            this.formGroup.controls['switchedLoadPower'].setValue(this.formGroup.value.switchedLoadPower * -1);
          }
        }
      }
      this.formGroup.markAsPristine()
    } else {
      this.service.toast('Input not valid', 'danger');
    }

  }
}