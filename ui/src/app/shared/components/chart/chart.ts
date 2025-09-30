// @ts-strict-ignore
import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnInit, Output } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController, PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";

import { ChartOptionsPopoverComponent } from "../../legacy/chartoptions/popover/POPOVER.COMPONENT";
import { UserService } from "../../service/USER.SERVICE";
import { Edge, Service } from "../../shared";
import { DefaultTypes } from "../../type/defaulttypes";

@Component({
  selector: "oe-chart",
  templateUrl: "./CHART.HTML",
  standalone: false,
})
export class ChartComponent implements OnInit, OnChanges {

  @Input() public title: string = "";
  @Input() public showPhases: boolean | null = null;
  @Input() public showTotal: boolean | null = null;
  @Output() public setShowPhases: EventEmitter<boolean> = new EventEmitter();
  @Output() public setShowTotal: EventEmitter<boolean> = new EventEmitter();
  @Input() public isPopoverNeeded: boolean = false;
  // Manually trigger ChangeDetection through Inputchange
  @Input() private period?: DEFAULT_TYPES.PERIOD_STRING;

  public edge: Edge | null = null;

  protected showPopover: boolean = false;
  protected newNavigationUrlSegment: string;

  constructor(
    protected service: Service,
    protected userService: UserService,
    private route: ActivatedRoute,
    public popoverCtrl: PopoverController,
    protected translate: TranslateService,
    protected modalCtr: ModalController,
    private ref: ChangeDetectorRef,
  ) { }

  ngOnInit() {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.EDGE = edge;
    });

    const _user = THIS.USER_SERVICE.CURRENT_USER();
    const isNewNavigation = THIS.USER_SERVICE.IS_NEW_NAVIGATION();
    THIS.NEW_NAVIGATION_URL_SEGMENT = isNewNavigation ? "/live" : "";
  }

  /** Run change detection explicitly after the change, to avoid expression changed after it was checked*/
  ngOnChanges() {
    THIS.REF.DETECT_CHANGES();
    THIS.CHECK_IF_POPOVER_NEEDED();
  }

  async presentPopover(ev: any) {
    const popover = await THIS.POPOVER_CTRL.CREATE({
      component: ChartOptionsPopoverComponent,
      event: ev,
      componentProps: {
        showPhases: THIS.SHOW_PHASES,
        showTotal: THIS.SHOW_TOTAL,
      },
    });

    await POPOVER.PRESENT();
    POPOVER.ON_DID_DISMISS().then((data) => {
      THIS.SHOW_PHASES = DATA.ROLE == "Phases" ? DATA.DATA : THIS.SHOW_PHASES;
      THIS.SHOW_TOTAL = DATA.ROLE == "Total" ? DATA.DATA : THIS.SHOW_TOTAL;
      THIS.SET_SHOW_PHASES.EMIT(THIS.SHOW_PHASES);
      THIS.SET_SHOW_TOTAL.EMIT(THIS.SHOW_TOTAL);
    });
    await POPOVER.PRESENT();
  }

  private checkIfPopoverNeeded() {
    if (THIS.IS_POPOVER_NEEDED) {
      if (THIS.SERVICE.PERIOD_STRING == DEFAULT_TYPES.PERIOD_STRING.MONTH || (THIS.SERVICE.PERIOD_STRING == DEFAULT_TYPES.PERIOD_STRING.YEAR)) {
        THIS.SHOW_POPOVER = false;
        THIS.SET_SHOW_PHASES.EMIT(false);
        THIS.SET_SHOW_TOTAL.EMIT(true);
      } else {
        THIS.SHOW_POPOVER = true;
      }
    }
  }

}
