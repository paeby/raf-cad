package core;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import testUtil.StupidRandom;

public class ErroneousSeedFeedTest {
	
	@Test
	public void testErroneousSeedFeedTest() {
		Random stupidRandom = new StupidRandom(0l);
		ErroneousSeedFeed seedFeed = new ErroneousSeedFeed(2, stupidRandom);
		long seed;
		
		// testing basics
		seedFeed.initInteration();
		seed = seedFeed.getNextSeed();
		assertEquals(0, seed);
		seedFeed.markLastSeedAsSuccess();
		seed = seedFeed.getNextSeed();
		assertEquals(1, seed);
		// error found, this seed should repeat two times when taking from the top
		// but not immediately 
		seedFeed.markLastSeedAsErroneous();
		seed = seedFeed.getNextSeed();
		assertEquals(2, seed);
		seedFeed.markLastSeedAsSuccess();
		seed = seedFeed.getNextSeed();
		assertEquals(3, seed);
		seedFeed.markLastSeedAsSuccess();
		
		// next round of seeds. Erroneous seed should be first
		seedFeed.initInteration();
		seed = seedFeed.getNextSeed();
		assertEquals(1, seed);
		seedFeed.markLastSeedAsSuccess();
		// but after that, randoms are generated. Erroneous seed has been solved
		// successively, and needs to be solved once more before being erased
		seed = seedFeed.getNextSeed();
		assertEquals(4, seed);
		seedFeed.markLastSeedAsSuccess();
		
		// again from the top; one last time we see the erroneous seed
		seedFeed.initInteration();
		seed = seedFeed.getNextSeed();
		assertEquals(1, seed);
		seedFeed.markLastSeedAsSuccess();
		seed = seedFeed.getNextSeed();
		assertEquals(5, seed);
		seedFeed.markLastSeedAsSuccess();
		
		// again from the top; no more erroneous seed
		seedFeed.initInteration();
		seed = seedFeed.getNextSeed();
		assertEquals(6, seed);
		seedFeed.markLastSeedAsSuccess();
		
		
		
	}
	
}
