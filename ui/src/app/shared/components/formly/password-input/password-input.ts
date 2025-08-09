import { Component, Input } from "@angular/core";
import { FormControl } from "@angular/forms";

@Component({
  selector: "oe-password-input",
  templateUrl: "./password-input.html",
  standalone: false,
})
export class PasswordInputComponent {
  @Input() control!: FormControl;
  @Input() placeholder: string = "";
  @Input() label: string = "";
  @Input() required: boolean = false;
  @Input() ariaLabelShow: string = "Show password";
  @Input() ariaLabelHide: string = "Hide password";

  protected showPassword: boolean = false;

  protected togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }
}