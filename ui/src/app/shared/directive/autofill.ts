// @ts-strict-ignore
import { Directive, ElementRef, OnInit } from "@angular/core";
import { Capacitor } from "@capacitor/core";
import { Logger } from "../shared";

@Directive({
  selector: "[appAutofill]",
  standalone: false,
})
export class AutofillDirective implements OnInit {

  constructor(private el: ElementRef, private logger: Logger) { }

  ngOnInit(): void {
    if (CAPACITOR.GET_PLATFORM() !== "ios") { return; }
    setTimeout(() => {
      try {
        THIS.EL.NATIVE_ELEMENT.CHILDREN[0].addEventListener("change", (e) => {
          THIS.EL.NATIVE_ELEMENT.VALUE = (E.TARGET as any).value;
        });
      } catch {
        CONSOLE.ERROR("Android Autofill Directive inactive");
      }
    }, 100); // Need some time for the ion-input to create the input element
  }
}
