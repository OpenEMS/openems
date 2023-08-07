import { AfterViewChecked, ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController, PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { ChartOptionsPopoverComponent } from "../../chartoptions/popover/popover.component";
import { DefaultTypes } from "../../service/defaulttypes";
import { Edge, Service } from "../../shared";

@Component({
  selector: 'oe-chart',
  templateUrl: './chart.html'
})
export class ChartComponent implements OnInit, AfterViewChecked {

  public edge: Edge | null = null;
  @Input() public title: string = '';
  @Input() public showPhases: boolean | null = null;
  @Input() public showTotal: boolean | null = null;
  @Output() public showTotalChange: EventEmitter<boolean> = new EventEmitter();
  @Output() public showPhasesChange: EventEmitter<boolean> = new EventEmitter();
  @Input() public isPopoverNeeded: boolean = false;

  // Manually trigger ChangeDetection through Inputchange
  @Input() private period: DefaultTypes.PeriodString;
  protected showPopover: boolean = false;

  constructor(
    protected service: Service,
    private route: ActivatedRoute,
    public popoverCtrl: PopoverController,
    protected translate: TranslateService,
    protected modalCtr: ModalController,
    private ref: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    });
  }

  ngAfterViewChecked() {
    this.ref.detectChanges();
    this.checkIfPopoverNeeded();
  }

  private checkIfPopoverNeeded() {
    if (this.isPopoverNeeded) {
      if (this.service.periodString == DefaultTypes.PeriodString.MONTH || (this.service.periodString == DefaultTypes.PeriodString.YEAR)) {
        this.showPopover = false;
        this.showPhasesChange.emit(false);
        this.showTotalChange.emit(true);
      } else {
        this.showPopover = true;
      }
    }
  }

  async presentPopover(ev: any) {
    const popover = await this.popoverCtrl.create({
      component: ChartOptionsPopoverComponent,
      event: ev,
      componentProps: {
        showPhases: this.showPhases,
        showTotal: this.showTotal
      }
    });

    await popover.present();
    popover.onDidDismiss().then((data) => {
      this.showPhases = data.role == 'Phases' ? data.data : this.showPhases;
      this.showTotal = data.role == 'Total' ? data.data : this.showTotal;
      this.showPhasesChange.emit(this.showPhases);
      this.showTotalChange.emit(this.showTotal);
    });
    await popover.present();
  }
}