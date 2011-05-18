package kids.dist.seminarski2;

import kids.dist.common.problem.Solution;

public interface DistributedHashTable extends Solution {
	/**
	 * Dodaje objekat u DHT pod datim hash-om. Hash je unikatan za
	 * svaki objekat: u slučaju dodavanja na već postojeći hash nije
	 * potrebno uraditi išta.
	 * 
	 * @param hash unikatni ID objekta
	 * @param object objekat
	 */
	public void put(int hash, Object object);
	
	/**
	 * Traži objekat pod datim hash-om u DHT-u
	 * 
	 * @param hash unikatni ID objekta
	 * @return objekat pod hashom, ili null ukoliko objekat nije pronađen 
	 */
	public Object get(int hash);
	
	/**
	 * Uklanja objekat pod datim hash-om iz DHT-a. Ukoliko objekat
	 * ne postoji u DHT-u metoda ne mora da radi išta.
	 * @param hash unikatni ID objekta
	 */
	public void remove(int hash);
}
