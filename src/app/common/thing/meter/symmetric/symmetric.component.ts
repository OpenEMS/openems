import { Component, OnInit, Input } from '@angular/core';
import { CommonThingComponent } from '../../thing.component';

@Component({
  selector: 'common-thing-meter-symmetric',
  templateUrl: './symmetric.component.html',
})
export class CommonMeterSymmetricComponent extends CommonThingComponent {
  private activePower: number;
  private reactivePower: number;

  @Input()
  set data(data: any) {
    super.init(data);
    let d = data.value;

    // title
    this.title = d._title ? d._title : "Zähler";

    // data
    this.activePower = "ActivePower" in d ? d.ActivePower : null;
    this.reactivePower = "ReactivePower" in d ? d.ReactivePower : null;
  }
}
