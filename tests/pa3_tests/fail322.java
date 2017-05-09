/*** line 11: "x" is not an array so cannot be indexed
 * COMP 520
 * Identification
 */
class Fail322 {
    // public static void main(String[] args) {}

    public int x;

    void f() {
	x = x[3];
    }
}
