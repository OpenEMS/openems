import { Component, Input } from '@angular/core';
import { CommonThingComponent } from '../../thing.component';

@Component({
  selector: 'common-thing-ess-feneconcommercial',
  templateUrl: './feneconcommercial.component.html',
})
export class CommonEssFeneconCommercialComponent extends CommonThingComponent {
  private soc: number;
  private activePower: number;
  private reactivePower: number;
  private warning: string;

  @Input()
  set data(data: any) {
    super.init(data);
    let d = data.value;

    // title
    this.title = d._title ? d._title : "Stromspeicher";

    // data
    this.soc = "Soc" in d ? d.Soc : null;
    this.activePower = "ActivePower" in d ? d.ActivePower : null;
    this.reactivePower = "ReactivePower" in d ? d.ReactivePower : null;
    this.warning = "Warning" in d ? d.Warning : null;
  }
}
