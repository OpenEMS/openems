import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Utils } from '../../shared/shared';

@Component({
  selector: 'index',
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit {

  public edge: Edge = null

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentPage('', this.route).then(edge => {
      this.edge = edge
    });
  }

}