import { Component, OnInit, Input } from '@angular/core';
import { CommonThingComponent } from '../../thing.component';

@Component({
  selector: 'common-thing-ess-feneconpro',
  templateUrl: './feneconpro.component.html',
})
export class CommonEssFeneconProComponent extends CommonThingComponent {
  private soc: number;
  private activePowerL1: number;
  private activePowerL2: number;
  private activePowerL3: number;
  private reactivePowerL1: number;
  private reactivePowerL2: number;
  private reactivePowerL3: number;
  private warning: string;

  @Input()
  set data(data: any) {
    super.init(data);
    let d = data.value;

    // title
    this.title = d._title ? d._title : "Stromspeicher";

    // data
    this.soc = "Soc" in d ? d.Soc : null;
    this.activePowerL1 = "ActivePowerL1" in d ? d.ActivePowerL1 : null;
    this.activePowerL2 = "ActivePowerL2" in d ? d.ActivePowerL2 : null;
    this.activePowerL3 = "ActivePowerL3" in d ? d.ActivePowerL3 : null;
    this.reactivePowerL1 = "ReactivePowerL1" in d ? d.ReactivePowerL1 : null;
    this.reactivePowerL2 = "ReactivePowerL2" in d ? d.ReactivePowerL2 : null;
    this.reactivePowerL3 = "ReactivePowerL3" in d ? d.ReactivePowerL3 : null;
    this.warning = "Warning" in d ? d.Warning : null;
  }
}
