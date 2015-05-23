package mainPackage;

public class ReallyEvilInnerClassCase {

  public int getValue(){
		return 1;
	}
  public static ReallyEvilInnerClassCase innerWithoutEnclosing = new ReallyEvilInnerClassCase() {
		public int getValue(){
			return -1;
		}
	};
}
