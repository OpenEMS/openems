import { Component, OnDestroy, OnInit } from "@angular/core";
import { SelectCustomEvent } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { parse } from "date-fns";
import { Subject } from "rxjs";
import { filter, take, takeUntil } from "rxjs/operators";
import { Filter } from "src/app/index/filter/FILTER.COMPONENT";
import { Role } from "src/app/shared/type/role";
import { Service, Utils, Websocket } from "../../../shared/shared";

export const LOG_LEVEL_FILTER = (translate: TranslateService): Filter => ({
  placeholder: TRANSLATE.INSTANT("EDGE.CONFIG.LOG.LEVEL"),
  category: "level",
  options: [
    {
      name: "Debug",
      value: "DEBUG",
    },
    {
      name: TRANSLATE.INSTANT("GENERAL.INFO"),
      value: "INFO",
    },
    {
      name: TRANSLATE.INSTANT("GENERAL.WARNING"),
      value: "WARN",
    },
    {
      name: TRANSLATE.INSTANT("GENERAL.FAULT"),
      value: "ERROR",
    },
  ],
});

@Component({
  selector: SYSTEM_LOG_COMPONENT.SELECTOR,
  templateUrl: "./SYSTEMLOG.COMPONENT.HTML",
  standalone: false,
})
export class SystemLogComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "systemLog";
  private static readonly DEBUG_LOG_CONTROLLER_ID = "ctrlDebugLog0";

  public isSubscribed: boolean = false;

  /** Displayed loglines */
  protected logLines: typeof this._logLines = [];
  protected query: string | null = null;
  protected filters: Filter = LOG_LEVEL_FILTER(THIS.TRANSLATE);
  protected isCondensedOutput: boolean | null = null;
  protected isAtLeastGuest: boolean = false;

  private ngUnsubscribe = new Subject<void>();
  private searchParams: string[] | null = null;
  private MAX_LOG_ENTRIES = 200;

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
  ) { }

  public subscribe() {
    // put placeholder
    if (THIS.LOG_LINES.LENGTH > 0) {
      THIS.LOG_LINES.UNSHIFT({
        time: "-------------------",
        level: "----",
        color: "black",
        message: "",
        source: "",
      });
    }

    // complete old subscribe
    THIS.NG_UNSUBSCRIBE.NEXT();
    THIS.NG_UNSUBSCRIBE.COMPLETE();
    THIS.NG_UNSUBSCRIBE = new Subject<void>();

    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      // send request to Edge
      EDGE.SUBSCRIBE_SYSTEM_LOG(THIS.WEBSOCKET);

      // subscribe to notifications
      EDGE.SYSTEM_LOG.PIPE(
        takeUntil(THIS.NG_UNSUBSCRIBE),
      ).subscribe(line => {

        // add line
        this._logLines.unshift({
          time: parse(LINE.TIME, "yyyy-MM-dd'T'HH:mm:SS.SSSXXX", new Date()).toLocaleString(),
          color: THIS.GET_COLOR(LINE.LEVEL),
          level: LINE.LEVEL,
          source: LINE.SOURCE,
          message: LINE.MESSAGE.REPLACE(/\n/g, "</br>"),
        });

        THIS.FILTER_LOGS();
        // remove old lines
        if (this._logLines.length > this.MAX_LOG_ENTRIES) {
          this._logLines.length = this.MAX_LOG_ENTRIES;
        }
      });
    });
    THIS.IS_SUBSCRIBED = true;
  }

  ngOnInit() {
    THIS.SUBSCRIBE();

    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.IS_AT_LEAST_GUEST = !EDGE.ROLE_IS_AT_LEAST(ROLE.OWNER);
      EDGE.GET_CONFIG(THIS.WEBSOCKET).pipe(filter(config => !!config), take(1))
        .subscribe(config => {
          const component = CONFIG.GET_COMPONENT(SystemLogComponent.DEBUG_LOG_CONTROLLER_ID);

          if (!component) {
            THIS.IS_CONDENSED_OUTPUT = null;
          }

          if (COMPONENT.PROPERTIES?.condensedOutput != null) {
            THIS.IS_CONDENSED_OUTPUT = COMPONENT.PROPERTIES?.condensedOutput;
          }
        });
    });
  }

  ngOnDestroy() {
    THIS.UNSUBSCRIBE();
  }

  public toggleSubscribe(event: CustomEvent) {
    if (EVENT.DETAIL["checked"]) {
      THIS.SUBSCRIBE();
    } else {
      THIS.UNSUBSCRIBE();
    }
  }

  public unsubscribe() {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      EDGE.UNSUBSCRIBE_SYSTEM_LOG(THIS.WEBSOCKET);
    });
    THIS.NG_UNSUBSCRIBE.NEXT();
    THIS.NG_UNSUBSCRIBE.COMPLETE();
    THIS.NG_UNSUBSCRIBE = new Subject<void>();
  }

  protected toggleCondensedOutput(event: CustomEvent) {
    THIS.SERVICE.GET_CURRENT_EDGE()
      .then(edge =>
        EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, SystemLogComponent.DEBUG_LOG_CONTROLLER_ID, [{
          name: "condensedOutput", value: EVENT.DETAIL["checked"],
        }]).then(() => {
          THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
        }).catch((reason) => {
          THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
        }));
  }

  /**
  * Search on change, triggered by searchbar input-event.
  *
  * @param event from template passed event
  */
  protected searchOnChange(searchParams?: SelectCustomEvent): void {

    if (searchParams) {
      THIS.SEARCH_PARAMS = searchParams?.target?.value ?? null;
    }

    THIS.FILTER_LOGS();
  }

  private getColor(level: "INFO" | "WARN" | "DEBUG" | "ERROR"): string {
    switch (level) {
      case "INFO":
        return "green";
      case "WARN":
        return "orange";
      case "DEBUG":
        return "gray";
      case "ERROR":
        return "red";
    }
  }

  /**
   * Filters the logs
   */
  private filterLogs(): void {

    if (THIS.QUERY === null && THIS.SEARCH_PARAMS === null) {
      THIS.LOG_LINES = this._logLines;
      return;
    }

    THIS.LOG_LINES = this._logLines
      .filter(line => (THIS.SEARCH_PARAMS != null && THIS.SEARCH_PARAMS?.length > 0)
        ? THIS.SEARCH_PARAMS?.includes(LINE.LEVEL)
        : true)
      .reduce((arr: typeof THIS.LOG_LINES, el) => {

        if (THIS.QUERY == null || !THIS.QUERY.LENGTH) {
          return this._logLines;
        }

        const message = EL.MESSAGE.SPLIT("</br>").filter(el => EL.TO_LOWER_CASE().includes(THIS.QUERY!.toLowerCase())).join("</br>");

        if (message?.length > 0) {
          EL.MESSAGE = message;
          ARR.PUSH(el);
        }

        return arr;
      }, []);
  }
}
