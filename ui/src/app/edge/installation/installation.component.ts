import { AcPv } from './views/protocol-additional-ac-producers/protocol-additional-ac-producers.component';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { DcPv } from './views/protocol-pv/protocol-pv.component';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { FeedInSetting } from './views/protocol-dynamic-feed-in-limitation/protocol-dynamic-feed-in-limitation.component';
import { SetupProtocol } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { Router } from '@angular/router';
import { EmsApp } from './views/heckert-app-installer/heckert-app-installer.component';
import { environment } from 'src/environments';

enum View {
  Completion,
  ConfigurationEmergencyReserve,
  ConfigurationExecute,
  ConfigurationLineSideMeterFuse,
  ConfigurationSummary,
  ConfigurationSystem,
  PreInstallation,
  ProtocolAdditionalAcProducers,
  ProtocolCustomer,
  ProtocolDynamicFeedInLimitation,
  ProtocolInstaller,
  ProtocolPv,
  ProtocolSerialNumbers,
  ProtocolSystem,
  HeckertAppInstaller
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
      tower0?: {
        label: string,
        value: string
      }[],
      tower1?: {
        label: string,
        value: string
      }[],
      tower2?: {
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

  // protocol-dynamic-feed-in-limitation
  dynamicFeedInLimitation?: {
    maximumFeedInPower: number,
    feedInSetting: FeedInSetting,
    fixedPowerFactor: FeedInSetting
  },

  // protocol-pv
  pv?: {
    dc1?: DcPv,
    dc2?: DcPv,
    ac?: AcPv[]
  }

  // heckert-app-installer
  selectedFreeApp?: EmsApp,

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
export class InstallationComponent implements OnInit, OnDestroy {
  private static readonly SELECTOR = "installation";

  public installationData: InstallationData;

  public progressValue: number;
  public progressText: string;

  public edge: Edge = null;

  public viewArrangement: View[] = [
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
    View.HeckertAppInstaller,
    View.ConfigurationSummary,
    View.ConfigurationExecute,
    View.ProtocolSerialNumbers,
    View.Completion
  ];
  public displayedView: View;
  public view = View;

  public spinnerId: string;

  constructor(private service: Service, private router: Router, public websocket: Websocket) { }

  public ngOnInit() {
    this.service.currentPageTitle = "Installation";

    this.spinnerId = "installation-websocket-spinner";
    this.service.startSpinner(this.spinnerId);

    // Only show app-installer view for Heckert
    if (environment.theme !== "Heckert") {
      let viewIndex = this.viewArrangement.indexOf(View.HeckertAppInstaller);
      this.viewArrangement.splice(viewIndex, 1);
    }

    let installationData: InstallationData;
    let viewIndex: number;

    // Determine installation data
    if (sessionStorage && sessionStorage.installationData) {
      installationData = JSON.parse(sessionStorage.installationData);

      // Recreate edge object to provide the correct
      // functionality of it (the prototype can't be saved as JSON,
      // so it has to get instantiated here again)
      installationData.edge = new Edge(
        installationData.edge.id,
        installationData.edge.comment,
        installationData.edge.producttype,
        installationData.edge.version,
        installationData.edge.role,
        installationData.edge.isOnline
      );
    } else {
      installationData = {};
    }

    // Determine view index
    if (sessionStorage && sessionStorage.viewIndex) {
      viewIndex = parseInt(sessionStorage.viewIndex);
    } else {
      viewIndex = 0;
    }

    this.installationData = installationData;
    this.displayViewAtIndex(viewIndex);
  }

  ngOnDestroy() { }

  public getViewIndex(view: View): number {
    return this.viewArrangement.indexOf(view);
  }

  public displayViewAtIndex(index: number) {
    let viewCount = this.viewArrangement.length;

    if (index >= 0 && index < viewCount) {
      this.displayedView = this.viewArrangement[index];

      this.progressValue = viewCount === 0 ? 0 : index / (viewCount - 1);
      this.progressText = "Schritt " + (index + 1) + " von " + viewCount;

      if (sessionStorage) {
        sessionStorage.setItem("viewIndex", index.toString());
      }
      // When clicking next on the last view
    } else if (index === viewCount) {
      // Explictly destroy the installation component 
      this.ngOnDestroy();

      // Navigate to online monitoring of the edge
      this.router.navigate(["device", this.installationData.edge.id]);

      // Clear session storage
      sessionStorage.clear();
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

      if (sessionStorage) {
        sessionStorage.setItem("installationData", JSON.stringify(installationData));
      }
    }

    this.displayViewAtIndex(this.getViewIndex(this.displayedView) + 1);
  }
}