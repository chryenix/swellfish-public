
import cern.jet.random.Distributions;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

/**
 * This generator returns Laplace distributed numbers using the pseudo-random
 * generator MersenneTwister. If no Seed is given, it is non-deterministic.
 * 
 * @author Tex
 *
 */
public class LaplaceRandomGenerator {

	private RandomEngine generator;

	public LaplaceRandomGenerator() {
		generator = new MersenneTwister(new java.util.Date());
	}

	public LaplaceRandomGenerator(int seed) {
		generator = new cern.jet.random.engine.MersenneTwister(seed);
	}

	public double nextNumber() {
		return Distributions.nextLaplace(generator);
	}

}
