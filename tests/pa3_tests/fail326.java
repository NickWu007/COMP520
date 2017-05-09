/*** line 7: cannot assign "c" with type "C2" a value of type "C1"
 * COMP 520
 * Type checking
 */
class Fail326 { 
    public static void main(String [] args) { 
	C2 c = new C1(); 
    }
}

class C1 { }

class C2 { }
