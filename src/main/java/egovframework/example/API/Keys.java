package egovframework.example.API;

public class Keys {
	//add own key
		public static String OPENAPI_KEY = "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"; 
		//public static String OPENAPI_ORGKEY = "";
		//public static String PINECONE_KEY = "";
		
		public static Keys keys = new Keys();
		public Keys getInstance() {
			return keys;
		}
}
