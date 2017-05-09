/*** line 9: binary operator "==" requires arguments to have the same type.
 * COMP 520
 * Type Checking
 */
class fail331 { 	
    // public static void main(String[] args) { }
       
    public void foo () {
	boolean c = (new A() == new B());
    }
}

class A {
    int y;
}

class B {
    int x;
}
