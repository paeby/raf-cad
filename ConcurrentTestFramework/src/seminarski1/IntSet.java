package seminarski1;


public interface IntSet {
	/**
	 * Ova metoda pokušava da ubaci <tt>value</tt> u skup, i vraća da li je
	 * ubacivanje uspelo ili ne. Ubacivanje ne treba da uspe ukoliko se dati
	 * <tt>value</tt> već nalazi u skupu.
	 * 
	 * <p>
	 * Možete pretpostaviti da <tt>value</tt> nikada neće biti jednako
	 * <tt>Integer.MAX_VALUE</tt> ili <tt>Integer.MIN_VALUE</tt>. Sve druge
	 * vrednosti su dozvoljene.
	 * 
	 * @param value
	 *            broj za ubacivanje
	 * @return da li je broj uspešno ubačen ili ne
	 */
	public boolean addInt(int value);
	
	/**
	 * Ova metoda pokušava da ukloni <tt>value</tt> iz skupa, i vraća da li je
	 * uklonjen ili ne: nije uklonjen ukoliko se <tt>value</tt> nije prethodno
	 * nalazio u skupu.
	 * 
	 * <p>
	 * Možete pretpostaviti da <tt>value</tt> nikada neće biti jednako
	 * <tt>Integer.MAX_VALUE</tt> ili <tt>Integer.MIN_VALUE</tt>. Sve druge
	 * vrednosti su dozvoljene.
	 * 
	 * @param value
	 *            broj za uklanjanje
	 * @return da li je broj uspešno izbačen ili ne
	 */
	public boolean removeInt(int value);
	
	/**
	 * Ova metoda proverava da li se <tt>value</tt> nalazi u skupu ili ne.
	 * 
	 * <p>
	 * Možete pretpostaviti da <tt>value</tt> nikada neće biti jednako
	 * <tt>Integer.MAX_VALUE</tt> ili <tt>Integer.MIN_VALUE</tt>. Sve druge
	 * vrednosti su dozvoljene.
	 * 
	 * @param value
	 *            element
	 * @return da li se <tt>value</tt> nalazi u skupu ili ne
	 */
	public boolean contains(int value);
}
