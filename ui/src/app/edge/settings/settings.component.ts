import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Utils } from '../../shared/shared';

@Component({
  selector: 'settings',
  templateUrl: './settings.component.html'
})
export class SettingsComponent implements OnInit {

  public edge: Edge = null

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentEdge(this.route).then(edge => {
      this.edge = edge
    });
  }

}