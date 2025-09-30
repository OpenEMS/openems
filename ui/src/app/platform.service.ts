// @ts-strict-ignore
import { Injectable, signal, WritableSignal } from "@angular/core";
import { App } from "@capacitor/app";
import { Capacitor } from "@capacitor/core";
import { AlertController, ToastController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { saveAs } from "file-saver-es";
import { DeviceDetectorService, DeviceInfo } from "ngx-device-detector";
import { BehaviorSubject } from "rxjs";
import { environment } from "src/environments";
import { JsonrpcRequest } from "./shared/jsonrpc/base";
import { GetSetupProtocolRequest } from "./shared/jsonrpc/request/getSetupProtocolRequest";
import { Base64PayloadResponse } from "./shared/jsonrpc/response/base64PayloadResponse";
import { Websocket } from "./shared/shared";

@Injectable()
export class PlatFormService {

  public static readonly platform: string = CAPACITOR.GET_PLATFORM();

  public static isActive: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(null);
  public static deviceInfo: DeviceInfo;
  public static notifications: Map<string, { subscribe: JsonrpcRequest, unsubscribe: JsonrpcRequest }> = new Map();
  private static isMobile: boolean = false;

  public isActiveAgain: WritableSignal<boolean> = signal(false);

  constructor(
    private alertCtrl: AlertController,
    private translate: TranslateService,
    private deviceService: DeviceDetectorService,
    private toaster: ToastController,
  ) {
    PLAT_FORM_SERVICE.DEVICE_INFO = THIS.DEVICE_SERVICE.GET_DEVICE_INFO();
    PLAT_FORM_SERVICE.IS_MOBILE = THIS.DEVICE_SERVICE.IS_MOBILE();
  }

  public static handleRefresh() {
    setTimeout(() =>
      WINDOW.LOCATION.RELOAD()
      , 1000);
  }

  public static getAppStoreLink(): string | null {
    if (THIS.IS_MOBILE) {
      switch (PLAT_FORM_SERVICE.DEVICE_INFO.OS) {
        case "iOS":
          return ENVIRONMENT.LINKS.APP.IOS;
        case "Android":
          return ENVIRONMENT.LINKS.APP.ANDROID;
        default:
          return null;
      }
    }
    return null;
  }

  public listen() {
    // Don't use in web
    if (PLAT_FORM_SERVICE.PLATFORM === "web") {
      return;
    }

    THIS.UPDATE_STATE();

    APP.ADD_LISTENER("appStateChange", () => {
      THIS.UPDATE_STATE();
    });
  }

  /**
   * Converts a base 64 encoded string to blob
  *
  * @param res the base 64 string
  * @returns null, if string is invalid, else the blob
  */
  public convertBase64ToBlob(res: Base64PayloadResponse | null): Blob | null {

    if (!res?.result?.payload) {
      return null;
    }

    const binary = atob(RES.RESULT.PAYLOAD.REPLACE(/\s/g, ""));
    const length = BINARY.LENGTH;

    const buffer = new ArrayBuffer(length);
    const view = new Uint8Array(buffer);

    for (let i = 0; i < length; i++) {
      view[i] = BINARY.CHAR_CODE_AT(i);
    }

    const data: Blob = new Blob([view], {
      type: "application/pdf",
    });

    return data;
  }

  /**
   * Downloads the data as pdf
  *
  * @param data the data as blob
  * @param fileName the file name to save the pdf to
  */
  public downloadAsPdf(data: Blob, fileName: string) {

    if (!THIS.DEVICE_HAS_FILE_PERMISSIONS()) {
      return;
    }
    saveAs(data, fileName);
  }

  public deviceHasFilePermissions(): boolean {
    if (THIS.GET_IS_APP()) {
      THIS.TOAST(THIS.TRANSLATE.INSTANT("APP.FUNCTIONALITY_TEMPORARILY_NOT_AVAILABLE"), "warning");
      return false;
    }
    return true;
  }

  public async sendRequest(req: GetSetupProtocolRequest, websocket: Websocket): Promise<Base64PayloadResponse> | null {
    if (!THIS.DEVICE_HAS_FILE_PERMISSIONS()) {
      return null;
    }
    return await WEBSOCKET.SEND_REQUEST(req) as Base64PayloadResponse;
  }
  /**
   * Method that shows a confirmation window for the app selection
  *
  * @param clickedApp the app that has been clicked
  */
  public async presentAlert(header: string, message: string, successCallback: () => void) {
    const alert = THIS.ALERT_CTRL.CREATE({
      header: header,
      message: message,
      buttons: [{
        text: THIS.TRANSLATE.INSTANT("INSTALLATION.BACK"),
        role: "cancel",
      },
      {
        text: THIS.TRANSLATE.INSTANT("INSTALLATION.FORWARD"),
        handler: () => {
          successCallback();
        },
      }],
      cssClass: "alertController",
    });
    (await alert).present();
  }

  public async toast(message: string, level: "success" | "warning" | "danger", duration?: number) {
    const toast = await THIS.TOASTER.CREATE({
      message: message,
      color: level,
      duration: duration ?? 2000,
      cssClass: "container",
    });
    TOAST.PRESENT();
  }

  /**
   * Checks if app or web-app
   *
   * @returns true, if current platform is not web
   */
  public getIsApp() {
    return CAPACITOR.GET_PLATFORM() !== "web";
  }

  private async updateState() {
    const { isActive } = await APP.GET_STATE();
    THIS.SET_IS_ACTIVE_AGAIN(isActive);
    PLAT_FORM_SERVICE.IS_ACTIVE.NEXT(isActive);
  }

  /**
   * Controls the reload behaviour after app was running in background und got active again
   *
   * @param isAppCurrentlyActive is app currently active
   */
  private setIsActiveAgain(isAppCurrentlyActive: boolean) {

    if (isAppCurrentlyActive === true
      && PLAT_FORM_SERVICE.IS_ACTIVE?.getValue() === false) {
      THIS.IS_ACTIVE_AGAIN.SET(true);
      return;
    }
    THIS.IS_ACTIVE_AGAIN.SET(false);
  }
}

