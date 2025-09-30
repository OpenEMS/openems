// @ts-strict-ignore
import { Component, HostListener, OnDestroy, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { DomSanitizer } from "@angular/platform-browser";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { filter, takeUntil } from "rxjs/operators";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { environment } from "src/environments";
import { Edge, Service, Utils, Websocket } from "../../../shared/shared";
import { InstallAppComponent } from "./INSTALL.COMPONENT";
import { GetApp } from "./jsonrpc/getApp";
import { GetAppDescriptor } from "./jsonrpc/getAppDescriptor";
import { GetApps } from "./jsonrpc/getApps";
import { AppCenter } from "./keypopup/appCenter";
import { AppCenterGetPossibleApps } from "./keypopup/appCenterGetPossibleApps";
import { AppCenterIsAppFree } from "./keypopup/appCenterIsAppFree";
import { KeyModalComponent, KeyValidationBehaviour } from "./keypopup/MODAL.COMPONENT";
import { canEnterKey, hasKeyModel, hasPredefinedKey } from "./permissions";

@Component({
  selector: SINGLE_APP_COMPONENT.SELECTOR,
  templateUrl: "./SINGLE.COMPONENT.HTML",
  standalone: false,
})
export class SingleAppComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "app-single";
  public readonly spinnerId: string = SINGLE_APP_COMPONENT.SELECTOR;

  public form: FormGroup | null = null;
  public model: any | null = null;

  protected canEnterKey: boolean | undefined;
  protected hasPredefinedKey: boolean | undefined;
  protected keyForFreeApps: string;
  protected isFreeApp: boolean = false;
  protected isPreInstalledApp: boolean = false;

  private appId: string | null = null;
  private appName: string | null = null;
  private app: GET_APPS.APP | null = null;
  private descriptor: GET_APP_DESCRIPTOR.APP_DESCRIPTOR | null = null;
  private isXL: boolean = true;
  // for stopping spinner when all responses are recieved
  private readonly requestCount: number = 3;
  private receivedResponse: number = 0;
  private edge: Edge | null = null;
  private key: string | null = null;
  private useMasterKey: boolean = false;
  private stopOnDestroy: Subject<void> = new Subject<void>();

  public constructor(
    private route: ActivatedRoute,
    private router: Router,
    protected utils: Utils,
    private websocket: Websocket,
    private translate: TranslateService,
    private service: Service,
    private sanitizer: DomSanitizer,
    protected modalController: ModalController,
  ) {
  }

  @HostListener("window:resize", ["$event"])
  private onResize(event) {
    THIS.UPDATE_IS_XL();
  }

  public ngOnInit() {
    THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
    THIS.UPDATE_IS_XL();

    THIS.APP_ID = THIS.ROUTE.SNAPSHOT.PARAMS["appId"];
    THIS.APP_NAME = THIS.ROUTE.SNAPSHOT.QUERY_PARAMS["name"];
    const appId = THIS.APP_ID;
    THIS.SERVICE.SET_CURRENT_COMPONENT(THIS.APP_NAME, THIS.ROUTE).then(edge => {
      THIS.EDGE = edge;

      THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET,
        new APP_CENTER.REQUEST({
          payload: new APP_CENTER_IS_APP_FREE.REQUEST({
            appId: THIS.APP_ID,
          }),
        }),
      ).then(response => {
        const result = (response as APP_CENTER_IS_APP_FREE.RESPONSE).result;
        THIS.IS_FREE_APP = RESULT.IS_APP_FREE;
      }).catch(() => {
        THIS.IS_FREE_APP = false;
      });

      // update if the app is free depending of the configured key in the edge config
      if (hasKeyModel(THIS.EDGE)) {
        THIS.EDGE.GET_CONFIG(THIS.WEBSOCKET).pipe(
          filter(config => config !== null),
          takeUntil(THIS.STOP_ON_DESTROY),
        ).subscribe(next => {
          const appManager = NEXT.GET_COMPONENT("_appManager");
          const newKeyForFreeApps = APP_MANAGER.PROPERTIES["keyForFreeApps"];
          if (!newKeyForFreeApps) {
            // no key in config
            THIS.INCREASE_RECEIVED_RESPONSE();
          }
          if (THIS.KEY_FOR_FREE_APPS === newKeyForFreeApps) {
            return;
          }
          THIS.KEY_FOR_FREE_APPS = newKeyForFreeApps;
          // update free apps
          THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, new APP_CENTER.REQUEST({
            payload: new APP_CENTER_GET_POSSIBLE_APPS.REQUEST({
              key: THIS.KEY_FOR_FREE_APPS,
            }),
          })).then(response => {
            const result = (response as APP_CENTER_GET_POSSIBLE_APPS.RESPONSE).result;
            THIS.IS_PRE_INSTALLED_APP = RESULT.BUNDLES.SOME(bundle => {
              return BUNDLE.SOME(app => {
                return APP.APP_ID == THIS.APP_ID;
              });
            });
          }).finally(() => {
            THIS.INCREASE_RECEIVED_RESPONSE();
          });
        });
      } else {
        THIS.IS_PRE_INSTALLED_APP = false;
        THIS.INCREASE_RECEIVED_RESPONSE();
      }

      THIS.SERVICE.METADATA
        .pipe(takeUntil(THIS.STOP_ON_DESTROY))
        .subscribe(entry => {
          THIS.CAN_ENTER_KEY = canEnterKey(edge, ENTRY.USER);
          THIS.HAS_PREDEFINED_KEY = hasPredefinedKey(edge, ENTRY.USER);
        });

      // set appname, image ...
      const state = history?.state;
      if (state && "app" in HISTORY.STATE) {
        if ("app" in HISTORY.STATE) {
          THIS.SET_APP(HISTORY.STATE.APP);
        }
        if ("appKey" in HISTORY.STATE) {
          THIS.KEY = HISTORY.STATE.APP_KEY;
        }
        if ("useMasterKey" in HISTORY.STATE) {
          THIS.USE_MASTER_KEY = HISTORY.STATE.USE_MASTER_KEY;
        }
      } else {
        EDGE.SEND_REQUEST(THIS.WEBSOCKET,
          new ComponentJsonApiRequest({
            componentId: "_appManager",
            payload: new GET_APP.REQUEST({ appId: appId }),
          })).then(response => {
            const app = (response as GET_APP.RESPONSE).RESULT.APP;
            APP.IMAGE_URL = ENVIRONMENT.LINKS.APP_CENTER.APP_IMAGE(THIS.TRANSLATE.CURRENT_LANG, APP.APP_ID);
            THIS.SET_APP(app);
          }).catch(reason => {
            CONSOLE.ERROR(REASON.ERROR);
            THIS.SERVICE.TOAST("Error while receiving App[" + appId + "]: " + REASON.ERROR.MESSAGE, "danger");
          });
      }
      // set app descriptor
      EDGE.SEND_REQUEST(THIS.WEBSOCKET,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GET_APP_DESCRIPTOR.REQUEST({ appId: appId }),
        })).then(response => {
          const descriptor = (response as GET_APP_DESCRIPTOR.RESPONSE).result;
          THIS.DESCRIPTOR = GET_APP_DESCRIPTOR.POSTPROCESS(descriptor, THIS.SANITIZER);
        })
        .catch(INSTALL_APP_COMPONENT.ERROR_TOAST(THIS.SERVICE, error => "Error while receiving AppDescriptor for App[" + appId + "]: " + error))
        .finally(() => {
          THIS.INCREASE_RECEIVED_RESPONSE();
        });
    });
  }

  public ngOnDestroy(): void {
    THIS.STOP_ON_DESTROY.NEXT();
    THIS.STOP_ON_DESTROY.COMPLETE();
  }

  protected iFrameStyle() {
    const styles = {
      "height": (THIS.IS_XL) ? "100%" : WINDOW.INNER_HEIGHT + "px",
    };
    return styles;
  }

  protected installApp(appId: string) {
    if (THIS.KEY || THIS.USE_MASTER_KEY) {
      // if key already set navigate directly to installation view
      const state = THIS.USE_MASTER_KEY ? { useMasterKey: true } : { appKey: THIS.KEY };
      THIS.ROUTER.NAVIGATE(["device/" + (THIS.EDGE.ID) + "/settings/app/install/" + THIS.APP_ID]
        , { queryParams: { name: THIS.APP_NAME }, state: state });
      return;
    }
    // if the version is not high enough and the edge doesnt support installing apps via keys directly navigate to installation
    if (!hasKeyModel(THIS.EDGE) || THIS.IS_FREE_APP) {
      THIS.ROUTER.NAVIGATE(["device/" + (THIS.EDGE.ID) + "/settings/app/install/" + THIS.APP_ID]
        , { queryParams: { name: THIS.APP_NAME } });
      return;
    }
    // show modal to let the user enter a key
    THIS.PRESENT_MODAL(appId, KEY_VALIDATION_BEHAVIOUR.NAVIGATE);
  }

  protected registerKey(appId: string) {
    THIS.PRESENT_MODAL(appId, KEY_VALIDATION_BEHAVIOUR.REGISTER);
  }

  private updateIsXL() {
    THIS.IS_XL = 1200 <= WINDOW.INNER_WIDTH;
  }

  private setApp(app: GET_APPS.APP) {
    THIS.APP = app;
    THIS.FORM = new FormGroup({});
    THIS.INCREASE_RECEIVED_RESPONSE();
  }

  private increaseReceivedResponse() {
    THIS.RECEIVED_RESPONSE++;
    if (THIS.RECEIVED_RESPONSE == THIS.REQUEST_COUNT) {
      THIS.RECEIVED_RESPONSE = 0;
      THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
    }
  }

  // popup for key
  private async presentModal(appId: string, behaviour: KeyValidationBehaviour) {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: KeyModalComponent,
      componentProps: {
        edge: THIS.EDGE,
        appId: appId,
        behaviour: behaviour,
        appName: THIS.APP_NAME,
      },
      cssClass: "auto-height",
    });
    return await MODAL.PRESENT();
  }

}
