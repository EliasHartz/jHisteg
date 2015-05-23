package mainPackage;

public class ClassWithInnerClass {

    public final String someString = "someString";
    public final InnerClass[] innerInstance;
    public final Object objectFromStaticInner;
    private final PrivateInnerClass privateInnerInstance;
    public static final StaticInnerClass staticInner = new StaticInnerClass();

    public ClassWithInnerClass(){
        innerInstance = new InnerClass[]{new InnerClass(42)};
        privateInnerInstance = new PrivateInnerClass(888);
        objectFromStaticInner = new StaticInnerClass().giveMeAnObject();
    }

    public class InnerClass{

        private final int[] intValue;

        public InnerClass(int value){
            intValue = new int[value];
        }

	public void store(int[][] arr, char[] charArr /* ignored*/){
	  int i = 0;
	  for(int[] arr2 : arr)
	    for(int value : arr2)
	        intValue[i++] = value; //might produce array-ouf-of-bounds exception
	}
    }

    private class PrivateInnerClass{

        private final Float floatValue;
	private final float[] arr;

        public PrivateInnerClass(float value){
	    if (value < 0f)
		value = (-value);
            floatValue = new Float(value);
	    arr = new float[]{0.f, 1.f, 4f};
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
