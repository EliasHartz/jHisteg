package mainPackage;

public class ClassWithInnerClass {

    public final String someString = "someString";
    public final InnerClass innerInstance;
    public final Object objectFromStaticInner;
    private final PrivateInnerClass privateInnerInstance;
    public static final StaticInnerClass staticInner = new StaticInnerClass();

    public ClassWithInnerClass(){
        innerInstance = new InnerClass(42);
        privateInnerInstance = new PrivateInnerClass(888);
        objectFromStaticInner = new StaticInnerClass().giveMeAnObject();
    }

    public class InnerClass{

        private final int intValue;

        public InnerClass(int value){
	    if (value < 0)
		value = (-value);
            intValue = value;
        }
    }

    private class PrivateInnerClass{

        private final Float floatValue;

        public PrivateInnerClass(float value){
	    if (value < 0f)
		value = (-value);
            floatValue = new Float(value);
        }

        public Float getBoxedValue(){
            return floatValue;
        }
    }

    public static class StaticInnerClass{
        public Object giveMeAnObject(){
            return new String("blubber");
        }
    }
}
