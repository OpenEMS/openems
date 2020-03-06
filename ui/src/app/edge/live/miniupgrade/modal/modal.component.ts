import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge } from '../../../../shared/shared';

@Component({
  selector: MiniupgradeModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class MiniupgradeModalComponent {

  @Input() public edge: Edge;

  private static readonly SELECTOR = "miniupgrade-modal";

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
  ) { }
}