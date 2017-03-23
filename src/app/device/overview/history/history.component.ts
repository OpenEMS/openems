import { Component, Input } from '@angular/core';

import { Device } from '../../../shared/shared';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent {

  @Input()
  private device: Device;
}
