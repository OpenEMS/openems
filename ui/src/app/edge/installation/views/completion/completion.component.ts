import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { format } from 'date-fns/esm';
import { saveAs } from 'file-saver-es';
import { GetSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/getSetupProtocolRequest';
import { Base64PayloadResponse } from 'src/app/shared/jsonrpc/response/base64PayloadResponse';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { Router } from '@angular/router';
import { SystemId } from '../../shared/system';

@Component({
  selector: CompletionComponent.SELECTOR,
  templateUrl: './completion.component.html',
})
export class CompletionComponent implements OnInit {

  private static readonly SELECTOR = 'completion';

  @Input() public ibn: AbstractIbn;
  @Input() public edge: Edge;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent: EventEmitter<any> = new EventEmitter();
  public system: SystemId | null = null;
  public isWaiting = false;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private translate: TranslateService,
    private router: Router,
  ) { }

  public ngOnInit(): void {
    this.system = this.ibn.id;
    this.isWaiting = true;

    this.ibn.getProtocol(this.edge, this.websocket).then((protocolId) => {
      this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.SENT_SUCCESSFULLY'), 'success');
      this.ibn.setupProtocolId = protocolId;
    }).catch((reason) => {
      this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.ERROR_SENDING'), 'danger');
      console.warn(reason);
    }).finally(() => {
      this.isWaiting = false;
    });
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    this.nextViewEvent.emit();
  }

  public downloadProtocol() {
    const request = new GetSetupProtocolRequest({ setupProtocolId: this.ibn.setupProtocolId });

    this.websocket.sendRequest(request).then((response: Base64PayloadResponse) => {

      const binary = atob(response.result.payload.replace(/\s/g, ''));
      const length = binary.length;

      const buffer = new ArrayBuffer(length);
      const view = new Uint8Array(buffer);

      for (let i = 0; i < length; i++) {
        view[i] = binary.charCodeAt(i);
      }

      const data: Blob = new Blob([view], {
        type: 'application/pdf',
      });

      const fileName = `IBN-${this.edge.id}-${format(new Date(), 'dd.MM.yyyy')}.pdf`;

      saveAs(data, fileName);
    }).catch((error) => {
      this.service.toast(this.translate.instant('INSTALLATION.COMPLETION.DOWNLOAD_ERROR'), 'danger');
      console.error(error);
    });
  }

  protected navigate() {
    this.router.navigate(['device/' + (this.edge.id) + '/settings/app']);
  }
}
