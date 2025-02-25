@org.osgi.annotation.versioning.Version("1.0.0")
@org.osgi.annotation.bundle.Export
package io.openems.edge.evse.api;

// Beobachtungen:
// - fems888
//   - KONA D 
//     - Unterbrechnung der Beladung funktioniert nicht. Beladung muss in dem Fall durch erneutes Einstecken oder über die App neu gestartet werden
//     - PhaseSwitch THREE to SINGLE funktioniert
//     - PhaseSwitch SINGLE to THREE funktioniert nicht mehr; lädt weiter nur auf einer Phase - dann sogar nur mit 16 A, weil eigentlich dreiphasig vorgegeben wird 