package otherPackage;

import mainPackage.HelloWorldMain;

public class IhateTheWorld extends HelloWorldMain{
	
   public static void main(String[] args) {
      System.out.println("I hate the world");
   }
   
	public IhateTheWorld(){
		super("I hate the world");
	}
	
	public void say(){
		if (!whatToSay.equals("I hate the world"))
			System.out.println("I REALLY REALLY HATE THE WORLD!");
		else super.say();
	}
	
   public void setWhatToSay(String newData){
      /* nothing in fact, cannot be set */
   }

}
