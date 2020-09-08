
import java.util.Arrays;

public class Sample {

	private static double[] sample(double[] stream, final int w, final double[] sensitivity, final double[] san_stream,
			double epsilon) {
		final int length = stream.length;

		for (int t = 0; t < length;) {
			double val = stream[t];
			double san_val_t;
			double sen_t = sensitivity[t];
			if (sen_t > 0.0d) {
				double lambda = sen_t / epsilon;
				san_val_t = Mechanism.sanitize(val, lambda);
			} else {
				san_val_t = val;
			}
			san_stream[t] = san_val_t;
			// now skip the next w time stamps
			int later = t;
			for (; later < Math.min(t + w - 1, length); later++) {
				san_stream[later] = san_val_t;
			}
			t = later;// yes, we skip them.
		}
		return san_stream;
	}

	/**
	 * w-event Sample - no effect exploited
	 */
	public static double[] SAMPLE(double[] org_stream, final double global_sensitivity, final int w,
			final double[] san_stream, double epsilon) {
		final int length = org_stream.length;
		final double[] temporal_sensitivity = new double[length];
		Arrays.fill(temporal_sensitivity, global_sensitivity);
		return sample(org_stream, w, temporal_sensitivity, san_stream, epsilon);
	}

	/**
	 * Sampling mechanism exploiting only TEAS
	 */
	public static double[] TS_SAMPLE(double[] org_stream, final double[] temporal_sensitivity, final int w,
			final double[] san_stream, double epsilon) {
		return sample(org_stream, w, temporal_sensitivity, san_stream, epsilon);
	}

	/**
	 * Sampling mechanism exploiting only TINAR
	 * 
	 */
	public static double[] TINAR_ONLY_SAMPLE(double[] stream, PolicyCollection pc, final double global_sensitivity,
			final double[] san_stream, double epsilon) {
		final int length = stream.length;
		final double[] temporal_sensitivity = new double[length];
		Arrays.fill(temporal_sensitivity, global_sensitivity);
		return Unicorn_Mechanism.UNICORN_IS(stream, pc, temporal_sensitivity, san_stream, epsilon);
	}
}
