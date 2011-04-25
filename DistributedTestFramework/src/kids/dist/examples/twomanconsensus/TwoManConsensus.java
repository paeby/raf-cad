package kids.dist.examples.twomanconsensus;

import kids.dist.examples.consensus.Consensus;

public interface TwoManConsensus extends Consensus {
	public int propose(int proposedValue);
}
