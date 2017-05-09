/**
 * COMP 520
 * IxIdRef
 */
class Pass390 {         

    public static void main(String[] args) {
        Arr2D m = new Arr2D();
	int v = m.row[1].col[3];  
    }
}

class Arr2D { Arr1D [] row; }

class Arr1D { int [] col; }
