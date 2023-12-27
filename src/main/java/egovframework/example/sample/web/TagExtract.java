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
public class TagExtract {
	private static final Logger logger = LogManager.getLogger(EgovSampleController.class);
	
//	@PostMapping("/extract-tag.do")
//	@ResponseBody
//	public ResponseEntity<?> extractTagsUsingWhisper(@RequestParam MultipartFile file, HttpServletRequest request)
//			throws IOException, InterruptedException {
//		long startTime = System.currentTimeMillis();
//		HttpHeaders headers = new HttpHeaders();
//		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
//		Map<String, String> response = new HashMap<>();// 결과를 맵핑할 변
//
//		ServletContext context = request.getSession().getServletContext();
//		String projectPath = context.getRealPath("/");
//		String absolutePathString = "";
//		logger.debug("projectPath: " + projectPath);
//
//		/* OS detection */
//		String osName = System.getProperty("os.name");
//		String ffmpeg_address = "";
//		String ffmpeg_dir_addr="";
//		String whisper_addr = "";
//		if (osName.toLowerCase().contains("windows")) {
//			logger.debug("OS detection: Windows OS");
//			ffmpeg_address = projectPath + "resources\\win\\ffmpeg.exe";
//			ffmpeg_dir_addr =projectPath + "resources\\win";
//			//whisper_intall_addr = projectPath +"resources\\whisper";
//
//		} else if (osName.toLowerCase().contains("mac")) {
//			logger.debug("OS detection: Mac OS");
//			ffmpeg_address = projectPath + "resources/mac/ffmpeg";
//			ffmpeg_dir_addr =projectPath + "resources/mac";
//			whisper_addr = projectPath + "resources/mac/whisper/bin/whisper";
//			//whisper_intall_addr = projectPath +"resources/whisper";
//		} else {
//			logger.debug("OS detection: Unknown OS");
//		}
//
//		File ffmpeg_file = new File(ffmpeg_address);
//		assert ffmpeg_file.exists() : "파일이 존재하지 않습니다.";
//		// String ffmpeg_absolutePath = ffmpeg_file.getAbsolutePath();
//		logger.debug("location of ffmpeg: " + ffmpeg_address);
//		long file_size = file.getSize();
//		response.put("initialFileSize: ", Long.toString(file_size)+" bytes");
//		logger.debug("Size of the file: " + file_size + " bytes");
//		response.put("isAudioExtracted", "false");
//
//		if (file_size > 26214400) {
//			response.put("isAudioExtracted", "true");
//			// return new ResponseEntity<>("파일의 크기가 26214400 bytes를 넘습니다. 분할하여 보내주세요 ",
//			// headers, HttpStatus.OK);
//		}
//		logger.debug("Project Path: " + projectPath);
//
//		byte[] bytes = file.getBytes();
//		Path directoryPath = Paths.get(projectPath);
//
//		// 디렉토리가 존재하지 않으면 생성
//		if (!Files.exists(directoryPath)) {
//			Files.createDirectories(directoryPath);
//		}
//		String nameWithoutExtension = "";
//		String filename = file.getOriginalFilename();
//		int lastIndex = filename.lastIndexOf(".");
//		if (lastIndex >= 0) {
//			// 확장자를 포함하여 파일 이름을 가져옵니다.
//			nameWithoutExtension = filename.substring(0, lastIndex);
//			logger.debug("Filename without extension: " + nameWithoutExtension);
//		} else {
//			logger.debug("File doesn't have an extension.");
//		}
//		logger.debug("File name: " + nameWithoutExtension);
//		Path filePath = directoryPath.resolve(file.getOriginalFilename());
//		Files.write(filePath, bytes);
//		Path absolutePath = filePath.toAbsolutePath();
//		absolutePathString = absolutePath.toString();
//		String origin_absolutePathString = new String(absolutePathString);
//		logger.debug("AbsolutePathString received" + absolutePathString);
//		File extractedAudio = null;
//		/* ffmpeg */
//		if (file_size > 26214400) {
//			Path extractedAbsolutePath = directoryPath.toAbsolutePath();
//			String extractedAbsolutePathString = "";
//			if (osName.toLowerCase().contains("windows")) {
//				extractedAbsolutePathString = extractedAbsolutePath.toString() + "\\" + nameWithoutExtension + ".mp3";
//
//			} else {
//				extractedAbsolutePathString = extractedAbsolutePath.toString() + "/" + nameWithoutExtension + ".mp3";
//
//			}
//
//			String ffmpegCommand = ffmpeg_address + " -i " + absolutePathString + " -vn -acodec libmp3lame "
//					+ extractedAbsolutePathString;
//			logger.debug("ffmpegCommand: " + ffmpegCommand);
//			ProcessBuilder processBuilder = new ProcessBuilder();
//
//			if (osName.toLowerCase().contains("windows")) {
//				String[] cmdArray = ffmpegCommand.split(" ");
//				processBuilder.command(cmdArray);
//			} else if (osName.toLowerCase().contains("mac")) {
//				processBuilder.command("bash", "-c", ffmpegCommand);
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
//
//		// 받아온 주소를 whisper에게 보내
//		OpenAiService service = new OpenAiService(Keys.OPENAPI_KEY, Duration.ofMinutes(9999));
//		CreateTranscriptionRequest createTranscriptionRequest = CreateTranscriptionRequest.builder().model("whisper-1")
//				.build();
//
//		String transcription_result = service.createTranscription(createTranscriptionRequest, absolutePathString).getText();
//		logger.debug(transcription_result);
//
//		if (extractedAudio != null) {
//			extractedAudio.delete();
//			logger.debug("Extracted audio file deleted successfully.");
//		}
//		File fileToDelete = new File(origin_absolutePathString);
//
//		if (fileToDelete.exists()) {
//			if (fileToDelete.delete()) {
//				logger.debug("Temporary video file deleted successfully.");
//			} else {
//				logger.debug("Failed to delete the temporary video file.");
//			}
//		} else {
//			logger.debug("temporary file not found.");
//		}
//		List<ChatMessage> message = new ArrayList<ChatMessage>();
//		message.add(new ChatMessage("user",
//				"다음 텍스트의 주요한 태그를 중요한 순서대로 5개를 쉼표를 구분자로 사용해서 추출해줘.: \"" + transcription_result + "\""));
//
//		ChatCompletionRequest completionRequest = ChatCompletionRequest.builder().messages(message)
//				.model("gpt-3.5-turbo-16k")
//
//				.maxTokens(700).temperature((double) 0.5f).build();
//		String summary_result = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage()
//				.getContent();
//
//		response.put("isSuccess", "true");
//		response.put("finalFileSize", Long.toString(file_size) + " bytes");
//		response.put("summary_result", summary_result);
//		logger.debug(summary_result);
//		long endTime = System.currentTimeMillis();
//		long executionTime = endTime - startTime;
//		response.put("executionTimeInMilli", Long.toString(executionTime));
//		logger.info("Execution time:"+executionTime);
//		
//		ObjectMapper objectMapper = new ObjectMapper();
//		String jsonResponse = objectMapper.writeValueAsString(response);
//
//		return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);
//	}
}