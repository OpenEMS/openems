// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, Service, Utils, Websocket } from "../../../shared/shared";
import { AddAppInstance } from "./jsonrpc/addAppInstance";
import { GetAppAssistant } from "./jsonrpc/getAppAssistant";
import { AppCenter } from "./keypopup/appCenter";
import { AppCenterInstallAppWithSuppliedKeyRequest } from "./keypopup/appCenterInstallAppWithSuppliedKey";
import { AppCenterIsAppFree } from "./keypopup/appCenterIsAppFree";
import { KeyModalComponent, KeyValidationBehaviour } from "./keypopup/MODAL.COMPONENT";
import { hasPredefinedKey } from "./permissions";

@Component({
  selector: INSTALL_APP_COMPONENT.SELECTOR,
  templateUrl: "./INSTALL.COMPONENT.HTML",
  standalone: false,
})
export class InstallAppComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "app-install";
  public readonly spinnerId: string = INSTALL_APP_COMPONENT.SELECTOR;

  protected form: FormGroup | null = null;
  protected fields: FormlyFieldConfig[] | null = null;
  protected model: any | null = null;
  protected appName: string | null = null;
  protected isInstalling: boolean = false;

  private stopOnDestroy: Subject<void> = new Subject<void>();
  private key: string | null = null;
  private useMasterKey: boolean = false;
  private appId: string | null = null;
  private edge: Edge | null = null;
  private hasPredefinedKey: boolean = false;
  private isAppFree: boolean = false;

  public constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private modalController: ModalController,
    private router: Router,
    private translate: TranslateService,
  ) { }

  /**
 * Displays a error toast with the string supplied from the messageBuilder.
 * If the error is from a Jsonrpc call the error message gets extracted.
 *
 * @param service the service to open the toast with
 * @param messageBuilder the message supplier
 * @returns a method to handle a catch from a promise
 */
  public static errorToast(service: Service, messageBuilder: (reason) => string): (reason: any) => void {
    return (reason) => {
      if (REASON.ERROR) {
        reason = REASON.ERROR;
        if (REASON.MESSAGE) {
          reason = REASON.MESSAGE;
        }
      }
      CONSOLE.ERROR(reason);
      SERVICE.TOAST(messageBuilder(reason), "danger");
    };
  }

  public ngOnInit() {
    THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
    const state = history?.state;
    if (state) {
      if ("appKey" in state) {
        THIS.KEY = state["appKey"];
      }
      if ("useMasterKey" in state) {
        THIS.USE_MASTER_KEY = state["useMasterKey"];
      }
    }
    const appId = THIS.ROUTE.SNAPSHOT.PARAMS["appId"];
    const appName = THIS.ROUTE.SNAPSHOT.QUERY_PARAMS["name"];
    THIS.APP_ID = appId;
    THIS.SERVICE.SET_CURRENT_COMPONENT(appName, THIS.ROUTE).then(edge => {
      THIS.EDGE = edge;

      THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET,
        new APP_CENTER.REQUEST({
          payload: new APP_CENTER_IS_APP_FREE.REQUEST({
            appId: THIS.APP_ID,
          }),
        }),
      ).then(response => {
        const result = (response as APP_CENTER_IS_APP_FREE.RESPONSE).result;
        THIS.IS_APP_FREE = RESULT.IS_APP_FREE;
      }).catch(() => {
        THIS.IS_APP_FREE = false;
      });

      THIS.SERVICE.METADATA
        .pipe(takeUntil(THIS.STOP_ON_DESTROY))
        .subscribe(entry => {
          THIS.HAS_PREDEFINED_KEY = hasPredefinedKey(edge, ENTRY.USER);
        });
      EDGE.SEND_REQUEST(THIS.WEBSOCKET,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GET_APP_ASSISTANT.REQUEST({ appId: appId }),
        })).then(response => {
          const appAssistant = GET_APP_ASSISTANT.POSTPROCESS((response as GET_APP_ASSISTANT.RESPONSE).result);

          THIS.FIELDS = GET_APP_ASSISTANT.SET_INITIAL_MODEL(APP_ASSISTANT.FIELDS, {});
          THIS.APP_NAME = APP_ASSISTANT.NAME;
          THIS.MODEL = {};
          THIS.FORM = new FormGroup({});

        })
        .catch(INSTALL_APP_COMPONENT.ERROR_TOAST(THIS.SERVICE, error => "Error while receiving App Assistant for [" + appId + "]: " + error))
        .finally(() => {
          THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
        });
    });
  }

  public ngOnDestroy(): void {
    THIS.STOP_ON_DESTROY.NEXT();
    THIS.STOP_ON_DESTROY.COMPLETE();
  }

  /**
   * Submit for installing a app.
   */
  protected submit() {
    THIS.OBTAIN_KEY().then(key => {
      THIS.SERVICE.START_SPINNER_TRANSPARENT_BACKGROUND(THIS.APP_ID);
      // remove alias field from properties
      const alias = THIS.FORM.VALUE["ALIAS"];
      const clonedFields = {};
      for (const item in THIS.FORM.VALUE) {
        if (item !== "ALIAS") {
          clonedFields[item] = THIS.FORM.VALUE[item];
        }
      }

      let request: JsonrpcRequest = new ComponentJsonApiRequest({
        componentId: "_appManager",
        payload: new ADD_APP_INSTANCE.REQUEST({
          appId: THIS.APP_ID,
          alias: alias,
          properties: clonedFields,
          ...(key && { key: key }),
        }),
      });
      // if key not set send request with supplied key
      if (!key) {
        request = new APP_CENTER.REQUEST({
          payload: new APP_CENTER_INSTALL_APP_WITH_SUPPLIED_KEY_REQUEST.REQUEST({
            installRequest: request,
          }),
        });
      }

      THIS.IS_INSTALLING = true;
      THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, request).then(response => {
        const result = (response as ADD_APP_INSTANCE.RESPONSE).result;

        if (RESULT.INSTANCE) {
          RESULT.INSTANCE_ID = RESULT.INSTANCE.INSTANCE_ID;
          THIS.MODEL = RESULT.INSTANCE.PROPERTIES;
        }
        if (RESULT.WARNINGS && RESULT.WARNINGS.LENGTH > 0) {
          THIS.SERVICE.TOAST(RESULT.WARNINGS.JOIN(";"), "warning");
        } else {
          THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.SUCCESS_INSTALL"), "success");
        }

        THIS.FORM.MARK_AS_PRISTINE();
        const navigationExtras = { state: { appInstanceChange: true } };
        THIS.ROUTER.NAVIGATE(["device/" + (THIS.EDGE.ID) + "/settings/app/"], navigationExtras);
      })
        .catch(INSTALL_APP_COMPONENT.ERROR_TOAST(THIS.SERVICE, error => THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.FAIL_INSTALL", { error: error })))
        .finally(() => {
          THIS.IS_INSTALLING = false;
          THIS.SERVICE.STOP_SPINNER(THIS.APP_ID);
        });
    }).catch(() => {
      // can not get key => dont install
    });
  }

  /**
   * Gets the key to install the current app with.
   *
   * @returns the key or null if the predefined key gets used
   */
  private obtainKey(): Promise<string | null> {
    return new Promise<string | null>((resolve, reject) => {
      if (THIS.KEY) {
        resolve(THIS.KEY);
        return;
      }
      if (THIS.USE_MASTER_KEY) {
        resolve(null);
        return;
      }
      if (THIS.IS_APP_FREE) {
        resolve(null);
        return;
      }
      THIS.PRESENT_MODAL()
        .then(resolve)
        .catch(reject);
    });
  }

  // popup for key
  private async presentModal(): Promise<string> {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: KeyModalComponent,
      componentProps: {
        edge: THIS.EDGE,
        appId: THIS.APP_ID,
        behaviour: KEY_VALIDATION_BEHAVIOUR.SELECT,
        appName: THIS.APP_NAME,
      },
      cssClass: "auto-height",
    });

    const selectKeyPromise = new Promise<string>((resolve, reject) => {
      MODAL.ON_DID_DISMISS().then(event => {
        if (!EVENT.DATA) {
          reject();
          return; // no key selected
        }
        if (EVENT.DATA?.useMasterKey) {
          resolve(null);
          return;
        }
        if (EVENT.DATA?.key?.keyId) {
          resolve(EVENT.DATA.KEY.KEY_ID);
          return;
        }
        reject();
      });
    });

    await MODAL.PRESENT();
    return selectKeyPromise;
  }


}
