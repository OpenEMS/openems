package io.openems.backend.alerting.message;

public enum AlertingTemplate {

    DEFAULT("alerting_email");

    public final String templatePath;

    AlertingTemplate(String templatePath) {
	this.templatePath = templatePath;
    }
}
