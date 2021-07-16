import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Edge, Service, Websocket } from 'src/app/shared/shared';

import { AddEdgeToUserRequest } from 'src/app/shared/jsonrpc/request/addEdgeToUserRequest';
import { AddEdgeToUserResponse } from 'src/app/shared/jsonrpc/response/addEdgeToUserResponse';
import { InstallationData } from '../../installation.component';
import { Role } from 'src/app/shared/type/role';

@Component({
  selector: PreInstallationComponent.SELECTOR,
  templateUrl: './pre-installation.component.html'
})
export class PreInstallationComponent implements OnInit {

  private static readonly SELECTOR = "pre-installation";

  @Input() public installationData: InstallationData;

  @Output() public nextViewEvent = new EventEmitter<InstallationData>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  public isWaiting: boolean = false;

  constructor(private service: Service, public websocket: Websocket) { }

  public ngOnInit(): void {

    this.service.currentPageTitle = "Installation";

    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = {};

  }

  public getFields(): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "setupPassword",
      type: "input",
      templateOptions: {
        type: "text",
        required: true
      }
    });

    return fields;

  }

  public onNextClicked() {

    if (this.form.invalid) {
      return;
    }

    this.isWaiting = true;

    this.websocket.sendRequest(new AddEdgeToUserRequest({ setupPassword: this.model.setupPassword })).then((response: AddEdgeToUserResponse) => {

      let edge = response.result.edge;

      // Test if edge is online
      if (!edge.online) {
        this.service.toast("Es konnte keine Verbindung zum FEMS hergestellt werden.", "danger");
        return;
      }

      // Set edge
      this.installationData.edge = new Edge(
        edge.id,
        edge.comment,
        edge.producttype,
        edge.version,
        Role.getRole("installer"),
        edge.online
      );

      // Get metadata
      let metadata = this.service.metadata?.getValue();

      // Test if metadata is available
      if (!metadata) {
        return;
      }

      // Add edge to metadata
      metadata.edges[edge.id] = this.installationData.edge;

      // Update metadata
      this.service.metadata.next({
        user: metadata.user,
        edges: metadata.edges
      });

      // Set config if it is available
      let configSubscription = this.installationData.edge.getConfig(this.websocket).subscribe((config) => {

        // Test if config is available
        if (!config) {
          return;
        }

        // Set config
        this.installationData.config = config;

        // Unsubscribe the BehaviourSubject
        configSubscription.unsubscribe();

        // Start installation process
        this.service.toast("Installation für " + this.installationData.edge.id + " gestartet.", "success");
        this.nextViewEvent.emit(this.installationData);

      });

    }).catch((reason) => {
      this.service.toast("Fehler bei der Authentifizierung.", "danger");
      console.log(reason);
    }).finally(() => {
      this.isWaiting = false;
    });
  }
}
