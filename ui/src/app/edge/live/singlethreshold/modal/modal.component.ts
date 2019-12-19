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
    this.formGroup = this.formBuilder.group({
      minimumSwitchingTime: new FormControl(this.controller.properties.minimumSwitchingTime, Validators.compose([
        Validators.min(10),
        Validators.pattern('^[0-9]*$'),
      ])),
      threshold: new FormControl(this.controller.properties.threshold),
      switchedLoadPower: new FormControl(this.controller.properties.switchedLoadPower, Validators.compose([
        Validators.min(1)
      ])),
      inputMode: new FormControl(this.controller.properties.inputChannelAddress),
      invertBehaviour: new FormControl(this.controller.properties.invert, Validators.requiredTrue)
    })
    this.inputMode = this.getInputMode()
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
    let oldThreshold: number = this.controller.properties.threshold;
    let newThreshold: number = this.controller.properties.threshold;

    switch (event.detail.value) {
      case "SOC":
        this.inputMode = 'SOC';
        this.formGroup.value.inputMode = 'SOC'
        if (Math.abs(this.controller.properties.threshold) < 0 || Math.abs(this.controller.properties.threshold) > 100) {
          newThreshold = 50;
        } else if (this.controller.properties.threshold < 0) {
          newThreshold = newThreshold * -1;
        }
        break;
      case "GRIDSELL":
        this.inputMode = 'GRIDSELL';
        this.formGroup.value.inputMode = 'GRIDSELL'
        if (this.controller.properties.threshold > 0) {
          newThreshold = newThreshold * -1;
        }
        break;
      case "GRIDBUY":
        this.inputMode = 'GRIDBUY';
        this.formGroup.value.inputMode = 'GRIDBUY'
        if (this.controller.properties.threshold < 0) {
          newThreshold = newThreshold * -1;
        }
        break;
      case "PRODUCTION":
        this.inputMode = 'PRODUCTION';
        this.formGroup.value.inputMode = 'PRODUCTION'
        if (this.controller.properties.threshold < 0) {
          newThreshold = newThreshold * -1;
        }
        break;
    }

    // converts threshold if necessary
    if (oldThreshold != newThreshold) {
      this.formGroup.controls['threshold'].setValue(newThreshold);
      this.formGroup.controls['threshold'].markAsDirty()
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

  showApplyChanges(): boolean {
    if (this.formGroup.dirty) {
      return true;
    } else {
      return false;
    }
  }

  getData() {
    console.log("inputMode", this.inputMode);
    console.log("formInputMode", this.formGroup.value.inputMode)
  }

  applyChanges() {
    let newValues = [
      { minimumSwitchingTime: this.formGroup.value.minimumSwitchingTime },
      { threshold: this.formGroup.value.threshold },
      { switchedLoadPower: this.formGroup.value.switchedLoadPower },
      { inputChannelAddress: this.formGroup.value.inputMode },
      { invert: this.formGroup.value.invertBehaviour }
    ];

    let oldValues = [
      { minimumSwitchingTime: this.controller.properties.minimumSwitchingTime },
      { threshold: this.controller.properties.threshold },
      { switchedLoadPower: this.controller.properties.switchedLoadPower },
      { inputChannelAddress: this.controller.properties.inputChannelAddress },
      { invert: this.controller.properties.invert }
    ];

    let updateComponentArray = [];

    newValues.forEach((values, index) => {
      if (Object.values(values)[0] != Object.values(oldValues[index])[0]) {
        updateComponentArray.push({ name: Object.keys(values)[0], value: Object.values(values)[0] })
      }
    });

    console.log("updateComponentArray", updateComponentArray)

    this.loading = true;
    this.formGroup.markAsPristine()
    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, updateComponentArray).then(() => {
        this.controller.properties.minimumSwitchingTime = this.formGroup.value.minimumSwitchingTime;
        this.controller.properties.threshold = this.formGroup.value.threshold;
        this.controller.properties.switchedLoadPower = this.formGroup.value.switchedLoadPower;
        // this.controller.properties.inputChannelAddress = this.convertToChannelAddress(this.formGroup.value.inputMode);
        this.controller.properties.invert = this.formGroup.value.invertBehaviour;
        this.loading = false;
        this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
      }).catch(reason => {
        this.loading = false;
        // this.formGroup.controls['minimumSwitchingTime'].setValue(this.controller.properties.minimumSwitchingTime);
        // this.formGroup.controls['threshold'].setValue(this.controller.properties.threshold);
        // this.formGroup.controls['switchedLoadPower'].setValue(this.controller.properties.switchedLoadPower);
        // this.formGroup.controls['inputChannelAddress'].setValue(this.controller.properties.inputChannelAddress);
        // this.formGroup.controls['invert'].setValue(this.controller.properties.invert);
        this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
        console.warn(reason);
      });
    }
  }
}