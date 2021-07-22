import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router } from '@angular/router';
import { GetSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/getSetupProtocolRequest';
import { Base64PayloadResponse } from 'src/app/shared/jsonrpc/response/base64PayloadResponse';
import { Websocket } from 'src/app/shared/shared';
import { InstallationData } from '../../installation.component';
import { format } from 'date-fns/esm';
import { saveAs } from 'file-saver-es';

@Component({
  selector: CompletionComponent.SELECTOR,
  templateUrl: './completion.component.html'
})
export class CompletionComponent {

  private static readonly SELECTOR = "completion";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();

  constructor(private websocket: Websocket, private router: Router) { }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    this.router.navigate(["device", this.installationData.edge.id]);
  }

  public downloadProtocol() {

    let request = new GetSetupProtocolRequest({ setupProtocolId: this.installationData.setupProtocolId })

    this.websocket.sendRequest(request).then((response: Base64PayloadResponse) => {

      var binary = atob(response.result.payload.replace(/\s/g, ''));
      var length = binary.length;
      var buffer = new ArrayBuffer(length);
      var view = new Uint8Array(buffer);
      for (var i = 0; i < length; i++) {
        view[i] = binary.charCodeAt(i);
      }

      const data: Blob = new Blob([view], {
        type: "application/pdf"
      });

      let fileName = "IBN-" + this.installationData.edge.id + "-" + format(new Date(), "dd.MM.yyyy") + ".pdf";

      saveAs(data, fileName);

    }).catch((reason) => {

    });

  }

}
