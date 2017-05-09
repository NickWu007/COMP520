/*** line 12: cannot return a value from a method whose result type is void
 * COMP 520
 * Type Checking
 */
class fail313 { 	
    public static void main(String[] args) {
	fail313 f = new fail313();
	f.noresult();
    }
    
    public void noresult() {
	return 10;
    }
}
