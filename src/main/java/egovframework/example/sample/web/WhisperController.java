package egovframework.example.sample.web;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.service.OpenAiService;

import egovframework.example.API.Keys;

public class WhisperController {
	private static final Logger logger = LogManager.getLogger(EgovSampleController.class);
	private OpenAiService service;
	//= new OpenAiService(Keys.OPENAPI_KEY, Duration.ofMinutes(9999));
	//CreateTranscriptionRequest createTranscriptionRequest;
	
	public WhisperController(){
		this.setService(new OpenAiService(Keys.OPENAPI_KEY, Duration.ofMinutes(9999)));
		//createTranscriptionRequest = CreateTranscriptionRequest.builder().model("whisper-1").build();
		
		//String transcription_result = service.createTranscription(createTranscriptionRequest, absolutePathString).getText();
		//logger.debug(transcription_result);
	}

	public OpenAiService getService() {
		return service;
	}

	public void setService(OpenAiService service) {
		this.service = service;
	}
}
