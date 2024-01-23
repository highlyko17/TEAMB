package egovframework.example.sample.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//OS 종류 판별
public class OSDetect {
	private static final Logger logger = LogManager.getLogger(EgovSampleController.class);
	private String osName;
	private String ffmpeg_address;
	private String ffmpeg_dir_addr;
	private String whisper_addr;
	private String projectPath;
	private String srt_dir_address;
	private String resource_address;
	private String srt_address;
	
	public String getSrt_address() {
		return srt_address;
	}

	public void setSrt_address(String srt_address) {
		this.srt_address = srt_address;
	}
	
	public String getSrt_dir_address() {
		return srt_dir_address;
	}

	public void setSrt_dir_address(String srt_dir_address) {
		this.srt_dir_address = srt_dir_address;
	}
	
	public String getProjectPath() {
		return projectPath;
	}

	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public String getFfmpeg_address() {
		return ffmpeg_address;
	}

	public void setFfmpeg_address(String ffmpeg_address) {
		this.ffmpeg_address = ffmpeg_address;
	}

	public String getFfmpeg_dir_addr() {
		return ffmpeg_dir_addr;
	}

	public void setFfmpeg_dir_addr(String ffmpeg_dir_addr) {
		this.ffmpeg_dir_addr = ffmpeg_dir_addr;
	}

	public String getWhisper_addr() {
		return whisper_addr;
	}

	public void setWhisper_addr(String whisper_addr) {
		this.whisper_addr = whisper_addr;
	}
	
	public String getResource_address() {
		return resource_address;
	}

	public void setResource_address(String resource_address) {
		this.resource_address = resource_address;
	}
	
	public OSDetect(String projectPath){
		this.osName = System.getProperty("os.name");
		ffmpeg_address = "";
		ffmpeg_dir_addr = "";
		whisper_addr = "";
		srt_dir_address = "";
		resource_address ="";
		this.projectPath = projectPath;
	}

	public void detection() {
		if (osName.toLowerCase().contains("windows")) {
			logger.debug("OS detection: Windows OS");
			ffmpeg_address = projectPath + "resources\\win\\ffmpeg.exe";
			ffmpeg_dir_addr =projectPath + "resources\\win";
			resource_address = projectPath + "resources";
		} else if (osName.toLowerCase().contains("mac")) {
			logger.debug("OS detection: Mac OS");
			ffmpeg_address = projectPath + "resources/mac/ffmpeg";
			ffmpeg_dir_addr =projectPath + "resources/mac";
			whisper_addr = projectPath + "resources/mac/whisper/bin/whisper";
			resource_address = projectPath + "resources";
		} else {
			logger.debug("OS detection: Unknown OS");
		}
		
		logger.debug("whisper_addr: " + whisper_addr);
	}
	
	public void whisperDetection() {
		if (osName.toLowerCase().contains("windows")) {
            logger.debug("OS detection: Windows OS");
            ffmpeg_address = projectPath + "resources\\win\\ffmpeg.exe";
            ffmpeg_dir_addr =projectPath + "resources\\win";
            whisper_addr = projectPath + "resources\\win\\whisper\\bin\\whisper.exe";
            srt_dir_address = projectPath + "resources\\temp";
            resource_address = projectPath + "resources";
            
         } else if (osName.toLowerCase().contains("mac")) { 
            logger.debug("OS detection: Mac OS");
            ffmpeg_address = projectPath + "resources/mac/ffmpeg";
            ffmpeg_dir_addr =projectPath + "resources/mac";
            whisper_addr = projectPath + "resources/mac/whisper/bin/whisper";
            srt_dir_address = projectPath + "resources/temp";
            resource_address = projectPath + "resources";
         } else {
            logger.debug("OS detection: Unknown OS");
         }
		
		logger.debug("whisper_addr: " + whisper_addr);
	}
	
	public String makeSrt(OSDetect osd, FileController fc) throws IOException {
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
        
        return srt_content;
	}
}
