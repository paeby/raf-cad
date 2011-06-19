package sem2.gorannikolicTTT.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TwoClosest {

	public List<Integer> kClosest = new ArrayList<Integer>();
	public List<Integer> bucketsReturned = new ArrayList<Integer>();
	
	public boolean ended;
	
	public MyTimestamp timestamp;
	
	public int hash;
	public int myId;
	public boolean canStopWaiting = false;
	
	public TwoClosest(List<Integer> kClosest2, int hash2, int myId) {
		this.myId = myId;
		this.hash = hash2;
		
//		if(myIdExists())
//			ended = true;
		
			if((kClosest2.get(0) ^ hash) < (kClosest2.get(1) ^ hash))
			{
				kClosest.add(kClosest2.get(0));
				kClosest.add(kClosest2.get(1));
			}
			else
			{
				kClosest.add(kClosest2.get(1));
				kClosest.add(kClosest2.get(0));
			}
	}



	public void finishRound() {

		for(int i = 0 ;i < bucketsReturned.size(); i++)
			if((kClosest.get(0) ^ hash) > (bucketsReturned.get(i) ^ hash))
				kClosest.add(0, bucketsReturned.get(i));
			else if((kClosest.get(1) ^ hash) > (bucketsReturned.get(i) ^ hash) && (kClosest.get(0) ^ hash) < (bucketsReturned.get(i) ^ hash))
				kClosest.add(1, bucketsReturned.get(i));
		
		if(kClosest.size() > 2)
		{
			List<Integer> tempNew = new ArrayList<Integer>();
			tempNew.add(kClosest.get(0));
			tempNew.add(kClosest.get(1));
			
			kClosest = tempNew;
			
//			if(myIdExists())
//				ended = true;
		}
		else
		{
			ended = true;
			List<Integer> tempNew = new ArrayList<Integer>();
			tempNew.add(kClosest.get(0));
			tempNew.add(kClosest.get(0) ^ 0x01);
			
			kClosest = tempNew;
		}
	}

	@Override
	public String toString() {
		return "myid:" + myId + "; kClosest:" + Arrays.toString(kClosest.toArray()) + "; timestamp:" + timestamp;
	}

	private boolean myIdExists() {
		for (Integer id : kClosest) {
			if(id == myId)
				return true;
		}
		return false;
	}



	public void merge(List<Integer> newTwoClosest) {
		if(!bucketsReturned.contains(newTwoClosest.get(0)))
			bucketsReturned.add(newTwoClosest.get(0));
		if(!bucketsReturned.contains(newTwoClosest.get(1)))
			bucketsReturned.add(newTwoClosest.get(1));
		
		if(bucketsReturned.size() % 4 == 0)
			canStopWaiting = false;
	}
}
