// @ts-strict-ignore
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from "@angular/core";
import { AlertController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, presentAlert, Service, Websocket } from "src/app/shared/shared";
import { environment } from "src/environments";
import { ExecuteSystemUpdate } from "./executeSystemUpdate";
import { SystemUpdateState } from "./getSystemUpdateStateResponse";

@Component({
  selector: OeSystemUpdateComponent.SELECTOR,
  templateUrl: "./oe-system-update.component.html",
})
export class OeSystemUpdateComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "oe-system-update";

  @Output() public stateChanged: EventEmitter<SystemUpdateState> = new EventEmitter();
  @Input() public executeUpdateInstantly: boolean = false;
  @Input({ required: true }) public edge!: Edge;
  public readonly environment = environment;
  public readonly spinnerId: string = OeSystemUpdateComponent.SELECTOR;

  protected executeUpdate: ExecuteSystemUpdate | null = null;
  protected isWaiting: boolean;

  constructor(
    private websocket: Websocket,
    private service: Service,
    private alertCtrl: AlertController,
    private translate: TranslateService,
  ) { }

  ngOnInit() {
    this.executeUpdate = new ExecuteSystemUpdate(this.edge, this.websocket);

    this.executeUpdate.systemUpdateStateChange = (systemUpdateState) => {
      this.stateChanged.emit(systemUpdateState);
      if (systemUpdateState.updated) {
        this.service.stopSpinner(this.spinnerId);
        this.isWaiting = false;
      }
    };

    this.service.startSpinnerTransparentBackground(this.spinnerId);
    this.isWaiting = true;
    this.executeUpdate.start()
      .finally(() => {
        if (!this.executeUpdate.systemUpdateState.running) {
          this.service.stopSpinner(this.spinnerId);
          this.isWaiting = false;
        }
        if (this.executeUpdate.systemUpdateState.available && this.executeUpdateInstantly) {
          this.executeSystemUpdate();
        }
      });
  }

  public ngOnDestroy() {
    this.executeUpdate.stop();
  }

  public executeSystemUpdate() {
    this.service.startSpinnerTransparentBackground(this.spinnerId);
    this.isWaiting = true;
    this.executeUpdate.executeSystemUpdate();
  }

  protected confirmationAlert: () => void = () => presentAlert(this.alertCtrl, this.translate, {
    message: this.translate.instant("SETTINGS.SYSTEM_UPDATE.WARNING", { system: environment.edgeShortName }),
    subHeader: this.translate.instant("SETTINGS.SYSTEM_UPDATE.SUB_HEADER"),
    buttons: [{
      text: this.translate.instant("SETTINGS.SYSTEM_UPDATE.UPDATE_EXECUTE"),
      handler: () => this.executeSystemUpdate(),
    }],
  });

}
