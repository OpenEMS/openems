// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { AlertController } from "@ionic/angular";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, Service, Utils, Websocket } from "../../../shared/shared";
import { InstallAppComponent } from "./INSTALL.COMPONENT";
import { DeleteAppInstance } from "./jsonrpc/deleteAppInstance";
import { GetAppAssistant } from "./jsonrpc/getAppAssistant";
import { GetAppInstances } from "./jsonrpc/getAppInstances";
import { UpdateAppInstance } from "./jsonrpc/updateAppInstance";

interface MyInstance {
  instanceId: string, // uuid
  form: FormGroup,
  isDeleting: boolean,
  isUpdating: boolean,
  fields: FormlyFieldConfig[]
  properties: {},
}

@Component({
  selector: UPDATE_APP_COMPONENT.SELECTOR,
  templateUrl: "./UPDATE.COMPONENT.HTML",
  standalone: false,
})
export class UpdateAppComponent implements OnInit {

  private static readonly SELECTOR = "app-update";
  public readonly spinnerId: string = UPDATE_APP_COMPONENT.SELECTOR;

  protected instances: MyInstance[] = [];
  protected appName: string | null = null;

  private edge: Edge | null = null;

  public constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private router: Router,
    private translate: TranslateService,
    private alertCtrl: AlertController,
  ) {
  }

  public ngOnInit() {
    THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
    const appId = THIS.ROUTE.SNAPSHOT.PARAMS["appId"];
    const appName = THIS.ROUTE.SNAPSHOT.QUERY_PARAMS["name"];
    THIS.SERVICE.SET_CURRENT_COMPONENT(appName, THIS.ROUTE).then(edge => {
      THIS.EDGE = edge;
      EDGE.SEND_REQUEST(THIS.WEBSOCKET,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GET_APP_INSTANCES.REQUEST({ appId: appId }),
        })).then(getInstancesResponse => {
          const recInstances = (getInstancesResponse as GET_APP_INSTANCES.RESPONSE).RESULT.INSTANCES;

          EDGE.SEND_REQUEST(THIS.WEBSOCKET,
            new ComponentJsonApiRequest({
              componentId: "_appManager",
              payload: new GET_APP_ASSISTANT.REQUEST({ appId: appId }),
            })).then(getAppAssistantResponse => {
              const appAssistant = (getAppAssistantResponse as GET_APP_ASSISTANT.RESPONSE).result;
              THIS.APP_NAME = APP_ASSISTANT.NAME;
              THIS.INSTANCES = [];
              for (const instance of recInstances) {
                const form = new FormGroup({});
                const model = {
                  "ALIAS": INSTANCE.ALIAS,
                  ...INSTANCE.PROPERTIES,
                };
                THIS.INSTANCES.PUSH({
                  instanceId: INSTANCE.INSTANCE_ID,
                  form: form,
                  isDeleting: false,
                  isUpdating: false,
                  fields: GET_APP_ASSISTANT.SET_INITIAL_MODEL(GET_APP_ASSISTANT.POSTPROCESS(structuredClone(appAssistant)).fields, structuredClone(model)),
                  properties: model,
                });
              }

              THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
            }).catch(INSTALL_APP_COMPONENT.ERROR_TOAST(THIS.SERVICE, error => "Error while receiving App Assistant for [" + appId + "]: " + error));
        }).catch(INSTALL_APP_COMPONENT.ERROR_TOAST(THIS.SERVICE, error => "Error while receiving App-Instances for [" + appId + "]: " + error));
    });
  }

  protected submit(instance: MyInstance) {
    THIS.SERVICE.START_SPINNER_TRANSPARENT_BACKGROUND(INSTANCE.INSTANCE_ID);
    INSTANCE.IS_UPDATING = true;
    // remove alias field from properties
    const alias = INSTANCE.FORM.VALUE["ALIAS"];
    const clonedFields = {};
    for (const item in INSTANCE.FORM.VALUE) {
      if (item != "ALIAS") {
        clonedFields[item] = INSTANCE.FORM.VALUE[item];
      }
    }
    INSTANCE.FORM.MARK_AS_PRISTINE();
    THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET,
      new ComponentJsonApiRequest({
        componentId: "_appManager",
        payload: new UPDATE_APP_INSTANCE.REQUEST({
          instanceId: INSTANCE.INSTANCE_ID,
          alias: alias,
          properties: clonedFields,
        }),
      })).then(response => {
        const result = (response as UPDATE_APP_INSTANCE.RESPONSE).result;

        if (RESULT.WARNINGS && RESULT.WARNINGS.LENGTH > 0) {
          THIS.SERVICE.TOAST(RESULT.WARNINGS.JOIN(";"), "warning");
        } else {
          THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.SUCCESS_UPDATE"), "success");
        }
        INSTANCE.PROPERTIES = RESULT.INSTANCE.PROPERTIES;
        INSTANCE.PROPERTIES["ALIAS"] = RESULT.INSTANCE.ALIAS;
      })
      .catch(INSTALL_APP_COMPONENT.ERROR_TOAST(THIS.SERVICE, error => THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.FAIL_UPDATE", { error: error })))
      .finally(() => {
        INSTANCE.IS_UPDATING = false;
        THIS.SERVICE.STOP_SPINNER(INSTANCE.INSTANCE_ID);
      });
  }

  protected async submitDelete(instance: MyInstance) {
    const translate = THIS.TRANSLATE;

    const alert = THIS.ALERT_CTRL.CREATE({
      subHeader: TRANSLATE.INSTANT("EDGE.CONFIG.APP.DELETE_CONFIRM_HEADLINE"),
      message: TRANSLATE.INSTANT("EDGE.CONFIG.APP.DELETE_CONFIRM_DESCRIPTION"),
      buttons: [{
        text: TRANSLATE.INSTANT("GENERAL.CANCEL"),
        role: "cancel",
      },
      {
        text: TRANSLATE.INSTANT("EDGE.CONFIG.APP.DELETE_CONFIRM"),
        handler: () => THIS.DELETE(instance),
      }],
      cssClass: "alertController",
    });
    (await alert).present();
  }

  protected delete(instance: MyInstance) {
    THIS.SERVICE.START_SPINNER_TRANSPARENT_BACKGROUND(INSTANCE.INSTANCE_ID);
    INSTANCE.IS_DELETING = true;
    THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET,
      new ComponentJsonApiRequest({
        componentId: "_appManager",
        payload: new DELETE_APP_INSTANCE.REQUEST({
          instanceId: INSTANCE.INSTANCE_ID,
        }),
      })).then(response => {
        THIS.INSTANCES.SPLICE(THIS.INSTANCES.INDEX_OF(instance), 1);
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.SUCCESS_DELETE"), "success");
        const navigationExtras = { state: { appInstanceChange: true } };
        THIS.ROUTER.NAVIGATE(["device/" + (THIS.EDGE.ID) + "/settings/app/"], navigationExtras);
      })
      .catch(INSTALL_APP_COMPONENT.ERROR_TOAST(THIS.SERVICE, error => THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.FAIL_DELETE", { error: error })))
      .finally(() => {
        INSTANCE.IS_DELETING = false;
        THIS.SERVICE.STOP_SPINNER(INSTANCE.INSTANCE_ID);
      });
  }
}
