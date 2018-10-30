import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { TranslateService, LangChangeEvent } from '@ngx-translate/core';

import { environment } from '../../environments';

import { Websocket, Utils, Service, Alerts } from '../shared/shared';
import { Edge } from '../shared/edge/edge';

import { HttpClient, HttpHeaders, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { SpinnerDialog } from '@ionic-native/spinner-dialog/ngx';


@Component({
  selector: 'index',
  templateUrl: './index.component.html'
})
export class IndexComponent {
  public env = environment;
  public form: FormGroup;
  public wpForm: FormGroup;
  private filter: string = '';

  private stopOnDestroy: Subject<void> = new Subject<void>();
  private allEdgeIds: string[] = [];
  private edges: Edge[] = [];
  private filteredTruncated: boolean = false;
  private static maxFilteredEdges = 20;


  constructor(
    public websocket: Websocket,
    public utils: Utils,
    private formBuilder: FormBuilder,
    private wpformBuilder: FormBuilder,
    private router: Router,
    private http: HttpClient,

    private alerts: Alerts,
    public translate: TranslateService,
    private service: Service) {
    this.form = this.formBuilder.group({
      "password": this.formBuilder.control('user')
    });

    this.wpForm = this.wpformBuilder.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
      saveAccount: [false]
    }),


      //Forwarding to device index if there is only 1 edge
      websocket.edges.pipe(takeUntil(this.stopOnDestroy)).subscribe(edges => {
        let edgeIds = Object.keys(edges);
        /*
        if (edgeIds.length == 1) {
          let edge = edges[edgeIds[0]];
          this.router.navigate(['/device', edge.name]);
        }
        */
        this.updateFilteredEdges();
      })
  }

  ngOnInit() {
    if (localStorage.getItem("username") != null) {
      this.wpForm.setValue({ username: localStorage.getItem("username"), password: localStorage.getItem("password"), saveAccount: false });
    }
  }

  updateFilteredEdges() {
    let edges = this.websocket.edges.getValue();
    this.allEdgeIds = Object.keys(edges);
    let filter = this.filter.toLowerCase();
    let filteredEdges = Object.keys(edges)
      .filter(edgeId => {
        let edge = edges[edgeId];
        if (/* name */ edge.name.toLowerCase().includes(filter)
          || /* comment */ edge.comment.toLowerCase().includes(filter)) {
          return true;
        }
        return false;
      })
      .map(edgeId => edges[edgeId]);

    if (filteredEdges.length > IndexComponent.maxFilteredEdges) {
      this.filteredTruncated = true;
      this.edges = filteredEdges.slice(0, IndexComponent.maxFilteredEdges);
    } else {
      this.filteredTruncated = false;
      this.edges = filteredEdges;
    }
  }

  doLogin() {
    let password: string = this.form.value['password'];
    this.websocket.logIn(password);
  }

  async doWPLogin() {
    if (this.wpForm.invalid) {
      this.alerts.showError(this.translate.instant('Index.FormInvalid'));
      return;
    }
    this.service.spinnerDialog.show("Login", this.translate.instant('Index.Connecting'));



    let password: string = this.wpForm.value['password'];
    let username: string = this.wpForm.value['username'];
    let valid = await this.validateWPLogin(username, password);

    if (valid['status'] === "ok") {
      let headers = new HttpHeaders();
      headers = headers.append("Authorization", "Basic " + btoa(username + ":" + password));
      headers = headers.append("Content-Type", "application/x-www-form-urlencoded");

      let body = new FormData();
      body.append('log', username);
      body.append('pwd', password);



      this.sendWPLogin(body).subscribe((response: Response) => { console.info("Response"); this.service.spinnerDialog.hide(); },
        (error: HttpErrorResponse) => { console.info(error); if (error.status === 200) { this.websocket.wpconnect(); } },
        () => { this.websocket.wpconnect(); });
      //this.websocket.wpconnect();
      if (this.wpForm.value['saveAccount']) {
        localStorage.setItem("username", username);
        localStorage.setItem("password", password);
        console.info("Account saved.");
      }

    } else {
      this.service.spinnerDialog.hide();
      if (valid['error'] === "Invalid username and/or password.") {
        this.alerts.showError(this.translate.instant('Index.LoginWrong'));

      }
      else {
        this.alerts.defaultAlert();
      }

      return;
    }






  }


  sendWPLogin(body: FormData) {
    return this.http.post("https://www.energydepot.de/login/", body);
  }

  validateWPLogin(username: string, password: string): Promise<any> {

    return this.http.get("https://www.energydepot.de/api/auth/generate_auth_cookie/?username=" + username + "&password=" + password).toPromise();
    /*
  .then((data) => {
      if (data['status'] === "ok") {
        return "ok";
      }
      if (data['status'] === "error") {
        return data['error'];
      }
    });*/


  }
  retrievePwd() {
    this.alerts.retrievePwd();
  }
  onDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}
