import { Component, ElementRef, EventEmitter, OnInit, Output, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { filter, take } from 'rxjs/operators';
import { SumState } from 'src/app/index/shared/sumState';
import { AddEdgeToUserRequest } from 'src/app/shared/jsonrpc/request/addEdgeToUserRequest';
import { SubscribeEdgesRequest } from 'src/app/shared/jsonrpc/request/subscribeEdgesRequest';
import { AddEdgeToUserResponse } from 'src/app/shared/jsonrpc/response/addEdgeToUserResponse';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';
import { environment } from 'src/environments';

@Component({
  selector: PreInstallationComponent.SELECTOR,
  templateUrl: './pre-installation.component.html'
})
export class PreInstallationComponent implements OnInit {
  private static readonly SELECTOR = 'pre-installation';

  public edge: Edge;
  @Output() public nextViewEvent = new EventEmitter();
  @Output() public edgeChange = new EventEmitter<Edge>();

  @ViewChild('setupKey', { static: false })
  private setupKey: ElementRef;

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;
  public isWaiting = false;
  public image: string;
  protected validKey: boolean = true;

  constructor(
    private service: Service,
    public websocket: Websocket,
    private translate: TranslateService
  ) { }

  public ngOnInit() {
    this.form = new FormGroup({});

    if (environment.theme === 'Heckert') {
      this.image = 'assets/img/Home-Typenschild-web.jpg';
    } else {
      this.image = 'assets/img/Home-Commercial-Installer-Key.png';
    }
  }

  public onNextClicked() {
    const setupPassword = this.setupKey.nativeElement.value.trim();
    this.isWaiting = true;

    this.websocket
      .sendRequest(new AddEdgeToUserRequest({ setupPassword }))
      .then((response: AddEdgeToUserResponse) => {
        const edge = response.result.edge;
        const emsBoxSerialNumber: string = response.result.serialNumber;

        // Test if edge is online
        if (!edge.online) {
          this.service.toast(
            this.translate.instant('INSTALLATION.PRE_INSTALLATION.EDGE_OFFLINE', { edgeShortName: environment.edgeShortName }), 'danger'
          );
          return;
        }

        // TODO either add lastmessage and role to response or call getEdgeRequest 
        this.edge = new Edge(
          edge.id,
          edge.comment, edge.producttype, edge.version, Role.INSTALLER, edge.online, null, SumState.OK, null
        );

        // Get metadata
        const metadata = this.service.metadata?.getValue();

        // Test if metadata is available
        if (!metadata) {
          return;
        }

        // Add edge to metadata
        metadata.edges[edge.id] = this.edge;

        // Update metadata
        this.service.metadata.next({
          user: metadata.user,
          edges: metadata.edges
        });

        this.service.metadata
          .pipe(
            filter(metadata => metadata != null),
            take(1))
          .subscribe(metadata => {
            this.edge = metadata.edges[edge.id];
          });


        // Add to session Storage.
        sessionStorage.setItem('edge', JSON.stringify(edge));
        if (emsBoxSerialNumber) {
          // Store Fems Box Serial number only if it is existing in Odoo.
          sessionStorage.setItem('emsBoxSerialNumber', emsBoxSerialNumber);
        }

        // Start installation process
        this.service.toast(
          this.translate.instant('INSTALLATION.PRE_INSTALLATION.INSTALLATION_SUCCESS', { edgeId: this.edge.id }),
          'success'
        );

        this.edgeChange.emit(this.edge);
        this.nextViewEvent.emit();
        this.websocket.sendRequest(new SubscribeEdgesRequest({ edges: [this.edge.id] }));
      })
      .catch((reason) => {
        this.service.toast(this.translate.instant('INSTALLATION.PRE_INSTALLATION.AUTHENTICATION_FAILED'), 'danger');
        this.validKey = false;
        console.log(reason);
      })
      .finally(() => {
        this.isWaiting = false;
      });
  }
}
