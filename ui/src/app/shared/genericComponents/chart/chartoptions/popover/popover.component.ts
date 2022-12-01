import { Component, Input } from '@angular/core';
import { ModalController, PopoverController } from '@ionic/angular';
import { Service } from 'src/app/shared/shared';

@Component({
  selector: 'chartoptionspopover',
  templateUrl: './popover.component.html'
})
export class ChartOptionsPopoverComponent {

  @Input() public showPhases: boolean | null = null;
  @Input() public showTotal: boolean | null = null;

  constructor(
    public service: Service,
    protected modalCtrl: ModalController,
    public popoverCtrl: PopoverController,
  ) { }

  public setPhases() {
    this.showPhases = !this.showPhases;
    this.popoverCtrl.dismiss(this.showPhases, 'Phases');
  }

  public setTotal() {
    this.showTotal = !this.showTotal;
    this.popoverCtrl.dismiss(this.showTotal, 'Total');
  }
}
