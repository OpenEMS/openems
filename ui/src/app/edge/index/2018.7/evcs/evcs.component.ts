import { Component, Input, ViewChildren, QueryList } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { Utils } from '../../../../shared/service/utils';
import { DefaultTypes } from '../../../../shared/service/defaulttypes';
import { CurrentDataAndSummary_2018_7 } from '../../../../shared/edge/currentdata.2018.7';
import { Edge } from '../../../../shared/edge/edge';
import { ChannelComponent } from '../../../../shared/config/channel.component';

@Component({
  selector: 'evcs-2018-7',
  templateUrl: './evcs.component.html'
})
export class EvcsComponent_2018_7 {

  @Input()
  public currentData: CurrentDataAndSummary_2018_7;

  @Input()
  public config: DefaultTypes.Config_2018_7;

  @Input()
  public edge: Edge;

  constructor(public utils: Utils) { }

  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  /**
   * Handle config update
   */

  @ViewChildren(ChannelComponent)
  private channelComponentChildren: QueryList<ChannelComponent>;
  private stopOnDestroy: Subject<void> = new Subject<void>();
  private formInitialized: boolean = false;

  ngAfterViewChecked() {
    // unfortunately components are not available yet in ngAfterViewInit, so we need to call it again and again, till they are there.
    if (this.formInitialized || this.channelComponentChildren.length == 0) {
      return;
    }
    this.channelComponentChildren.forEach(channelComponent => {
      channelComponent.message
        .pipe(takeUntil(this.stopOnDestroy))
        .subscribe((message) => {
          if (message != null) {
            this.edge.send(message);
          }
        });
    });
    this.formInitialized = true;
  }
}
