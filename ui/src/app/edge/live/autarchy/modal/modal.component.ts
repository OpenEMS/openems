import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket } from '../../../../shared/shared';

@Component({
  selector: AutarchyModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class AutarchyModalComponent {

  private static readonly SELECTOR = "autarchy-modal";

  @Input() edge: Edge;

  constructor(
    public service: Service,
    private websocket: Websocket,
    public modalCtrl: ModalController,
  ) { }

  ngOnInit() {
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, AutarchyModalComponent.SELECTOR);
    }
  }
}