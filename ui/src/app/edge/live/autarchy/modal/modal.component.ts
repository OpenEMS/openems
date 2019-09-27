import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket } from '../../../../shared/shared';

@Component({
  selector: AutarchyModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class AutarchyModalComponent {

  private static readonly SELECTOR = "autarchy-modal";

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
  ) { }

  ngOnInit() {
  }
}