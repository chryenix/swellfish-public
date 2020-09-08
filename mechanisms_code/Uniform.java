
import java.util.Arrays;

public class Uniform {

	private static double[] uniform(double[] stream, double[] sensitivity, int[] max_tinar, final double[] san_stream,
			double epsilon) {
		final int length = stream.length;
		for (int t = 0; t < length; t++) {
			double org_val = stream[t];
			double sen_t = sensitivity[t];
			double max_tinar_t = (double) max_tinar[t];

			if (sen_t == 0.0d || max_tinar_t == 0.0d) {
				san_stream[t] = org_val;
			} else {
				double eps_t = epsilon / max_tinar_t;
				double lambda = sen_t / eps_t;
				double san_val_t = Mechanism.sanitize(org_val, lambda);
				san_stream[t] = san_val_t;
			}
		}
		return san_stream;
	}

	public static double[] UNIFORM(double[] org_stream, final int w, final double global_sensitivity,
			final double[] san_stream, double epsilon) {
		final int length = org_stream.length;
		final double[] temporal_sensitivity = new double[length];
		Arrays.fill(temporal_sensitivity, global_sensitivity);
		final int[] ws = new int[length];
		Arrays.fill(ws, w);
		return uniform(org_stream, temporal_sensitivity, ws, san_stream, epsilon);
	}

	public static double[] TS_UNIFORM(double[] org_stream, double[] temporal_sensitivity, final int w,
			final double[] san_stream, double epsilon) {
		final int length = org_stream.length;
		final int[] ws = new int[length];
		Arrays.fill(ws, w);
		return uniform(org_stream, temporal_sensitivity, ws, san_stream, epsilon);
	}

	public static double[] TINAR_UNIFORM(double[] org_stream, final double global_sensitivity, int[] max_tinar,
			final double[] san_stream, double epsilon) {
		final int length = org_stream.length;
		final double[] temporal_sensitivity = new double[length];
		Arrays.fill(temporal_sensitivity, global_sensitivity);
		return uniform(org_stream, temporal_sensitivity, max_tinar, san_stream, epsilon);
	}

}
