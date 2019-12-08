import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
  selector: CorrentlyModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class CorrentlyModalComponent {

  private static readonly SELECTOR = "corrently-modal";

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
  ) { }

}