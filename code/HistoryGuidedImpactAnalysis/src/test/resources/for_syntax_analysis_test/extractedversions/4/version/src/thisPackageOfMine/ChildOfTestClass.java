package thisPackageOfMine;

public class ChildOfTestClass extends TestClass {

	//seems like change distiller also does not detect that this method is not overwritten anymore :-( shitty library!
	public String getS(){
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
  public final String desc = "This is just terrible programming style!";

  public void printSomethingOther(){
  	System.out.println("somethingOther");
  }
}
