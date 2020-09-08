
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Unicorn_Mechanism {

	private static double[] unicorn(double[] stream, final PolicyCollection pc, final double[] san_stream,
			double epsilon) {

		final int length = stream.length;
		double[] eps2_spent = new double[length];
		double[] eps1_spent = new double[length];
		double[] eps_spend_tt_uniform = new double[length];
		double last_published = 0;
		final double half = 0.5;

		HashMap<StreamPolicy, BoundedPriorityQueue> top_tinar_per_policy = new HashMap<StreamPolicy, BoundedPriorityQueue>();
		for (StreamPolicy p : pc.all_policies) {
			int tinar = p.tinar_time_stamps;
			top_tinar_per_policy.put(p, new BoundedPriorityQueue(tinar));
		}
		double eps_2_per_interval = half * epsilon;
		if (Mechanism.USE_NON_PRIVATE_SAMPLING) {
			eps_2_per_interval = epsilon;
		}
		double lambda_2;
		double san_val_t;

		// publish at t=0
		int t = 0;
		double val = stream[t];
		ArrayList<StreamPolicy> relevant_policies = pc.relevant_policies_per_t.get(t);
		if (relevant_policies.size() == 0) {
			san_val_t = val; // Yes we publish the true counts
			eps2_spent[t] = 0.0;
			eps_spend_tt_uniform[t] = 0.0d;
		} else {
			// double eps_1_t = half * ( epsilon / pc.max_tinar_t[t]);//XXX

			double eps_2_to_spent = eps_2_per_interval / pc.max_tinar_t[t];// 2*eps_1_t;//budget_share_eps_2_rm *
																			// eps_2_per_policy + eps_1_t;//XXX we
																			// simply use the budget for m_1, because we
																			// do not need it at t=0
			lambda_2 = pc.temporal_sensitivity[t] / eps_2_to_spent;
			san_val_t = Mechanism.sanitize(val, lambda_2);
			// adapt spent budgets
			eps2_spent[t] = eps_2_to_spent;
			eps_spend_tt_uniform[t] = epsilon / pc.max_tinar_t[t];
			for (StreamPolicy p : relevant_policies) {
				BoundedPriorityQueue top_eps_2_in_j = top_tinar_per_policy.get(p);
				top_eps_2_in_j.offer(eps_2_to_spent);
			}
		}
		san_stream[t] = san_val_t;
		last_published = san_val_t;

		for (t = 1; t < length; t++) {
			val = stream[t];
			relevant_policies = pc.relevant_policies_per_t.get(t);
			if (relevant_policies.isEmpty()) { // publish true counts, no relevant policy
				san_val_t = val;
				san_stream[t] = san_val_t;
				last_published = san_val_t;
				eps2_spent[t] = 0.0;
				eps1_spent[t] = 0.0;
				eps_spend_tt_uniform[t] = 0.0d;
			} else {
				/** sad - sanitized_avg_dissimilarity */
				// we spend min(0.5*eps/|d(J)|) over all now relevant J intervals
				double eps_1_t = half * (epsilon / pc.max_tinar_t[t]);// XXX
				double lambda_1 = pc.temporal_sensitivity[t] / eps_1_t; // 1D stream
				double s_a_d = Mechanism.private_dissimilarity(last_published, val, lambda_1);// Math.abs(last_published
																								// - val);
				eps1_spent[t] = eps_1_t;

				final double eps_2_to_spent = eps_2_to_spent(t, eps_2_per_interval, relevant_policies,
						top_tinar_per_policy);

				lambda_2 = pc.temporal_sensitivity[t] / eps_2_to_spent;
				if (s_a_d > lambda_2) {

					// publish
					san_val_t = Mechanism.sanitize(val, lambda_2);
					san_stream[t] = san_val_t;
					last_published = san_val_t;
					// adapt spent budgets
					eps2_spent[t] = eps_2_to_spent;
					eps_spend_tt_uniform[t] = epsilon / pc.max_tinar_t[t];
					for (StreamPolicy p : relevant_policies) {
						BoundedPriorityQueue top_eps_2_in_j = top_tinar_per_policy.get(p);
						top_eps_2_in_j.offer(eps_2_to_spent);
					}
				} else {
					// approx
					san_stream[t] = last_published;
					eps2_spent[t] = 0.0;
				}
			}
		}

		return san_stream;

	}

	public static double[] UNICORN_PS(double[] stream, final PolicyCollection pc, final double[] san_stream,
			double epsilon) {

		final int length = stream.length;
		double[] eps2_spent = new double[length];
		double[] eps_spend_tt_uniform = new double[length];

		HashMap<StreamPolicy, BoundedPriorityQueue> top_tinar_per_policy = new HashMap<StreamPolicy, BoundedPriorityQueue>();
		for (StreamPolicy p : pc.all_policies) {
			int tinar = p.tinar_time_stamps;
			top_tinar_per_policy.put(p, new BoundedPriorityQueue(tinar));
		}
		double eps_2_per_interval = epsilon;
		double lambda_2;
		double san_val_t;
		double val;
		ArrayList<StreamPolicy> relevant_policies;

		for (int t = 0; t < length; t++) {
			val = stream[t];
			relevant_policies = pc.relevant_policies_per_t.get(t);
			if (relevant_policies.isEmpty()) { // publish true counts, no relevant policy
				san_val_t = val;
				san_stream[t] = san_val_t;
				eps2_spent[t] = 0.0;
				eps_spend_tt_uniform[t] = 0.0d;
			} else {

				final double eps_2_to_spent = eps_2_to_spent(t, eps_2_per_interval, relevant_policies,
						top_tinar_per_policy);
				lambda_2 = pc.temporal_sensitivity[t] / eps_2_to_spent;
				// publish
				san_val_t = Mechanism.sanitize(val, lambda_2);
				san_stream[t] = san_val_t;
				// adapt spent budgets
				eps2_spent[t] = eps_2_to_spent;
				eps_spend_tt_uniform[t] = epsilon / pc.max_tinar_t[t];
				for (StreamPolicy p : relevant_policies) {
					BoundedPriorityQueue top_eps_2_in_j = top_tinar_per_policy.get(p);
					top_eps_2_in_j.offer(eps_2_to_spent);
					if (top_eps_2_in_j.sum() > eps_2_per_interval + 0.001) {
						System.err.println("Overpaid");
					}
				}
			}
		}

		return san_stream;
	}

	public static double[] UNICORN_NONPRIVATE_SAMPLING(double[] stream, final PolicyCollection pc,
			final double[] san_stream, double epsilon) {
		boolean old_val__private_sample = Mechanism.USE_NON_PRIVATE_SAMPLING;
		Mechanism.USE_NON_PRIVATE_SAMPLING = true;
		unicorn(stream, pc, san_stream, epsilon);
		Mechanism.USE_NON_PRIVATE_SAMPLING = old_val__private_sample;
		return san_stream;

	}

	public static double[] UNICORN(double[] stream, final PolicyCollection pc, final double[] san_stream,
			double epsilon) {
		unicorn(stream, pc, san_stream, epsilon);
		return san_stream;

	}

	public static double[] UNICORN_IS(double[] stream, PolicyCollection pc, final double[] sensitivity,
			final double[] san_stream, double epsilon) {
		final ArrayList<StreamPolicy> all_policies = pc.all_policies;
		// final double[] sensitivity = pc.temporal_sensitivity;
		final int length = stream.length;
		final double[] eps_spent = new double[length];// for debug
		/** Contains all polices for those I already spent the full budget. */
		final HashSet<Integer> budget_spent = new HashSet<Integer>(all_policies.size());

		// we always publish at t=0
		int t = 0;
		double org_val = stream[t];
		double lambda = sensitivity[t] / epsilon;// publish with full lambda
		double san_val_t = Mechanism.sanitize(org_val, lambda);
		san_stream[t] = san_val_t;
		double last_published = san_val_t;
		ArrayList<StreamPolicy> relevant_policies = pc.relevant_policies_per_t.get(t);
		eps_spent[t] = epsilon;
		for (StreamPolicy p : relevant_policies) {
			budget_spent.add(p.id);
		}

		// For all remaining time stamps
		for (t = 1; t < length; t++) {
			relevant_policies = pc.relevant_policies_per_t.get(t);

			if (relevant_policies.isEmpty()) {// no relevant policy...
				last_published = stream[t]; // ...no noise needed
			} else {
				if (noneContained(budget_spent, relevant_policies)) {// we haven't published in the J interval of any
																		// now relevant policy
					org_val = stream[t];
					lambda = sensitivity[t] / epsilon;// publish with full lambda
					san_val_t = Mechanism.sanitize(org_val, lambda);
					last_published = san_val_t;
					eps_spent[t] = epsilon;
					for (StreamPolicy p : relevant_policies) {
						budget_spent.add(p.id);
					}
				} // else path not required. We simply publish last value again.
			}
			san_stream[t] = last_published;// this is the only location where we actually publish.
		}
		// Mechanism.check(pc, eps_spent, Experiment.epsilon); //works
		return san_stream;
	}

	private static final boolean noneContained(final HashSet<Integer> budget_spent,
			final ArrayList<StreamPolicy> relevant_policies) {
		for (StreamPolicy p : relevant_policies) {
			if (budget_spent.contains(p.id)) {
				return false;
			}
		}
		return true;
	}

	static double eps_2_to_spent(final int t, final double eps_2, final ArrayList<StreamPolicy> relevant_policies,
			final HashMap<StreamPolicy, BoundedPriorityQueue> top_tinar_per_policy) {
		double eps_2_t = Double.MAX_VALUE;
		for (StreamPolicy p : relevant_policies) {
			final BoundedPriorityQueue top_eps_2_in_j = top_tinar_per_policy.get(p);
			// (1) eps_2_rm
			final double open_relevant_timestamps = p.J_stop - t + 1;
			final double my_eps_2_rm = top_eps_2_in_j.eps_rm(eps_2, p);

			double my_eps_2_t = my_eps_2_rm / open_relevant_timestamps;
			// (2) uniform eps
			double eps_uniform = eps_2 / p.tinar_time_stamps();
			// => maximum over both
			if (eps_uniform > my_eps_2_t) {
				my_eps_2_t = eps_uniform;
			} else if (eps_uniform + 0.0001 < my_eps_2_t) {
				// System.out.println("t="+t+" : "+p);
			}

			// minimum over all policies
			eps_2_t = Math.min(eps_2_t, my_eps_2_t);
		}
		return eps_2_t;
	}

}