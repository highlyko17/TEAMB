package egovframework.example.sample.web;

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
	
	public OSDetect(String projectPath){
		this.osName = System.getProperty("os.name");
		ffmpeg_address = "";
		ffmpeg_dir_addr = "";
		whisper_addr = "";
		this.projectPath = projectPath;
		detection();
	}
	
	public void detection() {
		if (osName.toLowerCase().contains("windows")) {
			logger.debug("OS detection: Windows OS");
			ffmpeg_address = projectPath + "resources\\win\\ffmpeg.exe";
			ffmpeg_dir_addr =projectPath + "resources\\win";

		} else if (osName.toLowerCase().contains("mac")) {
			logger.debug("OS detection: Mac OS");
			ffmpeg_address = projectPath + "resources/mac/ffmpeg";
			ffmpeg_dir_addr =projectPath + "resources/mac";
			whisper_addr = projectPath + "resources/mac/whisper/bin/whisper";
		} else {
			logger.debug("OS detection: Unknown OS");
		}
		
		logger.debug("whisper_addr: " + whisper_addr);
	}
}
