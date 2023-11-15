import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';

@Component({
  selector: 'fixdigitaloutput-modal',
  templateUrl: './modal.component.html',
})
export class Controller_Io_FixDigitalOutputModalComponent {

  @Input() public edge: Edge;
  @Input() public component: EdgeConfig.Component;

  constructor(
    public service: Service,
    protected translate: TranslateService,
    public modalCtrl: ModalController,
    public router: Router,
    public websocket: Websocket,
  ) { }

  /**  
   * Updates the 'isOn'-Property of the FixDigitalOutput-Controller.
   * 
   * @param event 
   */
  updateMode(event: CustomEvent) {
    let oldMode = this.component.properties.isOn;

    // ion-segment button only supports string as type
    // https://ionicframework.com/docs/v4/api/segment-button

    let newMode = (event.detail.value.toLowerCase() === 'true');

    this.edge.updateComponentConfig(this.websocket, this.component.id, [
      { name: 'isOn', value: newMode },
    ]).then(() => {
      this.component.properties.isOn = newMode;
      this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
    }).catch(reason => {
      this.component.properties.isOn = oldMode;
      this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
      console.warn(reason);
    });
  }
}
