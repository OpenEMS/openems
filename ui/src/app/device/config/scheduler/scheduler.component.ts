import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../shared/abstractconfig.component';
import { ConfigImpl } from '../../../shared/device/config';

@Component({
  selector: 'scheduler',
  templateUrl: '../shared/abstractconfig.component.html'
})
export class SchedulerComponent extends AbstractConfigComponent {
  public showSubThings = true

  protected filterThings(config: ConfigImpl): string[] {
    return [config.scheduler];
  }
}