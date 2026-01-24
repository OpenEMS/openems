package io.openems.edge.predictor.profileclusteringmodel;

import java.time.LocalDate;

public record CurrentProfile(//
		LocalDate date, //
		Profile profile) {
}
