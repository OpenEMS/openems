import { AcPv } from './views/protocol-additional-ac-producers/protocol-additional-ac-producers.component';
import { Component, OnInit } from '@angular/core';
import { DcPv } from './views/protocol-pv/protocol-pv.component';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { FeedInSetting } from './views/protocol-dynamic-feed-in-limitation/protocol-dynamic-feed-in-limitation.component';
import { SetupProtocol } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';

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
  // pre-installation
  edge?: Edge,

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

  battery?: {
    // configuration-system
    type?: string,
    // protocol-serial-numbers
    serialNumbers?: {
      tower1?: {
        label: string,
        value: string
      }[],
      tower2?: {
        label: string,
        value: string
      }[],
      tower3?: {
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

  // configuration-line-side-meter-fuse
  lineSideMeterFuse?: {
    fixedValue: number,
    otherValue: number
  }

  batteryInverter?: {
    // protocol-serial-numbers
    serialNumber?: string,
    // protocol-dynamic-feed-in-limitation
    dynamicFeedInLimitation?: {
      maximumFeedInPower: number,
      feedInSetting: FeedInSetting,
      fixedPowerFactor: FeedInSetting
    }
  },

  // protocol-pv
  pv?: {
    dc1?: DcPv,
    dc2?: DcPv,
    ac?: AcPv[]
  }

  // configuration-summary
  setupProtocol?: SetupProtocol

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

  public installationData: InstallationData;

  public progressValue: number;
  public progressText: string;

  public edge: Edge = null;

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
  public displayedView: View;
  public view = View;

  public spinnerId: string;

  constructor(private service: Service, public websocket: Websocket) { }

  public ngOnInit() {
    this.spinnerId = "installation-websocket-spinner";
    this.service.startSpinner(this.spinnerId);

    this.installationData = {};

    this.displayViewAtIndex(0);

    // JSON.stringify is not able to parse the prototype function getConfig() of Edge
    // let installationData: InstallationData;
    // let viewIndex: number;

    // if (sessionStorage && sessionStorage.installationData) {
    //   installationData = JSON.parse(sessionStorage.installationData);
    // } else {
    //   installationData = {};
    // }

    // if (sessionStorage && sessionStorage.viewIndex) {
    //   viewIndex = parseInt(sessionStorage.viewIndex);
    // } else {
    //   viewIndex = 0;
    // }

    // this.installationData = installationData;
    // this.displayViewAtIndex(viewIndex);
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

      // sessionStorage.setItem("viewIndex", index.toString());
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

      // JSON.stringify is not able to parse the prototype function getConfig() of Edge
      // sessionStorage.setItem("installationData", JSON.stringify(installationData));
    }

    this.displayViewAtIndex(this.getViewIndex(this.displayedView) + 1);
  }
}
