package egovframework.example.sample.web;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PythonModifier {
	private static final Logger logger = LogManager.getLogger(EgovSampleController.class);
	private OSDetect osd;
	private String locOfPython;
	private String osName;
	private String whisperCommand;
	private String lang;
	
	public String getWhisperCommand() {
		return whisperCommand;
	}

	public void setWhisperCommand(String whisperCommand) {
		this.whisperCommand = whisperCommand;
	}

	PythonModifier(OSDetect osd, String locOfPython, String lang, String absolutePathString) throws IOException, InterruptedException{
		this.osd = osd;
		this.locOfPython = locOfPython;
		this.lang = lang;
		whisperCommand = "";
		osName = osd.getOsName().toLowerCase();
		if(osName.contains("mac")) {
			macSetPythonPath(absolutePathString);
		}
		if(osName.contains("windows")) {
			windowSetPythonPath(absolutePathString);
		}
	}

	void windowSetPythonPath(String absolutePathString) {
		//python 위치 찾기 
		StringBuilder pythonPath = new StringBuilder();
		try {
			// PowerShell 명령어
            String powerShellCommand = "Get-Command python | Select-Object -ExpandProperty Source";
            
            // PowerShell 프로세스 실행
            ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-Command", powerShellCommand);
            Process process = processBuilder.start();
            
            Thread outputThread = new Thread(() -> {
                try {
                   InputStream inputStream = process.getInputStream();
                   InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                   BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                   
                   
                   String line;
                   while ((line = bufferedReader.readLine()) != null) {
                      System.out.println(line);
                      pythonPath.append(line.trim());
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

            // 프로세스 종료 대기
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

            // Python 경로 출력
            logger.debug("Python 위치: " + pythonPath.toString());
            logger.debug("Finding python location process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
       if(lang==null) {
          String whispEnVarDir =osd.getProjectPath()+"resources\\win\\whisper\\";
          String ffmpegEnVarDir =osd.getProjectPath()+"resources\\win\\ffmpeg-master-latest-win64-gpl\\bin\\";

          whisperCommand = "powershell.exe -Command \"Set-Item -Path Env:PYTHONPATH -Value '"+whispEnVarDir+"'; [Environment]::SetEnvironmentVariable('Path', \\\"$([System.Environment]::GetEnvironmentVariable('Path', [System.EnvironmentVariableTarget]::Process));"+ffmpegEnVarDir+"\", [System.EnvironmentVariableTarget]::Process); & '"+pythonPath+"' '"+osd.getWhisper_addr()+"' --output_dir '"+osd.getSrt_dir_address()+"' --output_format srt --model tiny '"+absolutePathString+"'\"";
    
       }
       else {
          String whispEnVarDir =osd.getProjectPath()+"resources\\win\\whisper\\";
          String ffmpegEnVarDir =osd.getProjectPath()+"resources\\win\\ffmpeg-master-latest-win64-gpl\\bin\\";

          whisperCommand = "powershell.exe -Command \"Set-Item -Path Env:PYTHONPATH -Value '"+whispEnVarDir+"'; [Environment]::SetEnvironmentVariable('Path', \\\"$([System.Environment]::GetEnvironmentVariable('Path', [System.EnvironmentVariableTarget]::Process));"+ffmpegEnVarDir+"\", [System.EnvironmentVariableTarget]::Process); & '"+pythonPath+"' '"+osd.getWhisper_addr()+"' --output_dir '"+osd.getSrt_dir_address()+"' --output_format srt --language "+lang+" --model tiny '"+absolutePathString+"'\"";
       }
	}
	
	void macSetPythonPath(String absolutePathString) throws IOException, InterruptedException {	 
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
	}
    else {
		replaceTargetString = "#!" + locOfPython;
	}
        
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(osd.getWhisper_addr()))) {
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
           try (PrintWriter writer = new PrintWriter(new FileWriter(osd.getWhisper_addr()))) {
              writer.print(content);
           } catch (IOException e) {
              logger.error("Error writing to the 'whisper' file.", e);
           }
        } catch (IOException e) {
           logger.error("No whisper", e);
        }
        
        String whispEnVarDir = osd.getProjectPath() + "resources/mac/whisper/bin";

        if(lang==null) {
              
              whisperCommand = 
                    "export PATH="+
                    	  "\""+
                          osd.getFfmpeg_dir_addr()+
                          "\""+
                          ":"+
                          "\""+
                          whispEnVarDir+
                          "\""+
                          ":$PATH;"+
                          "\""+
                          osd.getWhisper_addr()+
                          "\""+
                          " "+
                          "--output_dir "+
                          "\""+
                          osd.getSrt_dir_address()+
                          "\""+
                          " "+
                          "--output_format srt "+
                          "--model tiny "+
                          "\""+
                          absolutePathString+
                          "\"";
           }
           else {
              whisperCommand = 
                    "export PATH="+
                    	  "\""+
                    	  osd.getFfmpeg_dir_addr()+
                    	  "\""+
                          ":"+
                          "\""+
                          whispEnVarDir+
                          "\""+
                          ":$PATH;"+
                          "\""+
                          osd.getWhisper_addr()+
                          "\""+
                          " "+
                          "--output_dir "+
                          "\""+
                          osd.getSrt_dir_address()+
                          "\""+
                          " "+
                          "--output_format srt "+
                          "--language "+
                          lang+" "+
                          "--model tiny "+
                          "\""+
                          absolutePathString+
                          "\"";
           }
	}
}
