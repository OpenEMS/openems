import { Component, Input, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket, EdgeConfig } from '../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';

@Component({
  selector: 'fixdigitaloutput-modal',
  templateUrl: './modal.component.html'
})
export class FixDigitalOutputModalComponent {

  @Input() public edge: Edge;
  @Input() public controller: EdgeConfig.Component;

  constructor(
    protected service: Service,
    public websocket: Websocket,
    public router: Router,
    protected translate: TranslateService,
    public modalCtrl: ModalController,
  ) { }

  /**  
   * Updates the 'isOn'-Property of the FixDigitalOutput-Controller.
   * 
   * @param event 
   */
  updateMode(event: CustomEvent) {
    let oldMode = this.controller.properties.isOn;
    let newMode = event.detail.value;

    if (oldMode != newMode) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'isOn', value: newMode }
      ]).then(() => {
        this.controller.properties.isOn = newMode;
        this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
      }).catch(reason => {
        this.controller.properties.isOn = oldMode;
        this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
        console.warn(reason);
      });
    }
  }
}
