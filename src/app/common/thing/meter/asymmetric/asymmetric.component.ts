import { Component, Input } from '@angular/core';
import { CommonThingComponent } from '../../thing.component';

@Component({
  selector: 'common-thing-meter-asymmetric',
  templateUrl: './asymmetric.component.html',
})
export class CommonMeterAsymmetricComponent extends CommonThingComponent {
  private activePowerL1: number;
  private activePowerL2: number;
  private activePowerL3: number;
  private reactivePowerL1: number;
  private reactivePowerL2: number;
  private reactivePowerL3: number;

  @Input()
  set data(data: any) {
    super.init(data);
    let d = data.value;

    // title
    this.title = d._title ? d._title : "Zähler";

    // data
    this.activePowerL1 = "ActivePowerL1" in d ? d.ActivePowerL1 : null;
    this.activePowerL2 = "ActivePowerL2" in d ? d.ActivePowerL2 : null;
    this.activePowerL3 = "ActivePowerL3" in d ? d.ActivePowerL3 : null;
    this.reactivePowerL1 = "ReactivePowerL1" in d ? d.ReactivePowerL1 : null;
    this.reactivePowerL2 = "ReactivePowerL2" in d ? d.ReactivePowerL2 : null;
    this.reactivePowerL3 = "ReactivePowerL3" in d ? d.ReactivePowerL3 : null;
  }
}