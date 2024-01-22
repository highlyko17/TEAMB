package egovframework.example.sample.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
public class TimestampController {
   private static final Logger logger = LogManager.getLogger(EgovSampleController.class);
      @PostMapping("/timestamp.do")
      @ResponseBody
      public ResponseEntity<?> extractTimestamp(
            @RequestParam MultipartFile file, 
            @RequestParam("searchfor") String searchfor, 
            @RequestParam(name = "lang", required = false) String lang,
            @RequestParam(name = "locOfPython", required = false) String locOfPython,
            HttpServletRequest request)
            throws IOException, InterruptedException {
         OpenAiService service = new OpenAiService(Keys.OPENAPI_KEY, Duration.ofMinutes(9999));
         long startTime = System.currentTimeMillis();
         HttpHeaders headers = new HttpHeaders();
         headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
         Map<String, String> response = new HashMap<>();// 결과를 맵핑할 변

         ServletContext context = request.getSession().getServletContext();
         String projectPath = context.getRealPath("/");
         String absolutePathString = "";
         logger.debug("searchfor: " + searchfor);
         logger.debug("locOfPython: " + locOfPython);
         logger.debug("projectPath: " + projectPath);
         if(lang!=null) {
            logger.debug("lang: " + lang);
         }

         /* OS detection */
         OSDetect osd = new OSDetect(projectPath);
         osd.whisperDetection();
         
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
         extractedAudio = fc.runFfmpeg(extractedAudio);
         if (extractedAudio != null && extractedAudio.length() > 26214400) {
        	 return new ResponseEntity<>("오디오만 추출했음에도 파일의 크기가 26214400bytes를 초과합니다. 파일을 분할하여 주세요.", headers,
					HttpStatus.OK);
         }
         
         
         PythonModifier pc = new PythonModifier(osd, locOfPython, lang, absolutePathString);
         String whisperCommand = pc.getWhisperCommand();
         logger.debug("whisperCommand: " + whisperCommand);
         
         
         ProcessBuilder whisperProcessBuilder = new ProcessBuilder();
         
         
         if (osd.getOsName().toLowerCase().contains("windows")) {
           whisperProcessBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            String[] cmdArray = whisperCommand.split(" ");
            whisperProcessBuilder.command(cmdArray);
         } else if (osd.getOsName().toLowerCase().contains("mac")) {
            whisperProcessBuilder.command("bash", "-c", whisperCommand);
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
         
         String srt_address;
         if (osd.getOsName().toLowerCase().contains("windows")) {
        	 srt_address = osd.getSrt_dir_address()+"\\"+fc.getNameWithoutExtension()+".srt";
         }
         else {
        	 srt_address = osd.getSrt_dir_address()+"/"+fc.getNameWithoutExtension()+".srt";
         }
         logger.debug("srt file address: " + srt_address);
         Path srt_path = Paths.get(srt_address);// 삭제할 파
           byte[] srt_fileBytes = Files.readAllBytes(srt_path);
           String srt_content = new String(srt_fileBytes);
           logger.debug("srt_content:\n " + srt_content);
         //file_size = extractedAudio.length();
         
         
         long endTime = System.currentTimeMillis();
         long executionTime = endTime - startTime;
         response.put("executionTimeInMilli", Long.toString(executionTime));
         logger.info("Execution time:"+executionTime);
         response.put("srt_conent", srt_content);
         
         
         List<ChatMessage> message = new ArrayList<ChatMessage>();
//         message.add(new ChatMessage("user",
//               "다음은 srt내용이야. "+searchfor+"가 시작되는 timestamp를 반환해줘. " + srt_content));
         message.add(new ChatMessage("user",
                 "These are the content of srt file which is extraced from an audio file." + srt_content + "From the srt content, find timestamp related to " + searchfor + " and return the timestamp."));


         ChatCompletionRequest completionRequest = ChatCompletionRequest.builder().messages(message)
               .model("gpt-3.5-turbo-16k")

               .maxTokens(700).temperature((double) 0.5f).build();
               String summary_result = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage()
               .getContent();

         response.put("isSuccess", "true");
         response.put("finalFileSize", Long.toString(fc.getFile_size()) + " bytes");
         response.put("summary_result", summary_result);
         logger.debug(summary_result);
         

         ObjectMapper objectMapper = new ObjectMapper();
         String jsonResponse = objectMapper.writeValueAsString(response);

         File fileToDelete = new File(origin_absolutePathString);//사용자에게 받은 원본 파일

         if (fileToDelete.exists()) {
            if (fileToDelete.delete()) {
               logger.debug("origin video file deleted successfully.");
            } else {
               logger.debug("Failed to delete the origin video file.");
            }
         } else {
            logger.debug("temporary file not found.");
         }
         
         if (extractedAudio != null) {//mp3 전환 
            extractedAudio.delete();
            logger.debug("Extracted audio file deleted successfully.");
         }
         File srtToDelete = new File(srt_address);
         if (srtToDelete.exists()) {
            if (srtToDelete.delete()) {
               logger.debug("srtToDelete deleted successfully.");
            } else {
               logger.debug("Failed to delete the srtToDelete.");
            }
         } else {
            logger.debug("srtToDelete file not found.");
         }
         
         return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);
      }
}