public class Mechanism {

	public static boolean TRUNCATE = false;
	public static boolean USE_NON_PRIVATE_SAMPLING = false;
	public static LaplaceRandomGenerator generator = new LaplaceRandomGenerator();

	public static final double[] sanitize(final double[] org_stream_t, final double lambda) {
		final int dim = org_stream_t.length;
		final double[] sanitized_stream_t = new double[dim];
		for (int d = 0; d < dim; d++) {
			final double noise = generator.nextNumber() * lambda;
			sanitized_stream_t[d] = org_stream_t[d] + noise;
		}
		if (TRUNCATE) {
			for (int i = 0; i < sanitized_stream_t.length; i++) {
				double temp = sanitized_stream_t[i];
				temp = Math.max(0, temp);// allow no negative values
				temp = Math.round(temp);
				sanitized_stream_t[i] = temp;
			}
		}
		return sanitized_stream_t;
	}

	public static final double sanitize(final double org_stream_t, final double lambda) {

		final double noise = generator.nextNumber() * lambda;
		double sanitized_stream_t = org_stream_t + noise;

		if (TRUNCATE) {
			double temp = sanitized_stream_t;
			temp = Math.max(0, temp);// allow no negative values
			sanitized_stream_t = temp;

		}
		return sanitized_stream_t;
	}

	static double private_dissimilarity(final double last_published, final double org_val_t, final double lambda) {
		double dissimilarity = dissimilarity(last_published, org_val_t);
		if (Mechanism.USE_NON_PRIVATE_SAMPLING == false) {
			dissimilarity += generator.nextNumber() * lambda;// add noise
		}
		return dissimilarity;
	}

	static double dissimilarity(final double last_published, final double org_val_t) {
		double dissimilarity = Math.abs(last_published - org_val_t);
		return dissimilarity;
	}

}
