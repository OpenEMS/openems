import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';
import { GetApp } from './jsonrpc/getApp';
import { GetAppDescriptor } from './jsonrpc/getAppDescriptor';
import { GetApps } from './jsonrpc/getApps';
import { AppCenter } from './keypopup/appCenter';
import { AppCenterGetPossibleApps } from './keypopup/appCenterGetPossibleApps';
import { AppCenterIsAppFree } from './keypopup/appCenterIsAppFree';
import { KeyModalComponent, KeyValidationBehaviour } from './keypopup/modal.component';
import { canEnterKey, hasKeyModel, hasPredefinedKey } from './permissions';
import { InstallAppComponent } from './install.component';
import { TranslateService } from '@ngx-translate/core';
import { environment } from 'src/environments';

@Component({
  selector: SingleAppComponent.SELECTOR,
  templateUrl: './single.component.html',
})
export class SingleAppComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = 'app-single';
  public readonly spinnerId: string = SingleAppComponent.SELECTOR;

  public form: FormGroup | null = null;
  public model: any | null = null;

  private appId: string | null = null;
  private appName: string | null = null;
  private app: GetApps.App | null = null;
  private descriptor: GetAppDescriptor.AppDescriptor | null = null;
  private isXL: boolean = true;

  // for stopping spinner when all responses are recieved
  private readonly requestCount: number = 3;
  private receivedResponse: number = 0;

  private edge: Edge | null = null;

  private key: string | null = null;
  private useMasterKey: boolean = false;

  protected canEnterKey: boolean | undefined;
  protected hasPredefinedKey: boolean | undefined;

  private stopOnDestroy: Subject<void> = new Subject<void>();
  protected keyForFreeApps: string;
  protected isFreeApp: boolean = false;
  protected isPreInstalledApp: boolean = false;

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

  public ngOnInit() {
    this.service.startSpinner(this.spinnerId);
    this.updateIsXL();

    this.appId = this.route.snapshot.params['appId'];
    this.appName = this.route.snapshot.queryParams['name'];
    const appId = this.appId;
    this.service.setCurrentComponent(this.appName, this.route).then(edge => {
      this.edge = edge;

      this.edge.sendRequest(this.websocket,
        new AppCenter.Request({
          payload: new AppCenterIsAppFree.Request({
            appId: this.appId,
          }),
        }),
      ).then(response => {
        const result = (response as AppCenterIsAppFree.Response).result;
        this.isFreeApp = result.isAppFree;
      }).catch(() => {
        this.isFreeApp = false;
      });

      // update if the app is free depending of the configured key in the edge config
      if (hasKeyModel(this.edge)) {
        this.edge.getConfig(this.websocket).pipe(
          filter(config => config !== null),
          takeUntil(this.stopOnDestroy),
        ).subscribe(next => {
          const appManager = next.getComponent("_appManager");
          const newKeyForFreeApps = appManager.properties["keyForFreeApps"];
          if (!newKeyForFreeApps) {
            // no key in config
            this.increaseReceivedResponse();
          }
          if (this.keyForFreeApps === newKeyForFreeApps) {
            return;
          }
          this.keyForFreeApps = newKeyForFreeApps;
          // update free apps
          this.edge.sendRequest(this.websocket, new AppCenter.Request({
            payload: new AppCenterGetPossibleApps.Request({
              key: this.keyForFreeApps,
            }),
          })).then(response => {
            const result = (response as AppCenterGetPossibleApps.Response).result;
            this.isPreInstalledApp = result.bundles.some(bundle => {
              return bundle.some(app => {
                return app.appId == this.appId;
              });
            });
          }).finally(() => {
            this.increaseReceivedResponse();
          });
        });
      } else {
        this.isPreInstalledApp = false;
        this.increaseReceivedResponse();
      }

      this.service.metadata
        .pipe(takeUntil(this.stopOnDestroy))
        .subscribe(entry => {
          this.canEnterKey = canEnterKey(edge, entry.user);
          this.hasPredefinedKey = hasPredefinedKey(edge, entry.user);
        });

      // set appname, image ...
      const state = history?.state;
      if (state && 'app' in history.state) {
        if ('app' in history.state) {
          this.setApp(history.state.app);
        }
        if ('appKey' in history.state) {
          this.key = history.state.appKey;
        }
        if ('useMasterKey' in history.state) {
          this.useMasterKey = history.state.useMasterKey;
        }
      } else {
        edge.sendRequest(this.websocket,
          new ComponentJsonApiRequest({
            componentId: '_appManager',
            payload: new GetApp.Request({ appId: appId }),
          })).then(response => {
            const app = (response as GetApp.Response).result.app;
            app.imageUrl = environment.links.APP_CENTER.APP_IMAGE(this.translate.currentLang, app.appId);
            this.setApp(app);
          }).catch(reason => {
            console.error(reason.error);
            this.service.toast('Error while receiving App[' + appId + ']: ' + reason.error.message, 'danger');
          });
      }
      // set app descriptor
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: '_appManager',
          payload: new GetAppDescriptor.Request({ appId: appId }),
        })).then(response => {
          const descriptor = (response as GetAppDescriptor.Response).result;
          this.descriptor = GetAppDescriptor.postprocess(descriptor, this.sanitizer);
        })
        .catch(InstallAppComponent.errorToast(this.service, error => 'Error while receiving AppDescriptor for App[' + appId + ']: ' + error))
        .finally(() => {
          this.increaseReceivedResponse();
        });
    });
  }

  public ngOnDestroy(): void {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  @HostListener('window:resize', ['$event'])
  private onResize(event) {
    this.updateIsXL();
  }

  private updateIsXL() {
    this.isXL = 1200 <= window.innerWidth;
  }

  protected iFrameStyle() {
    const styles = {
      'height': (this.isXL) ? '100%' : window.innerHeight + 'px',
    };
    return styles;
  }

  private setApp(app: GetApps.App) {
    this.app = app;
    this.form = new FormGroup({});
    this.increaseReceivedResponse();
  }

  private increaseReceivedResponse() {
    this.receivedResponse++;
    if (this.receivedResponse == this.requestCount) {
      this.receivedResponse = 0;
      this.service.stopSpinner(this.spinnerId);
    }
  }

  // popup for key
  private async presentModal(appId: string, behaviour: KeyValidationBehaviour) {
    const modal = await this.modalController.create({
      component: KeyModalComponent,
      componentProps: {
        edge: this.edge,
        appId: appId,
        behaviour: behaviour,
        appName: this.appName,
      },
      cssClass: 'auto-height',
    });
    return await modal.present();
  }

  protected installApp(appId: string) {
    if (this.key || this.useMasterKey) {
      // if key already set navigate directly to installation view
      const state = this.useMasterKey ? { useMasterKey: true } : { appKey: this.key };
      this.router.navigate(['device/' + (this.edge.id) + '/settings/app/install/' + this.appId]
        , { queryParams: { name: this.appName }, state: state });
      return;
    }
    // if the version is not high enough and the edge doesnt support installing apps via keys directly navigate to installation
    if (!hasKeyModel(this.edge) || this.isFreeApp) {
      this.router.navigate(['device/' + (this.edge.id) + '/settings/app/install/' + this.appId]
        , { queryParams: { name: this.appName } });
      return;
    }
    // show modal to let the user enter a key
    this.presentModal(appId, KeyValidationBehaviour.NAVIGATE);
  }

  protected registerKey(appId: string) {
    this.presentModal(appId, KeyValidationBehaviour.REGISTER);
  }

}
