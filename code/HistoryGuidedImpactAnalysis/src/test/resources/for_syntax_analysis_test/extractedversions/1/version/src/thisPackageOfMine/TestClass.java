package thisPackageOfMine;

public class TestClass {

	public static boolean someStaticBoolean = true;

	private final String anotherString;
        private String s = "lalalalalaitisnow!"; //this is not detected by Change Distiller, which is really SHIT :-(
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

	private void myMethod(String blub, String blub2){
		System.out.println("BLUB: " + blub + blub2 );
        }
}
