import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../../../../shared/config/abstractconfig.component';
import { ConfigImpl_2018_7 } from '../../../../shared/edge/config.2018.7';

@Component({
  selector: 'persistence',
  templateUrl: '../../../../shared/config/abstractconfig.component.html'
})
export class PersistenceComponent extends AbstractConfigComponent {
  protected filterThings(config: ConfigImpl_2018_7): string[] {
    return config.persistences;
  }
}