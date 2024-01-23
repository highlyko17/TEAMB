package egovframework.example.sample.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public class FileController {
	private static final Logger logger = LogManager.getLogger(EgovSampleController.class);
	private File ffmpeg_file;
	private long file_size;
	private Map<String, String> response;
	private MultipartFile file;
	private Path directoryPath;
	private String absolutePathString;
	private OSDetect osd;
	private String nameWithoutExtension;
	private File extractedAudio;
	
	public String getAbsolutePathString() {
		return absolutePathString;
	}

	public void setAbsolutePathString(String absolutePathString) {
		this.absolutePathString = absolutePathString;
	}

	public String getNameWithoutExtension() {
		return nameWithoutExtension;
	}

	public void setNameWithoutExtension(String nameWithoutExtension) {
		this.nameWithoutExtension = nameWithoutExtension;
	}
	
	public Path getDirectoryPath() {
		return directoryPath;
	}

	public void setDirectoryPath(Path directoryPath) {
		this.directoryPath = directoryPath;
	}

	public File getFfmpeg_file() {
		return ffmpeg_file;
	}

	public void setFfmpeg_file(File ffmpeg_file) {
		this.ffmpeg_file = ffmpeg_file;
	}

	public long getFile_size() {
		return file_size;
	}

	public void setFile_size(long file_size) {
		this.file_size = file_size;
	}

	public Map<String, String> getResponse() {
		return response;
	}

	public void setResponse(Map<String, String> response) {
		this.response = response;
	}

	public MultipartFile getFile() {
		return file;
	}

	public void setFile(MultipartFile file) {
		this.file = file;
	}

	public FileController(Map<String, String> response, MultipartFile file, OSDetect osd) {
		ffmpeg_file = new File(osd.getFfmpeg_address());
		this.response = response;
		this.file = file;
		this.file_size = this.file.getSize();
		this.osd = osd;
		nameWithoutExtension = "";
	}

	public void exist() {
		assert ffmpeg_file.exists() : "파일이 존재하지 않습니다.";
		logger.debug("location of ffmpeg: " + osd.getFfmpeg_address());
	}
	
	public Map<String, String> sizing() {
		response.put("initialFileSize: ", Long.toString(file_size)+" bytes");
		logger.debug("Size of the file: " + file_size + " bytes");
		response.put("isAudioExtracted", "false");

		if (file_size > 26214400) {
			response.put("isAudioExtracted", "true");
		}
		
		return response;
	}
	
	public String setAbsolutePath() throws IOException {
		byte[] bytes = file.getBytes();
		directoryPath = Paths.get(osd.getProjectPath());
		
		// 디렉토리가 존재하지 않으면 생성
		if (!Files.exists(directoryPath)) {
			Files.createDirectories(directoryPath);
		}
		String filename = file.getOriginalFilename();
		int lastIndex = filename.lastIndexOf(".");
		if (lastIndex >= 0) {
			// 확장자를 포함하여 파일 이름을 가져옵니다.
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
		
		return absolutePathString;
	}

	
	public File runFfmpeg(File ea) throws InterruptedException, IOException {
		extractedAudio = ea;
		
		if (file_size > 26214400) {
			Path extractedAbsolutePath = directoryPath.toAbsolutePath();
			String extractedAbsolutePathString = "";
			if (osd.getFfmpeg_address().toLowerCase().contains("windows")) {
				extractedAbsolutePathString = 
						extractedAbsolutePath.toString() + "\\" + nameWithoutExtension + ".mp3";

			} else {
				extractedAbsolutePathString = extractedAbsolutePath.toString() + "/" + nameWithoutExtension + ".mp3";

			}

			String ffmpegCommand = "\""+osd.getFfmpeg_address() + "\""+" -i " + "\""+absolutePathString +"\""+ " -vn -acodec libmp3lame "
					+ "\""+extractedAbsolutePathString+"\"";
			logger.debug("ffmpegCommand: " + ffmpegCommand);
			
			
			
			ProcessBuilder processBuilder = new ProcessBuilder();

			if (osd.getOsName().toLowerCase().contains("windows")) {
				String[] cmdArray = ffmpegCommand.split(" ");
				processBuilder.command(cmdArray);
			} else if (osd.getOsName().toLowerCase().contains("mac")) {
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
			file_size = extractedAudio.length();

			absolutePathString = extractedAbsolutePathString;
			logger.debug("Extracted audio file size: " + extractedAudio.length() + " bytes");
		}
		
		return extractedAudio;
	}
	
	public void deleteFile(String origin_absolutePathString) {
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
	}
	
	public void deleteSrtFile(String srt_address) {
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
	}
}
