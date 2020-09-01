import { Component, Input } from '@angular/core';
import { Edge, Service } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: GridModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class GridModalComponent {

  private static readonly SELECTOR = "grid-modal";

  @Input() public edge: Edge | null = null;

  constructor(
    public service: Service,
    public modalCtrl: ModalController,
    public translate: TranslateService,
  ) { }
}
