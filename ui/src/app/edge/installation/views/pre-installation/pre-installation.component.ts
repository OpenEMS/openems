import { Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { AddEdgeToUserRequest } from 'src/app/shared/jsonrpc/request/addEdgeToUserRequest';
import { AddEdgeToUserResponse } from 'src/app/shared/jsonrpc/response/addEdgeToUserResponse';
import { Service, Websocket } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';
import { EdgeData } from '../../installation.component';

@Component({
  selector: PreInstallationComponent.SELECTOR,
  templateUrl: './pre-installation.component.html',
})
export class PreInstallationComponent implements OnInit {
  private static readonly SELECTOR = 'pre-installation';

  @Input() public edge: EdgeData;
  @Output() public nextViewEvent = new EventEmitter();
  @Output() public setEdgeEvent = new EventEmitter<EdgeData>();

  @ViewChild('setupKey', { static: false })
  private setupKey: ElementRef;

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;
  public isWaiting = false;

  constructor(private service: Service, public websocket: Websocket) { }

  public ngOnInit() {
    this.form = new FormGroup({});
  }

  public onNextClicked() {
    const setupPassword = this.setupKey.nativeElement.value;
    this.isWaiting = true;

    this.websocket
      .sendRequest(new AddEdgeToUserRequest({ setupPassword }))
      .then((response: AddEdgeToUserResponse) => {
        const edge = response.result.edge;

        // Test if edge is online
        if (!edge.online) {
          this.service.toast(
            'Es konnte keine Verbindung zum FEMS hergestellt werden.',
            'danger'
          );
          return;
        }

        // Set edge
        this.edge = {
          id: edge.id,
          comment: edge.comment,
          producttype: edge.producttype,
          version: edge.version,
          role: Role.getRole('installer'),
          isOnline: edge.online,
        };

        // Get metadata
        const metadata = this.service.metadata?.getValue();

        // Test if metadata is available
        if (!metadata) {
          return;
        }

        // Update metadata
        this.service.metadata.next({
          user: metadata.user,
          edges: metadata.edges,
        });

        // Start installation process
        this.service.toast(
          'Installation für ' + this.edge.id + ' gestartet.',
          'success'
        );
        this.setEdgeEvent.emit(this.edge);
        this.nextViewEvent.emit();
      })
      .catch((reason) => {
        this.service.toast('Fehler bei der Authentifizierung.', 'danger');
        console.log(reason);
      })
      .finally(() => {
        this.isWaiting = false;
      });
  }
}
