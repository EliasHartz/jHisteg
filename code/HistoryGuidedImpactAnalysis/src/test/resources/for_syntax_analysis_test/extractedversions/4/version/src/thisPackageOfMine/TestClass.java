package thisPackageOfMine;

public class TestClass {

	public static boolean someStaticBoolean = true;

	private final String anotherString;
        protected String s = "some value that cannot be modified!";
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

		Runnable r = new Runnable(){
		    @Override
		    public void run() {
		        int i = 0;
		        while(i<10) System.out.println(anotherString+" "+(++i));
		    }
		};
		Thread t = new Thread(r);
		t.run();
	}

	public TestClass(String s, int i){
		this.s = s;
		number = i;
                anotherString = "value given to constructor was '"+s+"'";
	}

	public String getTheStringThatCannotBeChanged(){
		return s;
	}

	protected static void setTheStaticBoolean(boolean b){
		TestClass.someStaticBoolean = b;
	}
}
