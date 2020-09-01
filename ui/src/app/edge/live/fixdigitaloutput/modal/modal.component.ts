import { Component, Input } from '@angular/core';
import { Edge, Service, Websocket, EdgeConfig } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'fixdigitaloutput-modal',
  templateUrl: './modal.component.html'
})
export class FixDigitalOutputModalComponent {

  @Input() public edge: Edge | null = null;
  @Input() public component: EdgeConfig.Component | null = null;

  constructor(
    protected service: Service,
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
    if (this.component && this.edge != null) {

      let oldMode = this.component.properties.isOn;
      let newMode = event.detail.value;

      this.edge.updateComponentConfig(this.websocket, this.component.id, [
        { name: 'isOn', value: newMode }
      ]).then(() => {
        if (this.component != null) {
          this.component.properties.isOn = newMode;
        }
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        if (this.component != null) {
          this.component.properties.isOn = oldMode;
        }
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
        console.warn(reason);
      });
    }
  }
}