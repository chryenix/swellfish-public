
import java.util.Arrays;

public class BAMechanism {

	public static double[] BA(double[] org_stream, final int w, final double global_sensitivity,
			final double[] sanitized_stream, double epsilon) {
		final int length = org_stream.length;
		final double[] temporal_sensitivity = new double[length];
		Arrays.fill(temporal_sensitivity, global_sensitivity);
		return TS_BA(org_stream, w, temporal_sensitivity, sanitized_stream, epsilon);
	}

	public static double[] TS_BA(double[] org_stream, final int w, final double[] temporal_sensitivity,
			final double[] sanitized_stream, double epsilon) {
		final int length = org_stream.length;

		double lambda_1;
		double lambda_2;
		// no budget shifting in 1-d case
		final double epsilon_1_t = 0.5d * epsilon / w; // per time stamp
		final double epsilon_2 = 0.5d * epsilon; // for entire w window
		double sensitivity;

		final double[] used_budget = new double[length];

		// We certainly publish the first time stamp.
		int t = 0;
		double budget = epsilon_2 / w;// Like in uniform.
		sensitivity = temporal_sensitivity[t];
		// XXX evil trick absorbing budget from M_1 as we always publish and thus need
		// no dissimilarity check
		if (BDMechanism.ABSORB_M1_BUDGET_AT_STATRT) {
			lambda_2 = sensitivity / (budget + epsilon_1_t);
		} else {
			lambda_2 = sensitivity / (budget);
		}
		double last_published = Mechanism.sanitize(org_stream[t], lambda_2);// one time stamp sanitized
		sanitized_stream[t] = last_published;
		used_budget[t] = budget;
		double used_budget_in_this_window = budget;
		t++;
		int timePointsSkipped = 0;

		while (t < length) {
			sensitivity = temporal_sensitivity[t];
			// Phase 1: Similarity
			/** sad - sanitized_avg_dissimilarity */
			double s_a_d;
			lambda_1 = sensitivity / (epsilon_1_t);
			s_a_d = Mechanism.private_dissimilarity(last_published, org_stream[t], lambda_1);

			// Phase 2: Publishing decision
			if (t >= w) {// budget recovery
				used_budget_in_this_window -= used_budget[t - w];// this one is not relevant anymore
			}
			budget = (epsilon_2 / (double) w) * (1.0d + (double) timePointsSkipped);
			// only publish if the newly introduced error probably is smaller than
			// publishing the last values again.
			lambda_2 = sensitivity / (budget);
			if (s_a_d > lambda_2) {
				last_published = Mechanism.sanitize(org_stream[t], lambda_2);// one time stamp sanitized
				sanitized_stream[t] = last_published;
				used_budget[t] = budget;
				used_budget_in_this_window += budget;

				if (timePointsSkipped > 0) {
					final int skip_until = t + timePointsSkipped;

					while (t < skip_until) {
						t++;
						if (t == length) {
							break;
						}
						sanitized_stream[t] = last_published;
						if (t >= w) {// budget recovery
							used_budget_in_this_window -= used_budget[t - w];// this one is not relevant anymore
						}
					}
					timePointsSkipped = 0;
				}
			} else {
				sanitized_stream[t] = last_published;
				if (timePointsSkipped < w - 1) {
					timePointsSkipped++;
				}
			}

			t++;
		}

		return sanitized_stream;
	}

}
