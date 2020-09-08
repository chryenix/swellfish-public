
import java.util.ArrayList;

public class RescueDP1D {
	public static boolean USE_KALMAN_FILTER = true;
	/**
	 * Specifics of lambda_r calculation upon determining next sample interval. If
	 * true the version from the paper is used. false usually leads to better
	 * utility.
	 */
	public static boolean USE_LAMBDA_R_PAPER = true;
	public static boolean DEBUG_MODE = false;

	double[] releases;
	final double K_p = 0.9d;
	final double K_i = 0.1d;
	final double K_d = 0.0d;
	/** Number of sampling points to include in PID error */
	int pi = 3;
	/**
	 * Some scale factor to determine the weight of the fraction of sampling error
	 * and perturbation error. Used to determine nex - As in Paper experiments, set
	 * to 10, for some inconclusive reason.
	 */
	double theta = 10.0d;
	/** Another scale factor in (0,1) used in adaptive budget allocation. */
	double phi = 0.2d;
	/** Maximum fraction of remaining epsilon to use */
	double p_max = 0.6d;
	/** Maximum absolute size of a budget portion per sampling point */
	double e_max = 0.2;// XXX - say so, according to paper.

	/** current length of the sample interval for all locations. */
	final int INITIAL_SAMPLE_INTERVAL;
	/** I_l in the paper */
	double sampleIntervalLength = 1.0d;
	int nextSamplePoint = 0;

	ArrayList<Integer> sampling_points;
	double sensitivity;

	// Kalman Filter stuff, only need one since 1D
	/** POST_ESTIMATE */
	double p;
	/** ERROR_VARIANCE Modeling process as N(0,q) */
	double q;
	/** PROCESS_MODEL_VARIANCE */
	double r;
	/** k */
	double k;

	public static double[] RESCUEDP(double[] org_stream, final int w, final double global_sensitivity,
			double[] sanitized_stream, double epsilon) {
		RescueDP1D rdp = new RescueDP1D();
		rdp.run1D(org_stream, global_sensitivity, w, sanitized_stream, epsilon);
		return sanitized_stream;
	}

	public RescueDP1D() {
		this(1);// set initial sample interval to 1
	}

	public RescueDP1D(int intialSampleInterval) {
		super();
		this.INITIAL_SAMPLE_INTERVAL = intialSampleInterval;
	}

	ArrayList<Double> debug_feedback_error = new ArrayList<Double>();
	double[] debug_eps_rm;

	/**
	 * 
	 * @param location
	 *            $j$ in the paper
	 * @return
	 */
	double feedback_error() {
		int current_release = n();// $n$
		double error = feedback_error(current_release);
		if (DEBUG_MODE)
			debug_feedback_error.add(error);
		return error;
	}

	double feedback_error(int sampling_point_number) {
		int k_o = this.sampling_points.get(sampling_point_number);
		int k_o_before = this.sampling_points.get(sampling_point_number - 1);
		double release_now = this.releases[k_o];// $r^j_k_o$ or $k_o[j]$
		double release_before = this.releases[k_o_before];// $r^j_k_{o-1}$ or $k_{o-1}[j]$
		return Math.abs(release_now - release_before);
	}

	/**
	 * 
	 * @return
	 */
	int n() {
		return sampling_points.size() - 1;
	}

	/**
	 * current
	 * 
	 * @return
	 */
	double k_n() {
		int n = n();
		return releases[n];
	}

	/**
	 * 
	 * @param location
	 *            $j$ in the paper
	 * @return
	 */
	double pid_error() {
		double delta_j = 0.0d;

		// (1) proportional gain
		double proportional_gain = K_p * feedback_error();
		// (2)integral gain
		double integral_gain = 0.0d;
		int n = n();// current release number
		for (int o = Math.max(1, n - pi + 1); o <= n; o++) {// we need at least two sampling points
			integral_gain += feedback_error(o);
		}
		integral_gain /= Math.min(pi, n);
		integral_gain *= K_i;
		// (3)derivative gain
		/*
		 * int k_n = this.sampling_points.get(n()); int k_n_before =
		 * this.sampling_points.get(n()-1); double derivative_gain =
		 * K_d*feedback_error()/(double)(k_n-k_n_before);
		 */

		delta_j = proportional_gain + integral_gain;// +derivative_gain;
		return delta_j;
	}

	/**
	 * Computes and sets the next sampling interval. I.e., sampleIntervalLength
	 * member and nextSamplePoint.
	 * 
	 * @param location
	 *            0 in 1D stream.
	 * @param lambda_r
	 *            A lower bound for the noise in t+1.
	 */
	void next_sampling_interval(final double lambda_r) {
		double delta_j = pid_error();
		double expected_error = lambda_r;
		// System.out.println("delta, lambda_r "+delta_j+" "+lambda_r);
		double temp = (delta_j / expected_error) * (delta_j / expected_error);// squared fraction of approximation and
																				// perturbation error.

		double next_interval = Math.max(1.0d, sampleIntervalLength + (theta * (1.0d - temp)));
		sampleIntervalLength = next_interval;// XXX let that a double, hope that makes sense, don't wann cast...
		this.nextSamplePoint += (int) sampleIntervalLength;
		// System.out.println(this.sampleIntervalLength+" i.e. at
		// "+this.nextSamplePoint);
	}

	double adaptive_budget_allocation(double eps_remaining) {
		double p = phi * Math.log(sampleIntervalLength + 1.0d); // portion of eps_r used now
		p = Math.min(p, p_max);// bound fraction of e_rm to use
		double e_i = Math.min(p * eps_remaining, e_max);// bound absolute size of portion
		return e_i;
	}

