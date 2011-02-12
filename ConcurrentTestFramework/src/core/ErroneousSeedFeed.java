package core;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

public class ErroneousSeedFeed {
	
	private final int noOfSuccessesToEraseFailure;
	private final LinkedList<ErroneousSeed> seeds;
	private final Random seedGenerator;
	private ListIterator<ErroneousSeed> seedIterator;
	
	public ErroneousSeedFeed() {
		this(DEFAULT_NUMBER_OF_SUCCESSES_TO_ERASE_FAILURE, new Random());
	}
	
	public ErroneousSeedFeed(int noOfSuccessesToEraseFailure, Random random) {
		super();
		this.noOfSuccessesToEraseFailure = noOfSuccessesToEraseFailure;
		this.seedGenerator = random;
		this.seeds = new LinkedList<ErroneousSeed>();
	}
	
	public long getNextSeed() {
		if (seedIterator.hasNext()) {
			return seedIterator.next().seed;
		} else {
			ErroneousSeed newSeed = new ErroneousSeed(seedGenerator.nextLong());
			newSeed.success_count = noOfSuccessesToEraseFailure - 1;
			seedIterator.add(newSeed);
			return newSeed.seed;
		}
	}
	
	public void markLastSeedAsSuccess() {
		ErroneousSeed prevSeed = seedIterator.previous();
		prevSeed.success_count++;
		if (prevSeed.success_count >= noOfSuccessesToEraseFailure)
			seedIterator.remove();
		else
			seedIterator.next();
	}
	
	public void markLastSeedAsErroneous() {
		ErroneousSeed erroneousSeed = seedIterator.previous();
		erroneousSeed.success_count = 0;
		seedIterator.remove();
		// ova komplikacija da bi izbegao concurrentmodificationexception;
		// iako dodajem element na početak liste kada je iterator na višim
		// pozicijama. Ovo bi se izbeglo custom implementacijom linkedliste
		int index = seedIterator.nextIndex();
		seeds.addFirst(erroneousSeed);
		seedIterator = seeds.listIterator(index + 1);
	}
	
	public void initInteration() {
		seedIterator = seeds.listIterator();
	}
	
	public void clearSeedFeed() {
		seedIterator = null;
		seeds.clear();
	}
	
	protected static final class ErroneousSeed {
		protected final long seed;
		protected int success_count;
		
		public ErroneousSeed(long seed) {
			super();
			this.seed = seed;
			this.success_count = 0;
		}
		
		@Override
		public String toString() {
			return "[" + seed + ": " + success_count + "]";
		}
	}
	
	public static final int DEFAULT_NUMBER_OF_SUCCESSES_TO_ERASE_FAILURE = 5;
	
}
