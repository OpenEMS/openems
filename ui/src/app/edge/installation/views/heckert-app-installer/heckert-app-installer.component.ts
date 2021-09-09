import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Service } from 'src/app/shared/shared';
import { InstallationData } from '../../installation.component';

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

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<InstallationData>();

  public apps: EmsApp[];

  public isHardyBarthVisible: boolean;
  public isConfirmed;

  constructor(private service: Service) { }

  public ngOnInit() {
    this.apps = [
      { id: EmsAppId.HardyBarthSingle, alias: "Hardy Barth Ladestation Single" },
      { id: EmsAppId.HardyBarthDouble, alias: "Hardy Barth Ladestation Double" },
      { id: EmsAppId.Keba, alias: "KEBA Ladestation" },
      { id: EmsAppId.HeatingElement, alias: "Heizstab" },
      { id: EmsAppId.HeatPump, alias: "Wärmepumpe ''SG-Ready''" },
      { id: EmsAppId.None, alias: "später" }
    ];

    this.isHardyBarthVisible = false;
    this.isConfirmed = false;
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    // Determine the selected app
    let selectedApp: EmsApp;

    for (let app of this.apps) {
      if (app.isSelected) {
        selectedApp = app;
      }
    }

    // Show message if nothing is selected
    if (!selectedApp) {
      this.service.toast("Wählen Sie eine App um fortzufahren.", "warning");
      return;
    }

    // Show message to confirm selection
    if (!this.isConfirmed) {
      this.service.toast("Bestätigen Sie Ihre aktuelle Auswahl, indem Sie nochmal auf ''weiter'' klicken.", "warning");
      this.isConfirmed = true;
      return;
    }

    // Save the id of the selected app and switch to next view
    this.installationData.selectedFreeApp = selectedApp;
    this.nextViewEvent.emit(this.installationData);
  }

  /**
   * Method that marks the clicked app as selected.
   * 
   * @param clickedApp the app that has been clicked
   */
  public selectApp(clickedApp: EmsApp) {
    // New selection must be confirmed
    this.isConfirmed = false;

    // Unselect all apps
    for (let app of this.apps) {
      app.isSelected = false;
    }

    // Select the clicked app
    clickedApp.isSelected = true;
  }
}