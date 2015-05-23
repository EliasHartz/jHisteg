package thisPackageOfMine;

public class TestClass {

	public static boolean someStaticBoolean = true;

	private final String anotherString;
        protected String s = "lalalalalaitisnow!";
        private final int number; 

	public static void main(String[] args) {
		System.out.println("Hello World");
	}

	public TestClass(){
		anotherString = "main class";
		if (TestClass.someStaticBoolean)
			number = 42;
		else
			number = 44;
	}

	public String getS(){
		return s;
	}

	protected static void setTheStaticBoolean(boolean b){
		TestClass.someStaticBoolean = b;
	}
}
