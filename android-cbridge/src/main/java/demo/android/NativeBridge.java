package demo.android;

public final class NativeBridge {
    static {
        System.loadLibrary("demo_android");
    }

    private NativeBridge() {}

    public static native long sum(int[] values);
    public static native String backend();
}
