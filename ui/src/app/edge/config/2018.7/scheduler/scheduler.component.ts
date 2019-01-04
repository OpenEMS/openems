import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../../../../shared/config/abstractconfig.component';

@Component({
  selector: 'scheduler',
  templateUrl: '../../../../shared/config/abstractconfig.component.html'
})
export class SchedulerComponent extends AbstractConfigComponent {
  public showSubThings = true

  // TODO
  // protected filterThings(config: ConfigImpl_2018_7): string[] {
  //   return [config.scheduler];
  // }
}