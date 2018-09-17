import { Injectable } from '@angular/core';
import {
    HttpEvent, HttpInterceptor, HttpHandler, HttpRequest, HttpErrorResponse
} from '@angular/common/http';

import { Observable } from 'rxjs';
import { Service } from './shared/shared';
import { catchError } from 'rxjs/operators';

/** Pass untouched request through to the next request handler. */
@Injectable()
export class Interceptor implements HttpInterceptor {
    constructor(
        private service: Service
    ) { };


    intercept(req: HttpRequest<any>, next: HttpHandler):
        Observable<HttpEvent<any>> {

        const cloneReq = req.clone({ withCredentials: true });

        return next.handle(cloneReq);

    }
}