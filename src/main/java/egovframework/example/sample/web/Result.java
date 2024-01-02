package egovframework.example.sample.web;

import java.util.Map;

public class Result {
	public Map<String, String> getResult(Map<String, String> response, long file_size, String summary_result, long executionTime) {
		response.put("isSuccess", "true");
		response.put("finalFileSize", Long.toString(file_size) + " bytes");
		response.put("summary_result", summary_result);
		response.put("executionTimeInMilli", Long.toString(executionTime));
		
		return response;
	}
}
