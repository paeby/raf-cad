package sem2.nenadognjanovicTTF;

import java.util.ArrayList;
import java.util.Arrays;

public class K {
	
	public static final int KEY_BITS = 8;
	
	public static final int MSG_FIND_CLOSEST_NODE = 1;
	public static final int MSG_CLOSEST_REPLY = 2;
	public static final int MSG_STORE = 3;
	public static final int MSG_STORE_REPLY = 4;
	public static final int MSG_GET = 5;
	public static final int MSG_GET_REPLY = 6;
	
	public static int distance(int nodeA, int nodeB) {
		// printBits(nodeA);printBits(nodeB);printBits(nodeA ^ nodeB);
		return nodeA ^ nodeB;
	}
	
	public static int getClosestLocallyKnownNode(int myID, int wantedID, ArrayList<Integer>[] myBuckets) {
		/*
		 * int bucket = getBucketForID(myID, wantedID);
		 * 
		 * while(bucket>0) { if(myBuckets[bucket].size()>0) return
		 * getClosestInBucket(myBuckets[bucket], wantedID); bucket--; } return
		 * myBuckets[0].get(0);
		 */

		int dist = Integer.MAX_VALUE;
		int b = -1, bi = -1;
		for (int i = 0; i <= getBucketForID(myID, wantedID); i++) {
			for (int j = 0; j < myBuckets[i].size(); j++) {
				int newDist = distance(myBuckets[i].get(j), wantedID);
				if (newDist < dist) {
					dist = newDist;
					b = i;
					bi = j;
				}
			}
		}
		
		return myBuckets[b].get(bi);
	}
	
	public static int getBucketForID(int myID, int nodeID) {
		if (nodeID == myID) {
			System.out.println("Warning - getBucketForID: requested node id " + nodeID + " is the same as mine");
			return 666999;
		}
		
		int bucket = 0;
		int dist = K.distance(myID, nodeID);
		
		while (dist > 1) {
			dist >>= 1;
			bucket++;
		}
		return bucket;
	}
	
	public static int getClosestInBucket(ArrayList<Integer> bucket, int nodeID) {
		if (bucket == null) {
			System.out.println("Error - getClosestInBucket: bucket is null");
			return 0;
		}
		if (bucket.size() == 0) {
			System.out.println("Error - getClosestInBucket: bucket is empty");
			return 0;
		}
		
		int dist = Integer.MAX_VALUE;
		int index = -1;
		for (int i = 0; i < bucket.size(); i++) {
			int newDist = distance(bucket.get(i), nodeID);
			if (newDist < dist) {
				dist = newDist;
				index = i;
			}
		}
		return bucket.get(index);
	}
	
	public static void printBits(int number) {
		System.out.println(String.format("%1$8s", Integer.toBinaryString(number)));
	}
	
}
