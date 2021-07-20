import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { ChannelAddress, Service, Websocket } from 'src/app/shared/shared';
import { InstallationData } from '../../installation.component';

@Component({
  selector: ProtocolCompletionComponent.SELECTOR,
  templateUrl: './protocol-completion.component.html'
})
export class ProtocolCompletionComponent implements OnInit {

  private static readonly SELECTOR = "protocol-completion";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<any>();

  public outputText: string;

  constructor(private service: Service, private websocket: Websocket) { }

  public ngOnInit(): void {
    // this.outputText = "";
    // this.readChannels();
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    this.submitSetupProtocol();
    this.nextViewEvent.emit();
  }

  public readChannels() {

    this.installationData.edge.subscribeChannels(this.websocket, ProtocolCompletionComponent.SELECTOR, [
      new ChannelAddress("battery0", "Capacity"),
      new ChannelAddress("batteryInverter0", "GoodweType")
    ]);

    let currentDataSubscription = this.installationData.edge.currentData.subscribe((currentData) => {

      if (currentData) {

        let newCapacity: number = parseInt(currentData.channel["battery0/Capacity"]);

        if (this.installationData.battery.capacity !== newCapacity) {
          this.installationData.battery.capacity = newCapacity;
          this.outputText += "Batteriekapazität: " + this.installationData.battery.capacity + " [Wh]\n";
        }

      }

    });

    if (currentDataSubscription) currentDataSubscription.unsubscribe();

  }

  public submitSetupProtocol() {

    let customer = this.installationData.customer;
    let protocol: SetupProtocol = {
      fems: {
        id: this.installationData.edge.id
      },
      customer: {
        firstname: customer.firstName,
        lastname: customer.lastName,
        email: customer.email,
        phone: customer.phone,
        address: {
          street: customer.street,
          city: customer.city,
          zip: customer.zip,
          country: customer.country
        },
        company: {
          name: customer.companyName
        }
      }
    };

    // If location data is different to customer data, the location
    // data gets sent too
    if (!this.installationData.location.isEqualToCustomerData) {
      let location = this.installationData.location;

      protocol.location = {
        firstname: location.firstName,
        lastname: location.lastName,
        email: location.email,
        phone: location.phone,
        address: {
          street: location.street,
          city: location.city,
          zip: location.zip,
          country: location.country
        },
        company: {
          name: location.companyName
        }
      }
    }

    let request = new SubmitSetupProtocolRequest({ protocol: protocol });
    let protocolId;

    this.websocket.sendRequest(request).then((response: JsonrpcResponseSuccess) => {
      protocolId = response.result["protocolId"]
      this.service.toast("Das Protokoll wurde erfolgreich versendet.", "success");
    }).catch((reason) => {
      console.log(reason);
      this.service.toast("Fehler beim Versenden des Protokolls.", "danger");
    });
  }
}
