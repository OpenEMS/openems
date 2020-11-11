import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
  selector: BydModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class BydModalComponent {

  private static readonly SELECTOR = "byd-modal";

  constructor(
    public modalCtrl: ModalController,
    public service: Service,
  ) { }
}