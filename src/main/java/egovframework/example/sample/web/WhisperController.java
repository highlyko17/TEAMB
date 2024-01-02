package egovframework.example.sample.web;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import egovframework.example.API.Keys;

public class WhisperController {
	private static final Logger logger = LogManager.getLogger(EgovSampleController.class);
	OpenAiService service;
	CreateTranscriptionRequest createTranscriptionRequest;
	String transcription_result;
	List<ChatMessage> message;
	
	public String getTranscription_result() {
		return transcription_result;
	}

	public void setTranscription_result(String transcription_result) {
		this.transcription_result = transcription_result;
	}
	
	public OpenAiService getService() {
		return service;
	}

	public void setService(OpenAiService service) {
		this.service = service;
	}

	WhisperController(){
		service = new OpenAiService(Keys.OPENAPI_KEY, Duration.ofMinutes(9999));
		createTranscriptionRequest = CreateTranscriptionRequest.builder().model("whisper-1").build();
	}

	public void transcript(String absolutePathString) {
		transcription_result = service.createTranscription(createTranscriptionRequest, absolutePathString).getText();
		logger.debug(transcription_result);
	}
	
	public String getResult(String str) {
		message = new ArrayList<ChatMessage>();
		message.add(new ChatMessage("user", str + transcription_result + "\""));
		
		ChatCompletionRequest completionRequest = ChatCompletionRequest.builder().messages(message)
				.model("gpt-3.5-turbo-16k")
				.maxTokens(700).temperature((double) 0.5f).build();
		String summary_result = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage()
				.getContent();
		
		return summary_result;
	}
}
