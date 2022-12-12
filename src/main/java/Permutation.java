public enum Permutation {
    PER_1(0,3),
    PER_2(4,5),
    PER_3(6,7),
    PER_4(1,1),
    PER_5(3,5),
    PER_6(6,9),
    PER_7(0,3),
    PER_8(5,7),
    PER_9(8,9),
    PER_10(1,2),
    PER_11(3,4),
    PER_12(5,8),
    PER_13(0,2),
    PER_14(4,7),
    PER_15(0,5),
    PER_16(6,9),
    PER_17(1,8),
    PER_18(0,3),
    PER_19(4,9),
    PER_20(0,9),
    PER_21(3,6),
    PER_22(7,9),

    ;


    public final int a;
    public final int b;

    Permutation(int a, int b) {
        this.a = a;
        this.b = b;
    }
}
