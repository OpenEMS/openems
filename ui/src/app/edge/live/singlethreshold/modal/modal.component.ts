import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge, EdgeConfig, ChannelAddress, Websocket } from '../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';

type mode = 'ON' | 'AUTOMATIC' | 'OFF';
type inputMode = 'SOC' | 'GRIDSELL' | 'PRODUCTION'

@Component({
  selector: SinglethresholdModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class SinglethresholdModalComponent {

  private static readonly SELECTOR = "singlethreshold-modal";

  public inputMode: inputMode = null;

  @Input() public edge: Edge;
  @Input() public controller: EdgeConfig.Component;
  @Input() public outputChannel: ChannelAddress;
  @Input() public inputChannel: ChannelAddress;

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
    public translate: TranslateService,
    public websocket: Websocket
  ) { }

  ngOnInit() {
    if (this.controller.properties.inputChannelAddress == '_sum/GridActivePower') {
      this.inputMode = 'GRIDSELL';
    } else if (this.controller.properties.inputChannelAddress == '_sum/ProductionActivePower') {
      this.inputMode = 'PRODUCTION';
    } else if (this.controller.properties.inputChannelAddress == '_sum/EssSoc') {
      this.inputMode = 'SOC';
    } else {
      this.inputMode = null;
    }
  }

  updateInputMode(event: CustomEvent) {
    let oldInputChannel: string = this.controller.properties.inputChannelAddress
    let newInputChannel: string;
    let oldThreshold: number = this.controller.properties.threshold;
    let newThreshold: number;

    switch (event.detail.value) {
      case "SOC":
        this.inputMode = 'SOC';
        newInputChannel = '_sum/EssSoc';
        if (Math.abs(this.controller.properties.threshold) < 0 || Math.abs(this.controller.properties.threshold) > 100) {
          newThreshold = 50;
        }
        break;
      case "GRIDSELL":
        this.inputMode = 'GRIDSELL';
        newInputChannel = '_sum/GridActivePower'
        if ()
          break;
      case "PRODUCTION":
        this.inputMode = 'PRODUCTION';
        newInputChannel = '_sum/ProductionActivePower'
        break;
    }

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'inputChannelAddress', value: newInputChannel }
      ]).then(() => {
        this.controller.properties.inputChannelAddress = newInputChannel;
        this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
      }).catch(reason => {
        this.controller.properties.inputChannelAddress = oldInputChannel;
        this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
        console.warn(reason);
      });
    }

    if (event.detail.value == 'SOC') {
      if (Math.abs(this.controller.properties.threshold) < 0 || Math.abs(this.controller.properties.threshold) > 100) {
        let oldThreshold: number = this.controller.properties.threshold;
        let newThreshold: number = 50;
        if (this.edge != null) {
          this.edge.updateComponentConfig(this.websocket, this.controller.id, [
            { name: 'threshold', value: newThreshold }
          ]).then(() => {
            this.controller.properties.threshold = newThreshold;
          }).catch(reason => {
            this.controller.properties.threshold = oldThreshold;
            console.warn(reason);
          });
        }
      }
    } else if (event.detail.value == 'PRODUCTION' || event.detail.value == 'GRIDSELL') {
      let oldThreshold: number = this.controller.properties.threshold;
      let newThreshold: number = this.controller.properties.threshold * -1;
      if (this.edge != null) {
        this.edge.updateComponentConfig(this.websocket, this.controller.id, [
          { name: 'threshold', value: newThreshold }
        ]).then(() => {
          this.controller.properties.threshold = newThreshold;
        }).catch(reason => {
          this.controller.properties.threshold = oldThreshold;
          console.warn(reason);
        });
      }
    }
    console.log("threshold", this.controller.properties.threshold)
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
}