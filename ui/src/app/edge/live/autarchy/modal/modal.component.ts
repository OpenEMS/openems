import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
  selector: AutarchyModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class AutarchyModalComponent {

  private static readonly SELECTOR = "autarchy-modal";

  constructor(
    public modalCtrl: ModalController,
    public service: Service,
  ) { }
}