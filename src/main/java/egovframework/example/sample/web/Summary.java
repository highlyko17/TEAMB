package egovframework.example.sample.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import egovframework.example.API.Keys;

@RestController
public class Summary {
	private static final Logger logger = LogManager.getLogger(EgovSampleController.class);
	
	@PostMapping("summarize_vid.do")
	@ResponseBody
	public ResponseEntity<?> summaryUsingWhisper(@RequestParam MultipartFile file, HttpServletRequest request)
			throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
		Map<String, String> response = new HashMap<>();// 결과를 맵핑할 변

		ServletContext context = request.getSession().getServletContext();
		String projectPath = context.getRealPath("/");
		String absolutePathString = "";
		logger.debug("projectPath: " + projectPath);

		/* OS detection */
		OSDetect osd = new OSDetect(projectPath);
		
		FileController fc = new FileController(response, file, osd);
		fc.exist();
		response = fc.sizing();
		
		logger.debug("Project Path: " + projectPath);
		
		absolutePathString = fc.setAbsolutePath();
		String origin_absolutePathString = new String(absolutePathString);
		logger.debug("AbsolutePathString received" + absolutePathString);
		File extractedAudio = null;
		
		fc.runFfmpeg();
		/* ffmpeg */
//		if (fc.getFile_size() > 26214400) {
//			Path extractedAbsolutePath = fc.getDirectoryPath().toAbsolutePath();
//			String extractedAbsolutePathString = "";
//			if (osd.getOsName().toLowerCase().contains("windows")) {
//				extractedAbsolutePathString = extractedAbsolutePath.toString() + "\\" + nameWithoutExtension + ".mp3";
//
//			} else {
//				extractedAbsolutePathString = extractedAbsolutePath.toString() + "/" + nameWithoutExtension + ".mp3";
//
//			}
//
//			String ffmpegCommand = osd.getFfmpeg_address() + " -i " + absolutePathString + " -vn -acodec libmp3lame "
//					+ extractedAbsolutePathString;
//			logger.debug("ffmpegCommand: " + ffmpegCommand);
//			String whisperCommand = osd.getWhisper_addr() +" "+absolutePathString;
//			logger.debug("whisperCommand: " + whisperCommand);
//			
//			
//			ProcessBuilder processBuilder = new ProcessBuilder();
//
//			if (osd.getOsName().toLowerCase().contains("windows")) {
//				String[] cmdArray = ffmpegCommand.split(" ");
//				processBuilder.command(cmdArray);
//			} else if (osd.getOsName().toLowerCase().contains("mac")) {
//				processBuilder.command("bash", "-c", ffmpegCommand);
//				
//			}
//
//			logger.debug("processBuilder.start()");
//			Process process = processBuilder.start();
//
//			Thread outputThread = new Thread(() -> {
//				try {
//					InputStream inputStream = process.getInputStream();
//					InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//					BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//
//					String line;
//					while ((line = bufferedReader.readLine()) != null) {
//						System.out.println(line);
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			});
//
//			Thread errorThread = new Thread(() -> {
//				try {
//					InputStream errorStream = process.getErrorStream();
//					InputStreamReader errorStreamReader = new InputStreamReader(errorStream);
//					BufferedReader errorBufferedReader = new BufferedReader(errorStreamReader);
//
//					String line;
//					while ((line = errorBufferedReader.readLine()) != null) {
//						System.err.println(line);
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			});
//
//			outputThread.start();
//			errorThread.start();
//
//			int exitCode;
//			try {
//				exitCode = process.waitFor();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//				exitCode = -1;
//			}
//
//			outputThread.join();
//			errorThread.join();
//			logger.debug("Extract process exited with code: " + exitCode);
//
//			extractedAudio = new File(extractedAbsolutePathString);
//
//			absolutePathString = extractedAbsolutePathString;
//			logger.debug("Extracted audio file size: " + extractedAudio.length() + " bytes");
//			file_size = extractedAudio.length();
//			if (extractedAudio.length() > 26214400) {
//				return new ResponseEntity<>("오디오만 추출했음에도 파일의 크기가 26214400bytes를 초과합니다. 파일을 분할하여 주세요.", headers,
//						HttpStatus.OK);
//			}
//		}

		// 받아온 주소를 whisper에게 보내
		OpenAiService service = new OpenAiService(Keys.OPENAPI_KEY, Duration.ofMinutes(9999));
		CreateTranscriptionRequest createTranscriptionRequest = CreateTranscriptionRequest.builder().model("whisper-1")
				.build();

		String transcription_result = service.createTranscription(createTranscriptionRequest, absolutePathString)
				.getText();
		logger.debug(transcription_result);

		if (extractedAudio != null) {
			extractedAudio.delete();
			logger.debug("Extracted audio file deleted successfully.");
		}
		File fileToDelete = new File(origin_absolutePathString);

		if (fileToDelete.exists()) {
			if (fileToDelete.delete()) {
				logger.debug("Temporary video file deleted successfully.");
			} else {
				logger.debug("Failed to delete the temporary video file.");
			}
		} else {
			logger.debug("temporary file not found.");
		}
		List<ChatMessage> message = new ArrayList<ChatMessage>();
		message.add(new ChatMessage("user",
				"다음 텍스트의 주제를 파악해서 텍스트의 언어로 700token 이하로 요약해줘: \"" + transcription_result + "\""));

		ChatCompletionRequest completionRequest = ChatCompletionRequest.builder().messages(message)
				.model("gpt-3.5-turbo-16k")

				.maxTokens(700).temperature((double) 0.5f).build();
		String summary_result = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage()
				.getContent();

		response.put("isSuccess", "true");
		response.put("finalFileSize", Long.toString(file_size) + " bytes");
		response.put("summary_result", summary_result);
		logger.debug(summary_result);
		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;
		response.put("executionTimeInMilli", Long.toString(executionTime));
		logger.info("Execution time:"+executionTime);

		ObjectMapper objectMapper = new ObjectMapper();
		String jsonResponse = objectMapper.writeValueAsString(response);

		return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);
	}
}