	public double[] run1D(double[] org_stream, double sensitivity, int w, double[] san_stream, double epsilon) {
		this.sensitivity = sensitivity;// setSensitivity(sensitivity);
		e_max = (3 * epsilon) / w;// max spend 3 times uniform eps
		final int length = org_stream.length;
		if (USE_KALMAN_FILTER) {
			// init Kalman Filter
			this.p = 10000.0d; // XXX don't ask me why like this...
			this.q = variance(org_stream);// Process Model x_k=x_{k-1}+\omega , with \omega ~ N(0,Q)
			this.r = sensitivity * sensitivity * w * w / (epsilon * epsilon);
			this.k = 0.0d;
		}

		this.sampleIntervalLength = this.INITIAL_SAMPLE_INTERVAL;
		this.nextSamplePoint = Math.max(1, (int) sampleIntervalLength);
		final double[] used_budget = new double[length];
		double used_budget_in_this_window = 0.0d;

		// perform first release
		int t = 0;
		// (1) -> get org data
		double val = org_stream[t];
		// (2) -> all regions publish at t=0
		sampling_points = new ArrayList<Integer>();
		sampling_points.add(t);
		// (3) -> no grouping
		// (4) -> budget
		double eps_rm = epsilon - used_budget_in_this_window;
		if (DEBUG_MODE)
			debug_eps_rm = new double[length];
		if (DEBUG_MODE)
			debug_eps_rm[t] = eps_rm;
		double eps = adaptive_budget_allocation(eps_rm);
		used_budget[t] = eps;
		used_budget_in_this_window += eps;
		// (5) add noise
		releases = san_stream;
		releases[t] = Mechanism.sanitize(val, this.sensitivity / eps);
		// (6) Filtering not in first release, need at least one prior release
		// (7) new sampling interval -> no need here, is controlled by initial sampling
		// interval member

		for (t = 1; t < length; t++) {
			if (t >= w) {// budget recovery
				double temp = used_budget[t - w];// this one is not relevant anymore
				used_budget_in_this_window -= temp;
			}
			if (t == nextSamplePoint) {
				// we know whom to sample...
				// (1) -> get org data
				val = org_stream[t];
				sampling_points.add(t);
				// (3) -> no grouping
				// (4) -> budget
				eps_rm = epsilon - used_budget_in_this_window;
				eps = adaptive_budget_allocation(eps_rm);
				if (eps < 0.000001) {
					System.err.println("t=" + t + " " + eps);
				}

				// (5) add noise
				double temp = Mechanism.sanitize(val, this.sensitivity / eps);
				releases[t] = temp;
				used_budget[t] = eps;
				used_budget_in_this_window += eps;
				if (DEBUG_MODE)
					debug_eps_rm[t] = eps_rm;
				// (6) Filtering
				if (USE_KALMAN_FILTER) {
					temp = applyKalmanFilter(releases[t - 1], releases[t]);
					if (temp == Double.POSITIVE_INFINITY) {
						System.err.println();
					}
					if (temp == Double.NaN) {
						System.err.println();
					}
					releases[t] = temp;
				}
				// (7) new sampling interval
				next_sampling_interval(lambda_r(used_budget_in_this_window, t, used_budget, w, epsilon));
			} else {
				releases[t] = releases[t - 1];
				if (USE_KALMAN_FILTER) {
					p = p + q; // According to FAST paper, one needs to execute the predict part.
					// k = p / (p + r);
					// p = p * (1.0d - k);
				}
			}
		}
		// System.out.println("eps\t"+outTSV(used_budget));
		// System.out.println("release\t"+outTSV(this.releases));
		return this.releases;
	}

	private static final double variance(final double[] data) {
		// The mean average
		double mean = 0.0;
		for (double val : data) {
			mean += val;
		}
		mean /= data.length;

		// The variance
		double variance = 0.0d;
		for (double val : data) {
			variance += (val - mean) * (val - mean);
		}
		variance /= data.length;
		return variance;
	}

	/**
	 * Algo 3 in the paper
	 * 
	 * @param last_release
	 *            x_{j|i}
	 * @param noisy_observation
	 * @return
	 */
	double applyKalmanFilter(final double last_release, final double noisy_observation) {
		// Prediction
		p = p + q; // (2) add zum alten estimate aus N(0,q) einfach neuen noise q... approximates
					// measurement noise
		// Correction
		k = p / (p + r); // (3)
		// Correction
		double posterior_estimation = last_release + k * (noisy_observation - last_release);
		p = p * (1.0d - k);
		if (Mechanism.TRUNCATE) {
			posterior_estimation = Math.max(0, Math.round(posterior_estimation));// map to query domain;;
		}
		return posterior_estimation;
	}

	/**
	 * Computes at bound for the lambda in t+1. Required to determine the next
	 * sample interval.
	 * 
	 * @param used_budget_in_this_window
	 * @param t
	 * @param used_budget
	 * @return
	 */
	private double lambda_r(double used_budget_in_prior_window, final int t, double[] used_budget, int w,
			double epsilon) {
		if (t + 1 >= w) {// budget recovery
			used_budget_in_prior_window -= used_budget[t + 1 - w];// NOT by reference
		}
		double eps_remaining = epsilon - used_budget_in_prior_window;
		double eps_possible = (USE_LAMBDA_R_PAPER) ? Math.min(eps_remaining, this.e_max)
				: adaptive_budget_allocation(eps_remaining);
		;
		return this.sensitivity / eps_possible;
	}

	public String name() {
		return "No Group RescueDP interval of " + this.INITIAL_SAMPLE_INTERVAL
				+ ((USE_KALMAN_FILTER) ? " and Kalman Filter" : "");
	}
}
