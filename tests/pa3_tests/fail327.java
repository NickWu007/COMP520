/*** line 11: index expression "a" has type "int []" where type "int" is required 
 * COMP 520
 * Identification
 */
class Fail327 {
    // public static void main(String[] args) {}

    public int [] a;

    void f() {
	int x = a[a];
    }
}