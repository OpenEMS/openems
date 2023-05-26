import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { AlertController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Service } from 'src/app/shared/shared';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { IbnUtils } from '../../shared/ibnutils';

export enum EmsAppId {
  HardyBarthSingle = "hardyBarthSingle",
  HardyBarthDouble = "hardyBarthDouble",
  Keba = "keba",
  HeatingElement = "heatingElement",
  HeatPump = "heatPump",
  None = "none"
}

export type EmsApp = {
  id: EmsAppId,
  alias: string,
  isSelected?: boolean
}

@Component({
  selector: HeckertAppInstallerComponent.SELECTOR,
  templateUrl: './heckert-app-installer.component.html',
  styleUrls: ['./heckert-app-installer.scss']
})
export class HeckertAppInstallerComponent implements OnInit {
  private static readonly SELECTOR = "heckert-app-installer";

  @Input() public ibn: AbstractIbn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public apps: EmsApp[];
  public isHardyBarthVisible: boolean = false;

  constructor(
    private service: Service,
    private alertCtrl: AlertController,
    protected translate: TranslateService) { }

  public ngOnInit() {
    this.apps = [
      { id: EmsAppId.HardyBarthSingle, alias: "Hardy Barth Ladestation Single" },
      { id: EmsAppId.HardyBarthDouble, alias: "Hardy Barth Ladestation Double" },
      { id: EmsAppId.Keba, alias: "KEBA Ladestation" },
      { id: EmsAppId.HeatingElement, alias: "Heizstab" },
      { id: EmsAppId.HeatPump, alias: "Wärmepumpe ''SG-Ready''" },
      { id: EmsAppId.None, alias: "später" }
    ];
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public saveConfirmedApp() {
    // Determine the selected app
    let selectedApp: EmsApp;

    for (let app of this.apps) {
      if (app.isSelected) {
        selectedApp = app;
      }
    }

    // Save the id of the selected app and switch to next view
    this.ibn.selectedFreeApp = selectedApp;
    IbnUtils.addIbnToSessionStorage(this.ibn);
    this.nextViewEvent.emit(this.ibn);
  }

  public onNextClicked() {
    if (this.ibn.selectedFreeApp) {
      this.nextViewEvent.emit(this.ibn);
    } else {
      this.service.toast(this.translate.instant('INSTALLATION.PLEASE_SELECT_OPTION'), 'danger');
    }
  }

  /**
   * Method that marks the clicked app as selected.
   * 
   * @param clickedApp the app that has been clicked
   */
  public selectApp(clickedApp: EmsApp) {

    // Unselect all apps
    for (let app of this.apps) {
      app.isSelected = false;
    }

    // Select the clicked app
    clickedApp.isSelected = true;
    this.presentAlert(clickedApp);
  }

  /**
   * Method that shows a confirmation window for the app selection
   * 
   * @param clickedApp the app that has been clicked
   */
  async presentAlert(clickedApp: EmsApp) {

    let isAppSelected = clickedApp.id != EmsAppId.None;
    let alert = this.alertCtrl.create({
      header: (isAppSelected ? this.translate.instant('INSTALLATION.CONFIRM_APP_SELECTION') : this.translate.instant('INSTALLATION.CONFIRM_APP_SELECTION_LATER')),
      subHeader: isAppSelected ? (clickedApp.alias ?? clickedApp.id) : '',
      message: isAppSelected ? this.translate.instant('INSTALLATION.ATTENTION_MESSAGE') : '',
      buttons: [{
        text: this.translate.instant('INSTALLATION.BACK'),
        role: 'cancel'
      },
      {
        text: this.translate.instant('INSTALLATION.FORWARD'),
        handler: () => {
          this.saveConfirmedApp();
        },
      }],
      cssClass: 'alertController',
    });
    (await alert).present();
  }
}