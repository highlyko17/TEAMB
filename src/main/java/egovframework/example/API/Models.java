package egovframework.example.API;

public class Models {
	public static String WHISPER_MODEL = "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"; 
	public static String SUMMARY_MODEL = "";
	//public static String OPENAPI_ORGKEY = "";
	//public static String PINECONE_KEY = "";
	
	public static Models models = new Models();
	public Models getInstance() {
		return models;
	}
}
