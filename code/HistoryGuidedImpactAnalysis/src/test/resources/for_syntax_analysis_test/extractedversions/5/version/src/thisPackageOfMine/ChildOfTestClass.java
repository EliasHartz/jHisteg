package thisPackageOfMine;

public class ChildOfTestClass extends TestClass {

	//synchronized is not being detected, neither as modifier nor as the block-statement is actually is. DAMN YOU CHANGE DISTILLER!
	public synchronized String getS(){
		return "child_"+s;
	}

	public class MyInnerClass{
		public final String desc = "This is okay";

		public void printSomethingInner(){
		  System.out.println("somethingInner");
		}
	}
}

class MyOtherClass{
  public String desc = "This is just terrible programming style!";

  protected synchronized static void printSomethingOther(){
  	System.out.println("somethingOther");
  }
}
