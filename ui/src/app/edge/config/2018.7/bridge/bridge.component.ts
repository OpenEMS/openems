import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../../../../shared/config/abstractconfig.component';

@Component({
  selector: 'bridge',
  templateUrl: 'bridge.component.html'
})
export class BridgeComponent extends AbstractConfigComponent {
  public showSubThings = true

  // TODO
  // protected filterThings(config: ConfigImpl_2018_7): string[] {
  //   return config.bridges;
  // }
}