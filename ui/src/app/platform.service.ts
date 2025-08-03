// @ts-strict-ignore
import { Injectable, signal, WritableSignal, inject } from "@angular/core";
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
  private alertCtrl = inject(AlertController);
  private translate = inject(TranslateService);
  private deviceService = inject(DeviceDetectorService);
  private toaster = inject(ToastController);


  public static readonly platform: string = Capacitor.getPlatform();

  public static isActive: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(null);
  public static deviceInfo: DeviceInfo;
  public static notifications: Map<string, { subscribe: JsonrpcRequest, unsubscribe: JsonrpcRequest }> = new Map();
  private static isMobile: boolean = false;

  public isActiveAgain: WritableSignal<boolean> = signal(false);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    PlatFormService.deviceInfo = this.deviceService.getDeviceInfo();
    PlatFormService.isMobile = this.deviceService.isMobile();
  }

  public static handleRefresh() {
    setTimeout(() =>
      window.location.reload()
      , 1000);
  }

  public static getAppStoreLink(): string | null {
    if (this.isMobile) {
      switch (PlatFormService.deviceInfo.os) {
        case "iOS":
          return environment.links.APP.IOS;
        case "Android":
          return environment.links.APP.ANDROID;
        default:
          return null;
      }
    }
    return null;
  }

  public listen() {
    // Don't use in web
    if (PlatFormService.platform === "web") {
      return;
    }

    this.updateState();

    App.addListener("appStateChange", () => {
      this.updateState();
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

    const binary = atob(res.result.payload.replace(/\s/g, ""));
    const length = binary.length;

    const buffer = new ArrayBuffer(length);
    const view = new Uint8Array(buffer);

    for (let i = 0; i < length; i++) {
      view[i] = binary.charCodeAt(i);
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

    if (!this.deviceHasFilePermissions()) {
      return;
    }
    saveAs(data, fileName);
  }

  public deviceHasFilePermissions(): boolean {
    if (this.getIsApp()) {
      this.toast(this.translate.instant("APP.FUNCTIONALITY_TEMPORARILY_NOT_AVAILABLE"), "warning");
      return false;
    }
    return true;
  }

  public async sendRequest(req: GetSetupProtocolRequest, websocket: Websocket): Promise<Base64PayloadResponse> | null {
    if (!this.deviceHasFilePermissions()) {
      return null;
    }
    return await websocket.sendRequest(req) as Base64PayloadResponse;
  }
  /**
   * Method that shows a confirmation window for the app selection
  *
  * @param clickedApp the app that has been clicked
  */
  public async presentAlert(header: string, message: string, successCallback: () => void) {
    const alert = this.alertCtrl.create({
      header: header,
      message: message,
      buttons: [{
        text: this.translate.instant("INSTALLATION.BACK"),
        role: "cancel",
      },
      {
        text: this.translate.instant("INSTALLATION.FORWARD"),
        handler: () => {
          successCallback();
        },
      }],
      cssClass: "alertController",
    });
    (await alert).present();
  }

  public async toast(message: string, level: "success" | "warning" | "danger", duration?: number) {
    const toast = await this.toaster.create({
      message: message,
      color: level,
      duration: duration ?? 2000,
      cssClass: "container",
    });
    toast.present();
  }

  /**
   * Checks if app or web-app
   *
   * @returns true, if current platform is not web
   */
  public getIsApp() {
    return Capacitor.getPlatform() !== "web";
  }

  private async updateState() {
    const { isActive } = await App.getState();
    this.setIsActiveAgain(isActive);
    PlatFormService.isActive.next(isActive);
  }

  /**
   * Controls the reload behaviour after app was running in background und got active again
   *
   * @param isAppCurrentlyActive is app currently active
   */
  private setIsActiveAgain(isAppCurrentlyActive: boolean) {

    if (isAppCurrentlyActive === true
      && PlatFormService.isActive?.getValue() === false) {
      this.isActiveAgain.set(true);
      return;
    }
    this.isActiveAgain.set(false);
  }
}

