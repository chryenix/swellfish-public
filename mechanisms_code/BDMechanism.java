
import java.util.ArrayList;
import java.util.Arrays;

public class BDMechanism {

	public static boolean ABSORB_M1_BUDGET_AT_STATRT = false;

	public static double[] BD(double[] org_stream, final int w, final double global_sensitivity,
			double[] sanitized_stream, double epsilon) {
		final int length = org_stream.length;
		final double[] temporal_sensitivity = new double[length];
		Arrays.fill(temporal_sensitivity, global_sensitivity);
		return TS_BD(org_stream, w, temporal_sensitivity, sanitized_stream, epsilon);
	}

	public static double[] TS_BD(double[] org_stream, final int w, final double[] temporal_sensitivity,
			final double[] sanitized_stream, double epsilon) {
		final int length = org_stream.length;

		double lambda_1;
		double lambda_2;
		// no budget shifting in 1-d case
		final double epsilon_1_t = 0.5d * epsilon / w; // per time stamp
		final double epsilon_2 = 0.5d * epsilon; // for entire w window
		double sensitivity;

		// lambda_1 = (w*sensitivity)/(*0.5d;

		final double[] used_budget = new double[length];
		final double[] remainig_budget = new double[length];
		ArrayList<Integer> when_published = new ArrayList<Integer>();

		// We certainly publish the first time stamp.
		int t = 0;
		double budget = epsilon_2 / 2;// Initially, use half of the available budget.
		sensitivity = temporal_sensitivity[t];
		// absorbing budget from M_1 as we always publish and thus need
		// no dissimilarity check
		if (ABSORB_M1_BUDGET_AT_STATRT) {
			lambda_2 = sensitivity / (budget + epsilon_1_t);
		} else {
			lambda_2 = sensitivity / (budget);
		}
		double last_published = Mechanism.sanitize(org_stream[t], lambda_2);// one time stamp sanitized
		sanitized_stream[t] = last_published;
		used_budget[t] = budget;
		when_published.add(0);
		remainig_budget[0] = budget;// the other half
		double used_budget_in_this_window = budget;
		t++;

		while (t < length) {
			sensitivity = temporal_sensitivity[t];
			// Phase 1: Similarity
			/** sad - sanitized_avg_dissimilarity */
			double s_a_d;
			lambda_1 = sensitivity / epsilon_1_t;
			s_a_d = Mechanism.private_dissimilarity(last_published, org_stream[t], lambda_1);

			// Phase 2: Publishing decision
			if (t >= w) {// budget recovery
				used_budget_in_this_window -= used_budget[t - w];// this one is not relevant anymore
			}
			budget = (epsilon_2 - used_budget_in_this_window) / 2;// use half of the remaining budget
			remainig_budget[t] = budget;
			// only publish if the newly introduced error probably is smaller than
			// publishing the last values again.
			lambda_2 = sensitivity / budget;
			if (s_a_d > lambda_2) {
				last_published = Mechanism.sanitize(org_stream[t], lambda_2);// one time stamp sanitized
				sanitized_stream[t] = last_published;
				used_budget[t] = budget;
				used_budget_in_this_window += budget;
				when_published.add(t);
			} else {
				sanitized_stream[t] = last_published;
			}
			t++;

		}

		return sanitized_stream;
	}

}
