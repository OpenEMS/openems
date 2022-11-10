import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ModalController, PopoverController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Service } from 'src/app/shared/shared';

@Component({
  selector: 'chartoptionspopover',
  templateUrl: './popover.component.html'
})
export class ChartOptionsPopoverComponent {

  @Input() public showPhases: boolean | null = null;
  @Input() public showTotal: boolean | null = null;
  @Output() public setShowPhases: EventEmitter<boolean> = new EventEmitter();
  @Output() public setShowTotal: EventEmitter<boolean> = new EventEmitter();

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
