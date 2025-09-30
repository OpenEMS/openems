// @ts-strict-ignore
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from "@angular/core";
import { AlertController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Edge, presentAlert, Service, Websocket } from "src/app/shared/shared";
import { environment } from "src/environments";
import { LiveDataService } from "../../live/livedataservice";
import { ExecuteSystemUpdate } from "./executeSystemUpdate";
import { SystemUpdateState } from "./getSystemUpdateStateResponse";

@Component({
  selector: OE_SYSTEM_UPDATE_COMPONENT.SELECTOR,
  templateUrl: "./oe-system-UPDATE.COMPONENT.HTML",
  standalone: false,
  providers: [{
    useClass: LiveDataService,
    provide: DataService,
  }],
})
export class OeSystemUpdateComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "oe-system-update";

  @Output() public stateChanged: EventEmitter<SystemUpdateState> = new EventEmitter();
  @Input() public executeUpdateInstantly: boolean = false;
  @Input({ required: true }) public edge!: Edge;
  public readonly environment = environment;
  public readonly spinnerId: string = OE_SYSTEM_UPDATE_COMPONENT.SELECTOR;

  protected executeUpdate: ExecuteSystemUpdate | null = null;
  protected isWaiting: boolean;

  constructor(
    private websocket: Websocket,
    private service: Service,
    private alertCtrl: AlertController,
    private translate: TranslateService,
  ) { }

  ngOnInit() {
    THIS.EXECUTE_UPDATE = new ExecuteSystemUpdate(THIS.EDGE, THIS.WEBSOCKET);

    THIS.EXECUTE_UPDATE.SYSTEM_UPDATE_STATE_CHANGE = (systemUpdateState) => {
      THIS.STATE_CHANGED.EMIT(systemUpdateState);
      if (SYSTEM_UPDATE_STATE.UPDATED) {
        THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
        THIS.IS_WAITING = false;
      }
    };

    THIS.SERVICE.START_SPINNER_TRANSPARENT_BACKGROUND(THIS.SPINNER_ID);
    THIS.IS_WAITING = true;
    THIS.EXECUTE_UPDATE.START()
      .finally(() => {
        if (!THIS.EXECUTE_UPDATE.SYSTEM_UPDATE_STATE.RUNNING) {
          THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
          THIS.IS_WAITING = false;
        }
        if (THIS.EXECUTE_UPDATE.SYSTEM_UPDATE_STATE.AVAILABLE && THIS.EXECUTE_UPDATE_INSTANTLY) {
          THIS.EXECUTE_SYSTEM_UPDATE();
        }
      });
  }

  public ngOnDestroy() {
    THIS.EXECUTE_UPDATE.STOP();
  }

  public executeSystemUpdate() {
    THIS.SERVICE.START_SPINNER_TRANSPARENT_BACKGROUND(THIS.SPINNER_ID);
    THIS.IS_WAITING = true;
    THIS.EXECUTE_UPDATE.EXECUTE_SYSTEM_UPDATE();
  }

  protected confirmationAlert: () => void = () => presentAlert(THIS.ALERT_CTRL, THIS.TRANSLATE, {
    message: THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.WARNING", { system: ENVIRONMENT.EDGE_SHORT_NAME }),
    subHeader: THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.SUB_HEADER"),
    buttons: [{
      text: THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.UPDATE_EXECUTE"),
      handler: () => THIS.EXECUTE_SYSTEM_UPDATE(),
    }],
  });

}
