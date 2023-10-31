import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { format } from 'date-fns/esm';
import { saveAs } from 'file-saver-es';
import { GetSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/getSetupProtocolRequest';
import { Base64PayloadResponse } from 'src/app/shared/jsonrpc/response/base64PayloadResponse';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { System } from '../../shared/system';

@Component({
  selector: CompletionComponent.SELECTOR,
  templateUrl: './completion.component.html'
})
export class CompletionComponent implements OnInit {

  private static readonly SELECTOR = "completion";

  @Input() public ibn: AbstractIbn;
  @Input() public edge: Edge;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent: EventEmitter<any> = new EventEmitter();
  protected system: string | null = null;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private translate: TranslateService
  ) { }

  public ngOnInit(): void {
    this.system = System.getSystemTypeLabel(this.ibn.type);
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    this.nextViewEvent.emit();
  }

  public downloadProtocol() {
    let request = new GetSetupProtocolRequest({ setupProtocolId: this.ibn.setupProtocolId });

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

      let fileName = "IBN-" + this.edge.id + "-" + format(new Date(), "dd.MM.yyyy") + ".pdf";

      saveAs(data, fileName);
    }).catch((reason) => {
      this.service.toast(this.translate.instant('INSTALLATION.COMPLETION.DOWNLOAD_ERROR'), "danger");
      console.log(reason);
    });
  }
}
