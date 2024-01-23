package egovframework.example.sample.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
	
	public String getSrtResult(String srt_content, String searchfor) {
		List<ChatMessage> message = new ArrayList<ChatMessage>();
        message.add(new ChatMessage("user",
                "These are the content of srt file which is extraced from an audio file." + srt_content + "From the srt content, find timestamp related to " + searchfor + " and return the timestamp."));
        
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder().messages(message)
                .model("gpt-3.5-turbo-16k")
                .maxTokens(700).temperature((double) 0.5f).build();
        
        String summary_result = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage()
                .getContent();
        
        return summary_result;
	}
	
	void startWhisperProcess(OSDetect osd, PythonModifier pc) throws IOException, InterruptedException {
		ProcessBuilder whisperProcessBuilder = new ProcessBuilder();
        
        if (osd.getOsName().toLowerCase().contains("windows")) {
          whisperProcessBuilder.environment().put("PYTHONIOENCODING", "utf-8");
           String[] cmdArray = pc.getWhisperCommand().split(" ");
           whisperProcessBuilder.command(cmdArray);
        } else if (osd.getOsName().toLowerCase().contains("mac")) {
           whisperProcessBuilder.command("bash", "-c", pc.getWhisperCommand());
        }

        logger.debug("whisperProcessBuilder.start()");
        Process whisperProcess = whisperProcessBuilder.start();

        Thread whisperOutputThread = new Thread(() -> {
           try {
              InputStream inputStream = whisperProcess.getInputStream();
              InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
              BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

              String line;
              while ((line = bufferedReader.readLine()) != null) {
                 System.out.println(line);
              }
           } catch (IOException e) {
              e.printStackTrace();
           }
        });

        Thread whisperErrorThread = new Thread(() -> {
           try {
              InputStream errorStream = whisperProcess.getErrorStream();
              InputStreamReader errorStreamReader = new InputStreamReader(errorStream);
              BufferedReader errorBufferedReader = new BufferedReader(errorStreamReader);

              String line;
              while ((line = errorBufferedReader.readLine()) != null) {
                 System.err.println(line);
              }
           } catch (IOException e) {
              e.printStackTrace();
           }
        });

        whisperOutputThread.start();
        whisperErrorThread.start();

        int exitCode;
        try {
           exitCode = whisperProcess.waitFor();
        } catch (InterruptedException e) {
           e.printStackTrace();
           exitCode = -1;
        }

        whisperOutputThread.join();
        whisperErrorThread.join();
        logger.debug("Whisper process exited with code: " + exitCode);
	}
}
