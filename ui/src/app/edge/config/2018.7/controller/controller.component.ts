import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../../../../shared/config/abstractconfig.component';

@Component({
  selector: 'controller',
  templateUrl: '../../../../shared/config/abstractconfig.component.html'
})
export class ControllerComponent extends AbstractConfigComponent {
  // TODO
  // protected filterThings(config: ConfigImpl_2018_7): string[] {
  //   return config.controllers;
  // }
}