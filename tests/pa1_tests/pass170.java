// PA1 parse refs decls pass
class Test {

    int p() {
        this = that;
        this();
        this.that(5);
	this.that[2] = 3;
        this.that.those[3]= them;
        this.that[2].those();
        int [] x = 1;
        a b = c;
	p();
	p[4]();
	p[4].b = 5;
	p[4].b(3);
        a[3].b.c[2] = false;
    }
}

