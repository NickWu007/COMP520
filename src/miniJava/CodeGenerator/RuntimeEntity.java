package miniJava.CodeGenerator;

import miniJava.mJAM.Machine;

/**
 * Created by NickWu on 2017/4/3.
 */
public abstract class RuntimeEntity {

    public int size;

    public RuntimeEntity() {
        size = 0;
    }

    public RuntimeEntity(int size){
        this.size = size;
    }
}
