package mainPackage;

public class ClassWithInnerClass {

    public final String someString;
    public final InnerClass innerInstance;
    public final Object objectFromStaticInner;
    private final PrivateInnerClass privateInnerInstance;
    public static final StaticInnerClass staticInner = new StaticInnerClass();

    public ClassWithInnerClass(){
        someString = "someString";
        innerInstance = new InnerClass(42);
        privateInnerInstance = new PrivateInnerClass(888);
        objectFromStaticInner = new StaticInnerClass().giveMeAnObject();
    }

    public boolean somethingSomething(){
        boolean b = privateInnerInstance.getValue() == privateInnerInstance.getBoxedValue();
        return b;
    }

    public class InnerClass{

        private final int intValue;

        public InnerClass(int value){
            intValue = value;
        }

        public int getValue(){
            return intValue;
        }
    }

    private class PrivateInnerClass{

        private final Float floatValue;

        public PrivateInnerClass(float value){
            floatValue = new Float(value);
        }

        public float getValue(){
            return floatValue; //auto-unbox!
        }

        public Float getBoxedValue(){
            return floatValue;
        }
    }

    public static class StaticInnerClass{
        private final Object o = new Object();

        public Object giveMeAnObject(){
            return o;
        }
    }
}
