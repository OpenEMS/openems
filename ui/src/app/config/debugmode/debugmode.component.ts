import { Component } from '@angular/core';
import { environment } from '../../../environments';

@Component({
  selector: 'debugmode',
  templateUrl: './debugmode.component.html'
})
export class DebugModeComponent {

  public env = environment;

  constructor() { }

  public toggleMode($event: any /*MdSlideToggleChange*/) {
    this.env.debugMode = $event.checked;
  }
}