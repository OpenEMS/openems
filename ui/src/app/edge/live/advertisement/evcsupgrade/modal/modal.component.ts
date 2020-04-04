import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge } from '../../../../../shared/shared';

@Component({
  selector: EvcsUpgradeModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class EvcsUpgradeModalComponent {

  private static readonly SELECTOR = "evcsupgrade-modal";

  @Input() public edge: Edge;

  constructor(
    public modalCtrl: ModalController,
    public service: Service,
  ) { }
}