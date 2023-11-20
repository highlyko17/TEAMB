package egovframework.example.API;

public class Keys {
	//add own key
		public static String OPENAPI_KEY = "아무거"; 
		//public static String OPENAPI_ORGKEY = "";
		//public static String PINECONE_KEY = "";
		
		public static Keys keys = new Keys();
		public Keys getInstance() {
			return keys;
		}
}
