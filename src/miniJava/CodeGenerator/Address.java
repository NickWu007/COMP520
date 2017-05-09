package miniJava.CodeGenerator;

import miniJava.mJAM.Machine;

/**
 * Created by NickWu on 2017/4/3.
 */
public class Address extends RuntimeEntity{

    public RuntimeAddress address;

    public Address(){
        super();
        address = null;
    }

    public Address(int size, int offset) {
        super(size);
        address = new RuntimeAddress(offset);
    }
}
