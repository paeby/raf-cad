package kids.dist.util;

public class TimeoutCounter {
	long startingTime;
	long timeout;
	
	public TimeoutCounter(long milis) {
		this.timeout = milis;
		restart();
	}
	
	public void restart() {
		this.startingTime = System.currentTimeMillis();
	}
	
	public boolean timeRanOut() {
		return System.currentTimeMillis() > startingTime + timeout;
	}
}
