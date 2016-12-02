import { Component, OnInit, Input } from '@angular/core';
import { CommonThingComponent } from '../../thing.component';

@Component({
  selector: 'common-thing-meter-simulator',
  templateUrl: './simulator.component.html',
})
export class CommonMeterSimulatorComponent extends CommonThingComponent {
  private activePower: number;

  @Input()
  set data(data: any) {
    super.init(data);
    let d = data.value;
    this.activePower = "ActivePower" in d ? d.ActivePower : null;
  }
}
