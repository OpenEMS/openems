import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../../../../shared/config/abstractconfig.component';
import { ConfigImpl_2018_7 } from '../../../../shared/edge/config.2018.7';

@Component({
  selector: 'bridge',
  templateUrl: 'bridge.component.html'
})
export class BridgeComponent extends AbstractConfigComponent {
  public showSubThings = true

  protected filterThings(config: ConfigImpl_2018_7): string[] {
    return config.bridges;
  }
}