import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../../../shared/config/abstractconfig.component';
import { ConfigImpl } from '../../../shared/device/config';

@Component({
  selector: 'persistence',
  templateUrl: '../../../shared/config/abstractconfig.component.html'
})
export class PersistenceComponent extends AbstractConfigComponent {
  protected filterThings(config: ConfigImpl): string[] {
    return config.persistences;
  }
}