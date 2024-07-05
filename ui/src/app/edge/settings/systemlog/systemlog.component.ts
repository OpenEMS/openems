import { Component, OnDestroy, OnInit } from '@angular/core';
import { SelectCustomEvent } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { parse } from 'date-fns';
import { Subject } from 'rxjs';
import { filter, take, takeUntil } from 'rxjs/operators';
import { Filter } from 'src/app/index/filter/filter.component';

import { Service, Utils, Websocket } from '../../../shared/shared';
import { Role } from 'src/app/shared/type/role';

export const LOG_LEVEL_FILTER = (translate: TranslateService): Filter => ({
  placeholder: translate.instant("EDGE.CONFIG.LOG.LEVEL"),
  category: "level",
  options: [
    {
      name: 'Debug',
      value: "DEBUG",
    },
    {
      name: translate.instant('GENERAL.INFO'),
      value: "INFO",
    },
    {
      name: translate.instant('GENERAL.WARNING'),
      value: "WARN",
    },
    {
      name: translate.instant("GENERAL.FAULT"),
      value: "ERROR",
    },
  ],
});

@Component({
  selector: SystemLogComponent.SELECTOR,
  templateUrl: './systemlog.component.html',
})
export class SystemLogComponent implements OnInit, OnDestroy {

  public isSubscribed: boolean = false;

  /** Displayed loglines */
  protected logLines: typeof this._logLines = [];
  protected query: string | null = null;
  protected filters: Filter = LOG_LEVEL_FILTER(this.translate);
  protected isCondensedOutput: boolean | null = null;
  protected isAtLeastGuest: boolean = false;

  private static readonly SELECTOR = "systemLog";
  private ngUnsubscribe = new Subject<void>();
  private searchParams: string[] | null = null;
  private MAX_LOG_ENTRIES = 200;
  private static readonly DEBUG_LOG_CONTROLLER_ID = 'ctrlDebugLog0';

  /** Original loglines */
  private _logLines: {
    time: string,
    level: string,
    color: string,
    message: string,
    source: string
  }[] = [];

  constructor(
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService,
  ) {
  }

  ngOnInit() {
    this.subscribe();

    this.service.getCurrentEdge().then(edge => {
      this.isAtLeastGuest = !edge.roleIsAtLeast(Role.OWNER);
      edge.getConfig(this.websocket).pipe(filter(config => !!config), take(1))
        .subscribe(config => {
          const component = config.getComponent(SystemLogComponent.DEBUG_LOG_CONTROLLER_ID);

          if (!component) {
            this.isCondensedOutput = null;
          }

          if (component.properties?.condensedOutput != null) {
            this.isCondensedOutput = component.properties?.condensedOutput;
          }
        });
    });
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

  protected toggleCondensedOutput(event: CustomEvent) {
    this.service.currentEdge.pipe(filter(edge => !!edge), take(1))
      .subscribe(edge =>
        edge.updateComponentConfig(this.websocket, SystemLogComponent.DEBUG_LOG_CONTROLLER_ID, [{
          name: 'condensedOutput', value: event.detail['checked'],
        }]).then(() => {
          this.service.toast(this.translate.instant('GENERAL.CHANGE_ACCEPTED'), 'success');
        }).catch((reason) => {
          this.service.toast(this.translate.instant('GENERAL.CHANGE_FAILED') + '\n' + reason.error.message, 'danger');
        }));
  }

  public subscribe() {
    // put placeholder
    if (this.logLines.length > 0) {
      this.logLines.unshift({
        time: "-------------------",
        level: "----",
        color: "black",
        message: "",
        source: "",
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
        takeUntil(this.ngUnsubscribe),
      ).subscribe(line => {

        // add line
        this._logLines.unshift({
          time: parse(line.time, "yyyy-MM-dd'T'HH:mm:ss.SSSxxx", new Date()).toLocaleString(),
          color: this.getColor(line.level),
          level: line.level,
          source: line.source,
          message: line.message.replace(/\n/g, "</br>"),
        });

        this.filterLogs();
        // remove old lines
        if (this._logLines.length > this.MAX_LOG_ENTRIES) {
          this._logLines.length = this.MAX_LOG_ENTRIES;
        }
      });
    });
    this.isSubscribed = true;
  }

  private getColor(level: 'INFO' | 'WARN' | 'DEBUG' | 'ERROR'): string {
    switch (level) {
      case 'INFO':
        return 'green';
      case 'WARN':
        return 'orange';
      case 'DEBUG':
        return 'gray';
      case 'ERROR':
        return 'red';
      default:
        return 'black';
    }
  }

  public unsubscribe() {
    this.service.getCurrentEdge().then(edge => {
      edge.unsubscribeSystemLog(this.websocket);
    });
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
    this.ngUnsubscribe = new Subject<void>();
  }

  /**
    * Search on change, triggered by searchbar input-event.
    *
    * @param event from template passed event
    */
  protected searchOnChange(searchParams?: SelectCustomEvent): void {

    if (searchParams) {
      this.searchParams = searchParams?.target?.value ?? null;
    }

    this.filterLogs();
  }

  /**
   * Filters the logs
   */
  private filterLogs(): void {

    if (this.query === null && this.searchParams === null) {
      this.logLines = this._logLines;
      return;
    }

    this.logLines = this._logLines
      .filter(line => (this.searchParams != null && this.searchParams?.length > 0)
        ? this.searchParams?.includes(line.level)
        : true)
      .reduce((arr: typeof this.logLines, el) => {

        if (this.query == null || !this.query.length) {
          return this._logLines;
        }

        const message = el.message.split('</br>').filter(el => el.toLowerCase().includes(this.query!.toLowerCase())).join('</br>');

        if (message?.length > 0) {
          el.message = message;
          arr.push(el);
        }

        return arr;
      }, []);
  }
}
