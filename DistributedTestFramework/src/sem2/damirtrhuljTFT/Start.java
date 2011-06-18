package sem2.damirtrhuljTFT;
import kids.dist.seminarski2.DistributedHashTableTester;


public class Start {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//System.out.println(12349%4);
		DistributedHashTableTester.testDHT(DamirSeminarski2.class, true, false, true);
		//DistributedHashTableTester.testDHT(DamirSeminarski2.class,8,4,true,false,true);
	}

}
