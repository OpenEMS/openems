// @ts-strict-ignore
import { Injectable } from "@angular/core";
import { App } from "@capacitor/app";
import { Capacitor } from "@capacitor/core";
import { Directory, Encoding, Filesystem } from "@capacitor/filesystem";
import { AlertController } from "@ionic/angular";
import { FileOpener } from "@ionic-native/file-opener";
import { TranslateService } from "@ngx-translate/core";
import { saveAs } from "file-saver-es";
import { DeviceDetectorService, DeviceInfo } from "ngx-device-detector";
import { BehaviorSubject, Subject } from "rxjs";
import { environment } from "src/environments";
import { JsonrpcRequest } from "./shared/jsonrpc/base";
import { PreviousRouteService } from "./shared/service/previousRouteService";
import { ArrayUtils } from "./shared/utils/array/array.utils";

@Injectable()
export class AppService {

  public static readonly platform: string = Capacitor.getPlatform();

  public static isActive: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(null);
  public static lastActive: Subject<Date> = new Subject();
  public static deviceInfo: DeviceInfo;
  public static notifications: Map<string, { subscribe: JsonrpcRequest, unsubscribe: JsonrpcRequest }> = new Map();

  private static isMobile: boolean = false;

  constructor(
    private alertCtrl: AlertController,
    private translate: TranslateService,
    private deviceService: DeviceDetectorService,
    private routeService: PreviousRouteService,
  ) {
    AppService.deviceInfo = this.deviceService.getDeviceInfo();
    AppService.isMobile = this.deviceService.isMobile();
  }

  public static handleRefresh() {
    setTimeout(() =>
      window.location.reload()
      , 1000);
  }

  public static getAppStoreLink(): string | null {
    if (this.isMobile) {
      switch (AppService.deviceInfo.os) {
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

  static async writeAndOpenFile(data: Blob, fileName: string) {
    if (!AppService.platform) {
      saveAs(data, fileName);
    }

    const reader = new FileReader();
    reader.readAsDataURL(data);
    reader.onloadend = async function () {
      try {
        const result = await Filesystem.writeFile({
          path: fileName,
          data: reader.result.toString(),
          directory: Directory.Data,
          recursive: true,
          encoding: Encoding.UTF8,
        });

        FileOpener.open(result.uri, data.type)
          .then(() => console.log("File is opened"))
          .catch(e => console.log("Error opening file", e));

        console.log("Wrote file", result.uri);
      } catch (e) {
        console.error("Unable to write file", e);
      }
    };
  }

  public listen() {
    // Don't use in web
    if (AppService.platform === "web") {
      return;
    }

    this.updateState();

    App.addListener("appStateChange", () => {
      this.updateState();
    });
  }

  public async downloadFile(path: string, blob: Blob, fileName: string) {
    // await this.presentAlert("Di", "asd", () => { }).then((state) => {
    //   console.log("state", state);
    // });

    fileName = "test.txt";

    const writeSecretFile = async () => {
      await Filesystem.writeFile({
        path: fileName,
        data: "this is a test",
        directory: Directory.Data,
        encoding: Encoding.UTF8,
      });
    };

    // const openFile = async () => {
    //   const { uri } = await Filesystem.getUri({ path: fileName, directory: Directory.Data });

    //   let fOpts = {
    //     filePath: uri + fileName,
    //     openWithDefault: true
    //   }

    //   FileOpener.open(fOpts);
    // }

    const readSecretFile = async () => {
      const contents = await Filesystem.readFile({
        path: fileName,
        directory: Directory.Documents,
        encoding: Encoding.UTF8,
      });

      console.log("secrets:", contents);
    };

    await writeSecretFile();
    // await openFile();
    await readSecretFile();
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

  private async updateState() {
    const { isActive } = await App.getState();
    this.reloadBehavior(isActive);
    AppService.isActive.next(isActive);
  }

  /**
   * Controls the reload behaviour after app was running in background und got active again
   *
   * @param isAppCurrentlyActive is app currently active
   */
  private reloadBehavior(isAppCurrentlyActive: boolean) {

    const route = this.routeService.getCurrentUrl();
    const isForbiddenToReload: boolean = ArrayUtils.containsStrings(route.split("/"), FORBIDDEN_ROUTES_TO_RELOAD);

    if (isAppCurrentlyActive === true
      && AppService.isActive?.getValue() === false
      && !isForbiddenToReload) {
      window.location.reload();
    }
  }
}

export const FORBIDDEN_ROUTES_TO_RELOAD: string[] = ["login", "installation", "index"];
