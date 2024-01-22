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
	
	@PostMapping("/extract-tag.do")
	@ResponseBody
	public ResponseEntity<?> extractTagsUsingWhisper(@RequestParam MultipartFile file, HttpServletRequest request)
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
		osd.detection();
		
		Path resource_path = Paths.get(osd.getResource_address());
        if (!Files.exists(resource_path)) {
            logger.error("resource 폴더가 존재하지 않습니다. resource 폴더를 다운받아 주세요.");
            logger.error("resource 폴더를 둘 곳: "+ osd.getResource_address());
            
            response.put("Install the 'resource' folder at the following address: ", osd.getResource_address());
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(response);
            return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);
        }

		FileController fc = new FileController(response, file, osd);
		fc.exist();
		response = fc.sizing();
		
		logger.debug("Project Path: " + projectPath);
		
		absolutePathString = fc.setAbsolutePath();
		String origin_absolutePathString = new String(absolutePathString);
		
		logger.debug("AbsolutePathString received" + absolutePathString);
		
		File extractedAudio = null;
		/* ffmpeg */
		extractedAudio = fc.runFfmpeg(extractedAudio);
		if (extractedAudio != null && extractedAudio.length() > 26214400) {
			return new ResponseEntity<>("오디오만 추출했음에도 파일의 크기가 26214400bytes를 초과합니다. 파일을 분할하여 주세요.", headers,
					HttpStatus.OK);
		}

		// 받아온 주소를 whisper에게 보내
		WhisperController wc = new WhisperController();
		if(extractedAudio!=null) {
			String extractedAudiFilePath = extractedAudio.getAbsolutePath();
			absolutePathString = extractedAudiFilePath;
		}
		wc.transcript(absolutePathString);

		fc.deleteFile(origin_absolutePathString);
		
		String str = "다음 텍스트의 주요한 태그를 중요한 순서대로 5개를 쉼표를 구분자로 사용해서 추출해줘.: \"";
		String summary_result = wc.getResult(str);

		Result rslt = new Result();
		
		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;
		response = rslt.getResult(response, fc.getFile_size(), summary_result, executionTime);
		logger.debug(summary_result);
		logger.info("Execution time:"+executionTime);

		ObjectMapper objectMapper = new ObjectMapper();
		String jsonResponse = objectMapper.writeValueAsString(response);

		return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);
	}
}
