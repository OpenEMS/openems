import { Component, OnInit, Input } from '@angular/core';
import { CommonThingComponent } from '../../thing.component';

@Component({
  selector: 'common-thing-ess-simulator',
  templateUrl: './simulator.component.html',
})
export class CommonEssSimulatorComponent extends CommonThingComponent {
  private soc: number;
  private activePower: number;
  private reactivePower: number;
  private warning: string;

  @Input()
  set data(data: any) {
    super.init(data);
    let d = data.value;
    this.soc = "Soc" in d ? d.Soc : null;
    this.activePower = "ActivePower" in d ? d.ActivePower : null;
    this.reactivePower = "ReactivePower" in d ? d.ReactivePower : null;
    this.warning = "Warning" in d ? d.Warning : null;
  }
}
