package thisPackageOfMine;

public class TestClass {

	public boolean myNowRenamedBoolean = true;

	private final String anotherString = "this value is so final, yeah!";
        protected String s = "some value that cannot be modified!";
        private final int number; 

	public static void main(String[] args) throws Exception{
		throw new IllegalArgumentException("BLUB");
	}

	public TestClass(){
		if (myNowRenamedBoolean && anotherString.length() > 5)
			number = 42;
		else
			number = 44;

		//execute a silly sysout in another thread!
		Runnable r = new Runnable(){
		    @Override
		    public void run() {
		        int i = 0;
		        while(i<5) System.out.println(anotherString+" "+(++i));
		    }
		};
		Thread t = new Thread(r);
		t.run();
	}

	public TestClass(String s, int i){
		this.s = s;
		number = i;
	}

	public String getTheStringThatCannotBeChanged(){
		return s;
	}

	protected void setTheStaticBoolean(boolean b){
		myNowRenamedBoolean = b;
	}
}
