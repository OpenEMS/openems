import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge, EdgeConfig, ChannelAddress, Websocket } from '../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { Validators, FormBuilder, FormGroup, FormControl, AbstractControl } from '@angular/forms';

type mode = 'ON' | 'AUTOMATIC' | 'OFF';
type inputMode = 'SOC' | 'GRIDSELL' | 'PRODUCTION' | 'OTHER'

@Component({
  selector: SinglethresholdModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class SinglethresholdModalComponent {

  private static readonly SELECTOR = "singlethreshold-modal";

  public formGroup: FormGroup;

  public inputMode: inputMode = null;
  public threshold: number = null;
  public minimumSwitchtingTime: number = null;

  @Input() public edge: Edge;
  @Input() public controller: EdgeConfig.Component;
  @Input() public outputChannel: ChannelAddress;
  @Input() public inputChannel: ChannelAddress;

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
    public translate: TranslateService,
    public websocket: Websocket,
    public formBuilder: FormBuilder
  ) {
    this.formGroup = this.formBuilder.group({
      minimumSwitchingTime: new FormControl('', Validators.compose([
        Validators.min(10),
      ])),
      socThreshold: new FormControl('', Validators.compose([
        Validators.min(1),
        Validators.max(100)
      ])),
      regularThreshold: new FormControl('', Validators.compose([
        Validators.min(0)
      ])),
      switchedLoadPower: new FormControl('', Validators.compose([
        Validators.min(1)
      ])),
      // todo: isChannelName (component.id + '/' + string) validator bauen
      otherChannel: new FormControl('', Validators.compose([
        Validators.min(1)
      ])),
      mode: new FormControl()
    })
  }


  ngOnInit() {
    if (this.controller.properties.inputChannelAddress == '_sum/GridActivePower') {
      this.inputMode = 'GRIDSELL';
    } else if (this.controller.properties.inputChannelAddress == '_sum/ProductionActivePower') {
      this.inputMode = 'PRODUCTION';
    } else if (this.controller.properties.inputChannelAddress == '_sum/EssSoc') {
      this.inputMode = 'SOC';
    } else if (this.controller.properties.inputChannelAddress != null) {
      this.inputMode = 'OTHER';
    }
  }

  updateInputMode(event: CustomEvent, currentController: EdgeConfig.Component) {
    let oldInputChannel: string = this.controller.properties.inputChannelAddress
    let newInputChannel: string;
    let oldThreshold: number = this.controller.properties.threshold;
    let newThreshold: number = this.controller.properties.threshold;

    switch (event.detail.value) {
      case "SOC":
        this.inputMode = 'SOC';
        newInputChannel = '_sum/EssSoc';
        if (Math.abs(this.controller.properties.threshold) < 0 || Math.abs(this.controller.properties.threshold) > 100) {
          newThreshold = 50;
        } else if (this.controller.properties.threshold < 0) {
          newThreshold = newThreshold * -1;
        }
        this.formGroup.value.mode = 'SOC'
        break;
      case "GRIDSELL":
        this.inputMode = 'GRIDSELL';
        newInputChannel = '_sum/GridActivePower'
        if (this.controller.properties.threshold > 0) {
          newThreshold = newThreshold * -1;
        }
        this.formGroup.value.mode = 'GRIDSELL'
        break;
      case "PRODUCTION":
        this.inputMode = 'PRODUCTION';
        newInputChannel = '_sum/ProductionActivePower'
        if (this.controller.properties.threshold < 0) {
          newThreshold = newThreshold * -1;
        }
        this.formGroup.value.mode = 'PRODUCTION'
        break;
      case "OTHER":
        this.inputMode = 'OTHER';
        this.formGroup.value.mode = 'OTHER'
        break;
    }

    // if (this.edge != null) {
    //   this.edge.updateComponentConfig(this.websocket, this.controller.id, [
    //     { name: 'inputChannelAddress', value: newInputChannel },
    //     { name: 'threshold', value: newThreshold }
    //   ]).then(() => {
    //     this.controller.properties.inputChannelAddress = newInputChannel;
    //     this.controller.properties.threshold = newThreshold;
    //     this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
    //   }).catch(reason => {
    //     this.controller.properties.inputChannelAddress = oldInputChannel;
    //     this.controller.properties.threshold = oldThreshold;
    //     this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
    //     console.warn(reason);
    //   });
    // }
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

  updateSocThreshold(event: CustomEvent) {
    let oldThreshold = this.controller.properties.threshold;
    let newThreshold = event;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'threshold', value: newThreshold }
      ]).then(() => {
        this.controller.properties['threshold'] = newThreshold;
        this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
      }).catch(reason => {
        this.controller.properties['threshold'] = oldThreshold;
        this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
        console.warn(reason);
      })
    }
  }

  updateNonSocThresholdInput(event: CustomEvent) {
    if (isNaN(event.detail.value)) {
      event.detail.value = null;
    } else {
      if (this.inputMode == 'GRIDSELL') {
        this.threshold = event.detail.value * -1;
      } else {
        this.threshold = event.detail.value;
      }
    }
  }

  setInvert() {
    let oldInvert = this.controller.properties.invert;
    let newInvert;

    if (this.controller.properties.invert == true) {
      newInvert = false;
    } else if (this.controller.properties.invert == false) {
      newInvert = true;
    }

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'invert', value: newInvert },
      ]).then(() => {
        this.controller.properties.invert = newInvert;
        this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
      }).catch(reason => {
        this.controller.properties.invert = oldInvert;
        this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
        console.warn(reason);
      })
    }
  }

  updateMinimumSwitchtingTimeInput(event: CustomEvent) {
    this.minimumSwitchtingTime = event.detail.value;
  }

  applyChanges() {
    // let oldMinimumSwitchingTime;
    // let newMinimumSwitchingTime;
    // let oldThreshold;
    // let newThreshold;

    // if (this.minimumSwitchtingTime != null) {
    //   oldMinimumSwitchingTime = this.controller.properties.minimumSwitchingTime;
    //   newMinimumSwitchingTime = this.minimumSwitchtingTime;
    // } else if (this.minimumSwitchtingTime == null) {
    //   oldMinimumSwitchingTime = this.controller.properties.minimumSwitchingTime;
    //   newMinimumSwitchingTime = this.controller.properties.minimumSwitchingTime;
    // }
    // if (this.threshold != null) {
    //   oldThreshold = this.controller.properties.threshold;
    //   newThreshold = this.threshold;
    // } else if (this.threshold == null) {
    //   oldThreshold = this.controller.properties.threshold;
    //   newThreshold = this.controller.properties.threshold;
    // }

    // if (this.edge != null) {
    //   this.edge.updateComponentConfig(this.websocket, this.controller.id, [
    //     { name: 'minimumSwitchingTime', value: newMinimumSwitchingTime },
    //     { name: 'threshold', value: newThreshold }
    //   ]).then(() => {
    //     this.controller.properties.minimumSwitchingTime = newMinimumSwitchingTime;
    //     this.controller.properties['threshold'] = newThreshold;
    //     this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
    //     this.threshold = null;
    //     this.minimumSwitchtingTime = null;
    //   }).catch(reason => {
    //     this.controller.properties['threshold'] = oldThreshold;
    //     this.controller.properties.minimumSwitchingTime = oldMinimumSwitchingTime;
    //     this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
    //     this.threshold = null;
    //     this.minimumSwitchtingTime = null;
    //     console.warn(reason);
    //   })
    // }
    console.log("input:", this.formGroup.value)
  }

  onSubmit(values) {
    console.log("value", values)
  }
}