import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../../../shared/config/abstractconfig.component';
import { ConfigImpl } from '../../../shared/device/config';

@Component({
  selector: 'bridge',
  templateUrl: 'bridge.component.html'
})
export class BridgeComponent extends AbstractConfigComponent {
  public showSubThings = true

  protected filterThings(config: ConfigImpl): string[] {
    return config.bridges;
  }
}