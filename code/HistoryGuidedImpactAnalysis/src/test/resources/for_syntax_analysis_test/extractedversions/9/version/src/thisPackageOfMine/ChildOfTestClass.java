package thisPackageOfMine;

public class ChildOfTestClass extends TestClass {

	public synchronized String myNowRenamedMethod(){
		return "child_"+s;
	}

	public class MyInnerClass{
		public final String desc = "This is okay";

		public void printSomethingInner(){
		  System.out.println("somethingInner");
		}
	}
}

class MyNowRenamedClass{
  public String desc = "This is just terrible programming style!";

  protected synchronized static void printSomethingOther(){
  	System.out.println("somethingOther");
  }
}
