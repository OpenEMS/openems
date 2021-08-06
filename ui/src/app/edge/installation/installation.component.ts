import { Component, OnInit } from '@angular/core';
import { Edge } from 'src/app/shared/shared';
import { AcPv } from './views/protocol-additional-ac-producers/protocol-additional-ac-producers.component';
import { FeedInSetting } from './views/protocol-dynamic-feed-in-limitation/protocol-dynamic-feed-in-limitation.component';
import { DcPv } from './views/protocol-pv/protocol-pv.component';

enum View {
  Completion,
  ConfigurationEmergencyReserve,
  ConfigurationExecute,
  ConfigurationLineSideMeterFuse,
  ConfigurationSummary,
  ConfigurationSystem,
  PreInstallation,
  ProtocolAdditionalAcProducers,
  ProtocolCompletion,
  ProtocolCustomer,
  ProtocolDynamicFeedInLimitation,
  ProtocolInstaller,
  ProtocolPv,
  ProtocolSerialNumbers,
  ProtocolSystem
};

export type InstallationData = {

  // protocol-installer
  installer?: {
    companyName: string,
    lastName: string,
    firstName: string,
    street: string,
    zip: string,
    city: string,
    country: string,
    email: string,
    phone: string
  },

  // protocol-customer
  customer?: {
    isCorporateClient: boolean,
    companyName: string,
    lastName: string,
    firstName: string,
    street: string,
    zip: string,
    city: string,
    country: string,
    email: string,
    emailConfirm: string,
    phone: string
  },

  // protocol-system
  location?: {
    isEqualToCustomerData: boolean,
    isCorporateClient: boolean,
    companyName: string,
    lastName: string,
    firstName: string,
    street: string,
    zip: string,
    city: string,
    country: string,
    email?: string,
    phone?: string
  },

  edge?: Edge,

  battery: {
    // configuration-system
    type?: string,
    // protocol-serial-numbers
    serialNumbers: {
      tower1: {
        label: string,
        value: string
      }[],
      tower2: {
        label: string,
        value: string
      }[],
      tower3: {
        label: string,
        value: string
      }[]
    },
    // configuration-emergency-reserve
    emergencyReserve?: {
      isEnabled: boolean,
      value: number
    },
  },

  misc: {
    // configuration-line-side-meter-fuse
    lineSideMeterFuse?: {
      fixedValue: number,
      otherValue: number
    }
  },

  batteryInverter?: {
    // -> Channel
    type: string,
    serialNumber: string,
    // protocol-dynamic-feed-in-limitation
    dynamicFeedInLimitation?: {
      maximumFeedInPower: number,
      feedInSetting: FeedInSetting,
      fixedPowerFactor: FeedInSetting
    }
  },

  // protocol-pv
  pv: {
    dc1?: DcPv,
    dc2?: DcPv,
    ac?: AcPv[]
  }

  // protocol-serial-numbers
  setupProtocolId?: string
}

export const COUNTRY_OPTIONS: { value: string, label: string }[] = [
  { value: "de", label: "Deutschland" },
  { value: "at", label: "Österreich" },
  { value: "ch", label: "Schweiz" }
];

@Component({
  selector: InstallationComponent.SELECTOR,
  templateUrl: './installation.component.html'
})
export class InstallationComponent implements OnInit {

  private static readonly SELECTOR = "installation";

  public progressValue: number;
  public progressText: string;
  public displayedView: View;
  public edge: Edge = null;
  public view = View;
  public installationData: InstallationData;
  public view_display_order: View[] = [
    View.PreInstallation,
    View.ProtocolInstaller,
    View.ProtocolCustomer,
    View.ProtocolSystem,
    View.ConfigurationSystem,
    View.ConfigurationEmergencyReserve,
    View.ConfigurationLineSideMeterFuse,
    View.ProtocolPv,
    View.ProtocolAdditionalAcProducers,
    View.ProtocolDynamicFeedInLimitation,
    View.ConfigurationSummary,
    View.ConfigurationExecute,
    // TODO Read data from batttery inverter here if possible
    // View.ProtocolCompletion,
    View.ProtocolSerialNumbers,
    View.Completion
  ];

  constructor() { }

  public ngOnInit() {

    // TODO Find better initialization solution
    //#region Init installation data

    this.installationData = {
      battery: {
        serialNumbers: {
          tower1: [],
          tower2: [],
          tower3: []
        }
      },
      misc: {
        lineSideMeterFuse: {
          fixedValue: 0,
          otherValue: 0
        }
      },
      batteryInverter: {
        type: "",
        serialNumber: ""
      },
      pv: {
        ac: []
      }
    }

    //#endregion

    this.displayViewAtIndex(0);
  }

  public getViewIndex(view: View): number {
    return this.view_display_order.indexOf(view);
  }

  public displayViewAtIndex(index: number) {
    let viewCount = this.view_display_order.length;

    if (index >= 0 && index < viewCount) {

      this.displayedView = this.view_display_order[index];

      this.progressValue = viewCount === 0 ? 0 : index / (viewCount - 1);
      this.progressText = "Schritt " + (index + 1) + " von " + viewCount;

    } else {
      console.warn("The given view index is out of bounds.");
    }
  }

  /**
   * Displays the previous view.
   */
  public displayPreviousView() {
    this.displayViewAtIndex(this.getViewIndex(this.displayedView) - 1);
  }

  /**
   * Displays the next view.
   * 
   * It is possible to pass an InstallationData-Object, which then
   * will be saved in this class.
   */
  public displayNextView(installationData?: InstallationData) {
    if (installationData) {
      this.installationData = installationData;
    }

    this.displayViewAtIndex(this.getViewIndex(this.displayedView) + 1);
  }

}