// @ts-strict-ignore
import { inject, Injectable, Injector, signal, WritableSignal } from "@angular/core";
import { App } from "@capacitor/app";
import { Capacitor } from "@capacitor/core";
import { ScreenOrientation } from "@capacitor/screen-orientation";
import { AlertController, Platform, ToastController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { saveAs } from "file-saver-es";
import { DeviceDetectorService, DeviceType } from "ngx-device-detector";
import { BehaviorSubject, distinctUntilChanged, map, startWith, Subject, takeUntil, tap } from "rxjs";
import { environment } from "src/environments";
import { JsonrpcRequest } from "./shared/jsonrpc/base";
import { GetSetupProtocolRequest } from "./shared/jsonrpc/request/getSetupProtocolRequest";
import { Base64PayloadResponse } from "./shared/jsonrpc/response/base64PayloadResponse";
import { Service, Websocket } from "./shared/shared";

@Injectable()
export class PlatFormService {
    public static readonly platform: string = Capacitor.getPlatform();

    public static isActive: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(null);
    public static notifications: Map<string, { subscribe: JsonrpcRequest; unsubscribe: JsonrpcRequest }> = new Map();

    public isActiveAgain: WritableSignal<boolean> = signal(false);
    private injector: Injector = inject(Injector);
    private device: Device = new Device(this.injector);

    constructor(
        private alertCtrl: AlertController,
        private translate: TranslateService,
        private deviceService: DeviceDetectorService,
        private toaster: ToastController,
    ) {
        if (!this.device.isApp()) {
            return;
        }

        if (deviceService.isTablet() || deviceService.isDesktop()) {
            ScreenOrientation.lock({ orientation: "landscape" });
        } else {
            ScreenOrientation.lock({ orientation: "portrait" });
        }
    }

    public static handleRefresh() {
        setTimeout(() => window.location.reload(), 1000);
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

    public handleResize(platform: Platform, service: Service, ngUnsubscribe: Subject<void>) {
        platform.resize
            .pipe(
                startWith(null),
                takeUntil(ngUnsubscribe),
                map(() => ({
                    width: platform.width(),
                    height: platform.height(),
                })),
                tap(({ width, height }) => {
                    service.deviceWidth = width;
                    service.deviceHeight = height;
                }),
                distinctUntilChanged(),
            )
            .subscribe();
    }

    /**
     * Converts a base 64 encoded string to blob
     *
     * @param res The base 64 string
     * @returns Null, if string is invalid, else the blob
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

    public getDevice() {
        return this.device;
    }

    private async updateState() {
        const { isActive } = await App.getState();
        this.setIsActiveAgain(isActive);
        PlatFormService.isActive.next(isActive);
    }

    /**
     * Controls the reload behaviour after app was running in background und got active again
     *
     * @param isAppCurrentlyActive Is app currently active
     */
    private setIsActiveAgain(isAppCurrentlyActive: boolean) {
        if (isAppCurrentlyActive === true && PlatFormService.isActive?.getValue() === false) {
            this.isActiveAgain.set(true);
            return;
        }
        this.isActiveAgain.set(false);
    }
}

class Device {
    private static readonly SMARTPHONE_BP = 576;

    constructor(private injector: Injector) {}

    public getAppStoreLink(): string | null {
        const deviceDetectorService = this.injector.get(DeviceDetectorService);
        if (deviceDetectorService.isMobile()) {
            const deviceInfo = deviceDetectorService.getDeviceInfo();
            switch (deviceInfo.os) {
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

    public isSmartphone() {
        const platform = this.injector.get(Platform);
        return platform.width() <= Device.SMARTPHONE_BP;
    }

    public isTablet() {
        const deviceDetectorService = this.injector.get(DeviceDetectorService);
        return deviceDetectorService.isTablet();
    }

    /**
     * Checks if app or web-app
     *
     * @returns True, if current platform is not web
     */
    public isApp() {
        return Capacitor.getPlatform() !== "web";
    }

    public getDeviceType(): DeviceType {
        const deviceDetectorService = this.injector.get(DeviceDetectorService);
        return deviceDetectorService.deviceType();
    }

    public hasFileWritePermissions(): boolean {
        const translate = this.injector.get(TranslateService);
        if (this.isApp()) {
            this.toast(translate.instant("APP.FUNCTIONALITY_TEMPORARILY_NOT_AVAILABLE"), "warning");
            return false;
        }
        return true;
    }

    public async toast(message: string, level: "success" | "warning" | "danger", duration?: number) {
        const toaster = this.injector.get(ToastController);
        const toast = await toaster.create({
            message: message,
            color: level,
            duration: duration ?? 2000,
            cssClass: "container",
        });
        toast.present();
    }

    /**
     * Downloads the data as pdf
     *
     * @param data The data as blob
     * @param fileName The file name to save the pdf to
     */
    public downloadAsPdf(data: Blob, fileName: string) {
        if (!this.hasFileWritePermissions()) {
            return;
        }
        saveAs(data, fileName);
    }

    public async sendRequest(
        req: GetSetupProtocolRequest,
        websocket: Websocket,
    ): Promise<Base64PayloadResponse> | null {
        if (!this.hasFileWritePermissions()) {
            return null;
        }
        return (await websocket.sendRequest(req)) as Base64PayloadResponse;
    }

    getDeviceInfo() {
        const deviceDetectorService = this.injector.get(DeviceDetectorService);
        return deviceDetectorService.getDeviceInfo();
    }
}
