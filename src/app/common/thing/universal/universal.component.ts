import { Component, OnInit, Input } from '@angular/core';

import { BehaviorSubject } from 'rxjs/BehaviorSubject';

@Component({
  selector: 'common-thing-universal',
  templateUrl: './universal.component.html',
})
export class CommonThingUniversalComponent {

  @Input()
  private thing: { key: string, value: {} };

}
