import { ActivatedRoute } from '@angular/router';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { parse } from 'date-fns';
import { Service, Utils, Websocket } from '../../../shared/shared';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: SystemLogComponent.SELECTOR,
  templateUrl: './systemlog.component.html'
})
export class SystemLogComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "systemLog";

  public lines: {
    time: string,
    level: string,
    color: string,
    message: string,
    source: string
  }[] = [];
  public isSubscribed: boolean = false;
  private MAX_LOG_ENTRIES = 200;
  private ngUnsubscribe = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent({ languageKey: 'Edge.Config.Index.liveLog' }, this.route);
    this.subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe();
  }

  public toggleSubscribe(event: CustomEvent) {
    if (event.detail['checked']) {
      this.subscribe();
    } else {
      this.unsubscribe();
    }
  }

  public subscribe() {
    // put placeholder
    if (this.lines.length > 0) {
      this.lines.unshift({
        time: "-------------------",
        level: "----",
        color: "black",
        message: "",
        source: ""
      });
    }

    // complete old subscribe
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
    this.ngUnsubscribe = new Subject<void>();

    this.service.getCurrentEdge().then(edge => {
      // send request to Edge
      edge.subscribeSystemLog(this.websocket);

      // subscribe to notifications
      edge.systemLog.pipe(
        takeUntil(this.ngUnsubscribe)
      ).subscribe(line => {
        // add line
        this.lines.unshift({
          time: parse(line.time, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx", new Date()).toLocaleString(),
          color: this.getColor(line.level),
          level: line.level,
          source: line.source,
          message: line.message
        });

        // remove old lines
        if (this.lines.length > this.MAX_LOG_ENTRIES) {
          this.lines.length = this.MAX_LOG_ENTRIES;
        }
      });
    });
    this.isSubscribed = true;
  };

  private getColor(level): string {
    switch (level) {
      case 'INFO':
        return 'green';
      case 'WARN':
        return 'orange';
      case 'DEBUG':
        return 'gray';
      case 'ERROR':
        return 'red';
    };
    return 'black';
  }

  public unsubscribe() {
    this.service.getCurrentEdge().then(edge => {
      edge.unsubscribeSystemLog(this.websocket);
    });
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
    this.ngUnsubscribe = new Subject<void>();
  };
}
