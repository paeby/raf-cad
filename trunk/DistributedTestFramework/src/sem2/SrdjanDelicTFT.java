package sem2;

import java.util.Arrays;
import java.util.HashMap;

import kids.dist.common.DistributedSystem;
import kids.dist.common.problem.InitiableSolution;
import kids.dist.seminarski2.DistributedHashTable;
//Srdjan Delic 19/05
import kids.dist.seminarski2.DistributedHashTableTester;

public class SrdjanDelicTFT implements InitiableSolution, DistributedHashTable{
	DistributedSystem system;
	int[] svi=null;
	Object vrednost;
	//zbog cekanja node-a

	long vremecekanja=100;
	boolean primljena=false;
	HashMap<Integer,Object> mapa = new HashMap<Integer,Object>();

	public void initialize() {
		svi =new int[system.getProcessNeighbourhood().length+1];
		for(int i=0;i<system.getProcessNeighbourhood().length;i++)
			svi[i]=system.getProcessNeighbourhood()[i];

		svi[svi.length-1]= system.getProcessId();
		Arrays.sort(svi);
		// TODO Auto-generated method stub

	}

	public void messageReceived(int from, int type, Object message) {
		// TODO Auto-generated method stub

		if(type==0){
			Object[]obj = (Object[]) message;
			mapa.put((Integer) obj[0], obj[1]);
			//odgovaramo dali je odredjna vrednost ubacena
			//
			system.sendMessage(from, 2, null);

		}else if(type==1){
			Object novaVrednost = mapa.get((Integer)message);
			system.sendMessage(from, 2, novaVrednost);

		}else if (type==2){

			vrednost = message;
			primljena=true;
		}


	}

	public Object get(int hash) {

		int poslao = hash%svi.length;
		int bc= poslao+1;
		if(bc==svi.length)
			bc=0;
		//cit = svi[poslao];
		if(svi[poslao]==system.getProcessId()){
			return mapa.get(hash);
		}else{
			system.sendMessage(svi[poslao], 1, hash);
			//		cekanje dok on ne vrati tu vrednost
			long vreme = System.currentTimeMillis();
			while(!primljena && (vreme+vremecekanja>=System.currentTimeMillis()))
				system.yield();
			if(!primljena){
				if(!(svi[bc]==system.getProcessId())){
					system.sendMessage(svi[bc],1, hash);
					while(!primljena) system.yield();
					primljena=false;
				}else return mapa.get(hash);
			}
			primljena =false;
		}
		return vrednost;
	}

	public void put(int hash, Object object) {
		//zbog ravnomernog slanja svima, bilo ko da dobije hash ,ostatkom pri deljenju ravnomerno ce poslati
		int kome = hash%svi.length;
		//definisemo backup(susedni ili prvi)
		int bc= kome+1;
		if(bc==svi.length)
			bc=0;

		if(svi[kome]==system.getProcessId()){
			//saljemo backup-u
			system.sendMessage(svi[bc], 0, new Object[]{hash,object});
			long vreme = System.currentTimeMillis();
			while(!primljena && (vreme+vremecekanja>=System.currentTimeMillis()))
				system.yield();
			if(primljena)
				primljena=false;


			mapa.put(hash,object);

		}else{

			system.sendMessage(svi[kome], 0,new Object[]{hash, object});
			long vreme = System.currentTimeMillis();
			while(!primljena && (vreme+vremecekanja>=System.currentTimeMillis()))
				system.yield();
			if(primljena)
				primljena=false;
			if(svi[bc]==system.getProcessId()){
				mapa.put(hash,object);
			}else{
				system.sendMessage(svi[bc], 0,new Object[]{hash, object});
				vreme = System.currentTimeMillis();
				while(!primljena && (vreme+vremecekanja>=System.currentTimeMillis()))
					system.yield();
				if(primljena)
					primljena=false;
			}
		}
	}
	
	public static void main(String[] args) {
		DistributedHashTableTester.testDHT(SrdjanDelicTFT.class, true, false, true);
	}

} 
