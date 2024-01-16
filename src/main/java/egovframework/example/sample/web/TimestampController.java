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
         logger.debug("projectPath: " + projectPath);
         if(lang!=null) {
            logger.debug("lang: " + lang);
         }

         /* OS detection */
         String osName = System.getProperty("os.name");
         String ffmpeg_address = "";
         String ffmpeg_dir_addr = "";
         String whisper_addr = "";
         String srt_dir_address ="";
         
         if (osName.toLowerCase().contains("windows")) {
            logger.debug("OS detection: Windows OS");
            ffmpeg_address = projectPath + "resources\\win\\ffmpeg.exe";
            ffmpeg_dir_addr =projectPath + "resources\\win";
            whisper_addr = projectPath + "resources\\win\\whisper\\bin\\whisper.exe";
            srt_dir_address = projectPath + "resources\\temp";
            
         } else if (osName.toLowerCase().contains("mac")) { 
            logger.debug("OS detection: Mac OS");
            ffmpeg_address = projectPath + "resources/mac/ffmpeg";
            ffmpeg_dir_addr =projectPath + "resources/mac";
            whisper_addr = projectPath + "resources/mac/whisper/bin/whisper";
            srt_dir_address = projectPath + "resources/temp";
         } else {
            logger.debug("OS detection: Unknown OS");
         }
         
         logger.debug("whisper_addr: " + whisper_addr);

         File ffmpeg_file = new File(ffmpeg_address);
         assert ffmpeg_file.exists() : "파일이 존재하지 않습니다.";
         logger.debug("location of ffmpeg: " + ffmpeg_address);
         
         long file_size = file.getSize();//들어온 파
         response.put("initialFileSize: ", Long.toString(file_size)+" bytes");
         logger.debug("Size of the file: " + file_size + " bytes");
         response.put("isAudioExtracted", "false");

         if (file_size > 26214400) {
            response.put("isAudioExtracted", "true");
         }
         logger.debug("Project Path: " + projectPath);

         byte[] bytes = file.getBytes();
         Path directoryPath = Paths.get(projectPath);

         // 디렉토리가 존재하지 않으면 생성
         if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
         }
         String nameWithoutExtension = "";
         String filename = file.getOriginalFilename();
         int lastIndex = filename.lastIndexOf(".");
         if (lastIndex >= 0) {
            nameWithoutExtension = filename.substring(0, lastIndex);
            logger.debug("Filename without extension: " + nameWithoutExtension);
         } else {
            logger.debug("File doesn't have an extension.");
         }
         logger.debug("File name: " + nameWithoutExtension);
         Path filePath = directoryPath.resolve(file.getOriginalFilename());
         Files.write(filePath, bytes);
         Path absolutePath = filePath.toAbsolutePath();
         absolutePathString = absolutePath.toString();
         String origin_absolutePathString = new String(absolutePathString);
         logger.debug("AbsolutePathString received" + absolutePathString);
         File extractedAudio = null;
         Path extractedAbsolutePath = directoryPath.toAbsolutePath();
         String extractedAbsolutePathString = new String(absolutePathString);
         /* ffmpeg */
         if (file_size > 26214400) {
            if (osName.toLowerCase().contains("windows")) {
               extractedAbsolutePathString = extractedAbsolutePath.toString() + "\\" + nameWithoutExtension + ".mp3";

            } else {
               extractedAbsolutePathString = extractedAbsolutePath.toString() + "/" + nameWithoutExtension + ".mp3";

            }

            String ffmpegCommand = ffmpeg_address + " -i " + absolutePathString + " -vn -acodec libmp3lame "
                  + extractedAbsolutePathString;
            logger.debug("ffmpegCommand: " + ffmpegCommand);
            //String whisperCommand = whisper_addr +" "+absolutePathString;
            //logger.debug("whisperCommand: " + whisperCommand);
            
            
            ProcessBuilder processBuilder = new ProcessBuilder();

            if (osName.toLowerCase().contains("windows")) {
               String[] cmdArray = ffmpegCommand.split(" ");
               processBuilder.command(cmdArray);
            } else if (osName.toLowerCase().contains("mac")) {
               processBuilder.command("bash", "-c", ffmpegCommand);
               
            }

            logger.debug("processBuilder.start()");
            Process process = processBuilder.start();

            Thread outputThread = new Thread(() -> {
               try {
                  InputStream inputStream = process.getInputStream();
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

            Thread errorThread = new Thread(() -> {
               try {
                  InputStream errorStream = process.getErrorStream();
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

            outputThread.start();
            errorThread.start();

            int exitCode;
            try {
               exitCode = process.waitFor();
            } catch (InterruptedException e) {
               e.printStackTrace();
               exitCode = -1;
            }

            outputThread.join();
            errorThread.join();
            logger.debug("Extract process exited with code: " + exitCode);

            extractedAudio = new File(extractedAbsolutePathString);

            absolutePathString = extractedAbsolutePathString;//삭제할 파
            logger.debug("Extracted audio file size: " + extractedAudio.length() + " bytes");
            file_size = extractedAudio.length();
            if (extractedAudio.length() > 26214400) {
               return new ResponseEntity<>("오디오만 추출했음에도 파일의 크기가 26214400bytes를 초과합니다. 파일을 분할하여 주세요.", headers,
                     HttpStatus.OK);
            }
         }
         
           if (osName.toLowerCase().contains("mac")) {
              //python 위치
               ProcessBuilder pyProcessBuilder = new ProcessBuilder();
               pyProcessBuilder.command("bash", "-c", "which python3");
               String replaceTargetString;
               if(locOfPython==null) {
                  Process process = pyProcessBuilder.start();
                  logger.debug("pyProcessBuilder.start()");
   
                  StringBuilder python3_loc = new StringBuilder();
   
                  Thread outputThread = new Thread(() -> {
                     try {
                        InputStream inputStream = process.getInputStream();
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
   
                        String python3_loc_line;
                        python3_loc_line = bufferedReader.readLine();
                        python3_loc.append(python3_loc_line);
   
                     } catch (IOException e) {
                        e.printStackTrace();
                     }
                  });
   
                  outputThread.start();
   
                  int exitCode;
                  try {
                     exitCode = process.waitFor();
                  } catch (InterruptedException e) {
                     e.printStackTrace();
                     exitCode = -1;
                  }
   
                  outputThread.join();
                  logger.debug("python3_loc:" + python3_loc);
                  logger.debug("Extract process exited with code: " + exitCode);
                  replaceTargetString = "#!" + python3_loc;
               }else {
                  replaceTargetString = "#!" +locOfPython;
               }
   
               
               StringBuilder content = new StringBuilder();
   
               try (BufferedReader reader = new BufferedReader(new FileReader(whisper_addr))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                     content.append(line).append("\n");
                  }
                  logger.debug("Origin whisper content:\n" + content);
                  if (content.length() > 0) {
                     String firstLine = content.toString().split("\n")[0];
                     content.replace(0, firstLine.length(), replaceTargetString);
                  }
                  logger.debug("Modified whisper content:\n" + content);
                  
                  // 변경된 내용을 다시 파일에 쓰기
                  try (PrintWriter writer = new PrintWriter(new FileWriter(whisper_addr))) {
                     writer.print(content);
                  } catch (IOException e) {
                     logger.error("Error writing to the 'whisper' file.", e);
                  }
               } catch (IOException e) {
                  logger.error("No whisper", e);
            }
           }
            
         String whisperCommand ="";
         
         
         if (osName.toLowerCase().contains("windows")) {//win
            if(lang==null) {
               String whispEnVarDir =projectPath+"resources\\win\\whisper\\";
               String ffmpegEnVarDir =projectPath+"resources\\win\\ffmpeg-master-latest-win64-gpl\\bin\\";

               whisperCommand = "powershell.exe -Command \"Set-Item -Path Env:PYTHONPATH -Value '"+whispEnVarDir+"'; [Environment]::SetEnvironmentVariable('Path', \\\"$([System.Environment]::GetEnvironmentVariable('Path', [System.EnvironmentVariableTarget]::Process));"+ffmpegEnVarDir+"\", [System.EnvironmentVariableTarget]::Process); & '"+whisper_addr+"' --output_dir '"+srt_dir_address+"' --output_format srt --model tiny '"+absolutePathString+"'\"";
         
            }
            else {
               String whispEnVarDir =projectPath+"resources\\win\\whisper\\";
               String ffmpegEnVarDir =projectPath+"resources\\win\\ffmpeg-master-latest-win64-gpl\\bin\\";

               whisperCommand = "powershell.exe -Command \"Set-Item -Path Env:PYTHONPATH -Value '"+whispEnVarDir+"'; [Environment]::SetEnvironmentVariable('Path', \\\"$([System.Environment]::GetEnvironmentVariable('Path', [System.EnvironmentVariableTarget]::Process));"+ffmpegEnVarDir+"\", [System.EnvironmentVariableTarget]::Process); & '"+whisper_addr+"' --output_dir '"+srt_dir_address+"' --output_format srt --language "+lang+" --model tiny '"+absolutePathString+"'\"";
         
            }
                         
         }
         else {//mac
            String whispEnVarDir =projectPath+"resources/mac/whisper/bin";

            if(lang==null) {
                  
                  whisperCommand = 
                        "export PATH="+
                              ffmpeg_dir_addr+
                              ":"+
                              whispEnVarDir+
                              ":$PATH;"+
                              whisper_addr+
                              " "+
                              "--output_dir "+
                              srt_dir_address+
                              " "+
                              "--output_format srt "+
                              "--model tiny "+
                              absolutePathString;
               }
               else {
                  whisperCommand = 
                        "export PATH="+
                              ffmpeg_dir_addr+
                              ":"+
                              whispEnVarDir+
                              ":$PATH;"+
                              whisper_addr+
                              " "+
                              "--output_dir "+
                              srt_dir_address+
                              " "+
                              "--output_format srt "+
                              "--language "+
                              lang+" "+
                              "--model tiny "+
                              absolutePathString;
               }
         }
         
      
         logger.debug("whisperCommand: " + whisperCommand);
         
         
         ProcessBuilder whisperProcessBuilder = new ProcessBuilder();
         
         
         if (osName.toLowerCase().contains("windows")) {
           whisperProcessBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            String[] cmdArray = whisperCommand.split(" ");
            whisperProcessBuilder.command(cmdArray);
         } else if (osName.toLowerCase().contains("mac")) {

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
         String srt_address = srt_dir_address+"/"+nameWithoutExtension+".srt";
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
         message.add(new ChatMessage("user",
               "다음은 srt내용이야. "+searchfor+"가 시작되는 timestamp를 반환해줘. " + srt_content));

         ChatCompletionRequest completionRequest = ChatCompletionRequest.builder().messages(message)
               .model("gpt-3.5-turbo-16k")

               .maxTokens(700).temperature((double) 0.5f).build();
               String summary_result = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage()
               .getContent();

         response.put("isSuccess", "true");
         response.put("finalFileSize", Long.toString(file_size) + " bytes");
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