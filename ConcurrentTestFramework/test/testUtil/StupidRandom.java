package testUtil;

import java.util.Random;

public class StupidRandom extends Random {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5408794531054442022L;
	private long seed;
	
	public StupidRandom() {
		this(0);
	}
	
	public StupidRandom(long seed) {
		this.seed = seed;
	}
	
	@Override
	public boolean nextBoolean() {
		return seed++ % 2 > 0;
	}
	
	@Override
	public void nextBytes(byte[] bytes) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	@Override
	public double nextDouble() {
		return (double) seed++;
	}
	
	@Override
	public float nextFloat() {
		return (float) seed++;
	}
	
	@Override
	public synchronized double nextGaussian() {
		throw new UnsupportedOperationException("Not implemented");		
	}
	
	@Override
	public int nextInt() {
		return (int)seed++;
	}
	
	@Override
	public int nextInt(int n) {
		return (int)seed++ % n;
	}
	
	@Override
	public long nextLong() {
		return seed++;
	}
	
	@Override
	public synchronized void setSeed(long seed) {
		this.seed = seed;
	}
	
}
