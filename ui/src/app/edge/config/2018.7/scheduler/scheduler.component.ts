import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../../../../shared/config/abstractconfig.component';
import { ConfigImpl_2018_7 } from '../../../../shared/edge/config.2018.7';

@Component({
  selector: 'scheduler',
  templateUrl: '../../../../shared/config/abstractconfig.component.html'
})
export class SchedulerComponent extends AbstractConfigComponent {
  public showSubThings = true

  protected filterThings(config: ConfigImpl_2018_7): string[] {
    return [config.scheduler];
  }
}