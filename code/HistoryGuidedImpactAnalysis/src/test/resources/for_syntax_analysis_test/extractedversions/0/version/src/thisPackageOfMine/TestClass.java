package thisPackageOfMine;

public class TestClass {

	public static boolean someStaticBoolean = true;

        private String s = "lalulalulalulalula";
        private final int number; 

	public static void main(String[] args) {
		TestClass.setTheStaticBoolean(false);
		System.out.println("Hello World");
	}

	public TestClass(){
		if (TestClass.someStaticBoolean)
			number = 42;
		else
			number = 44;
	}

	public String getS(){
		return s;
	}

	public static void setTheStaticBoolean(boolean b){
		TestClass.someStaticBoolean = b;
	}

	private void myMethod(String blub){
		System.out.println("BLUB: " + blub);
        }
}
