package io.openems.edge.predictor.profileclusteringmodel.prediction;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.metrics.Metrics;
import io.openems.edge.predictor.profileclusteringmodel.Profile;

public class ProfileSwitcher {

	private static final double ALPHA = 0.20;

	private final List<Profile> allProfiles;
	private final Profile currentProfile;
	private final Series<Integer> todaysValues;

	public ProfileSwitcher(List<Profile> allProfiles, Profile currentProfile, Series<Integer> todaysValues) {
		this.allProfiles = allProfiles;
		this.currentProfile = currentProfile;
		this.todaysValues = todaysValues;
	}

	/**
	 * Attempts to find a better matching cluster profile for today based on the
	 * observed values so far.
	 *
	 * <p>
	 * The method works as follows:
	 * <ul>
	 * <li>Finds the profile with the smallest squared error compared to today's
	 * values.</li>
	 * <li>If the best profile is the same as the current one, no change is
	 * made.</li>
	 * <li>Computes the distance between the current and best profile to determine a
	 * threshold.</li>
	 * <li>Switches to the better profile only if the improvement exceeds this
	 * threshold.</li>
	 * </ul>
	 *
	 * @return an {@code Optional<Profile>} containing a better matching profile if
	 *         found; otherwise {@link Optional#empty()}
	 */
	public Optional<Profile> findBetterProfile() {
		var candidateProfile = this.allProfiles.stream()//
				.min(Comparator.comparingDouble(//
						profile -> Metrics.squaredError(profile.values(), this.todaysValues)))//
				.orElse(this.currentProfile);

		if (this.currentProfile.equals(candidateProfile)) {
			return Optional.empty();
		}

		double candidateError = Metrics.squaredError(candidateProfile.values(), this.todaysValues);
		double currentError = Metrics.squaredError(this.currentProfile.values(), this.todaysValues);
		double errorImprovement = currentError - candidateError;

		double profileDistance = Metrics.squaredError(this.currentProfile.values(), candidateProfile.values());
		double switchThreshold = ALPHA * profileDistance;

		if (errorImprovement > switchThreshold) {
			return Optional.of(candidateProfile);
		}

		return Optional.empty();
	}
}
