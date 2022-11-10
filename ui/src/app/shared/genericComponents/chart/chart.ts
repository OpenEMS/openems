import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { AlertController, ModalController, PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, Service } from "../../shared";
import { ChartOptionsPopoverComponent } from "./chartoptions/popover/popover.component";

@Component({
  selector: 'oe-chart',
  templateUrl: './chart.html',
})
export class ChartComponent implements OnInit {

  public edge: Edge | null = null;
  @Input() public title: string = '';
  @Input() public showPhases: boolean;
  @Input() public showTotal: boolean;
  @Output() public setShowPhases: EventEmitter<boolean> = new EventEmitter();
  @Output() public setShowTotal: EventEmitter<boolean> = new EventEmitter();
  @Input() public isPopoverNeeded: boolean = false;

  constructor(
    protected service: Service,
    private route: ActivatedRoute,
    public popoverCtrl: PopoverController,
    private alertCtrl: AlertController,
    protected translate: TranslateService,
    protected modalCtr: ModalController,
  ) { }
  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    })
  }

  async presentPopover(ev: any) {
    let componentProps = {};
    if (this.showPhases !== null) {
      componentProps['showPhases'] = this.showPhases;
    }
    if (this.showTotal !== null) {
      componentProps['showTotal'] = this.showTotal;
    }
    const popover = await this.popoverCtrl.create({
      component: ChartOptionsPopoverComponent,
      event: ev,
      translucent: true,
      componentProps: componentProps,
      animated: true
    });

    await popover.present();
    popover.onDidDismiss().then((data) => {
      this.showPhases = data.role == 'Phases' ? data.data : this.showPhases;
      this.showTotal = data.role == 'Total' ? data.data : this.showTotal;
      this.setShowPhases.emit(this.showPhases)
      this.setShowTotal.emit(this.showTotal)
    });
    await popover.present();
  }
}