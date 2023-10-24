/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package egovframework.example.sample.web;

import java.awt.Choice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import egovframework.example.API.Keys;
import egovframework.example.sample.service.EgovSampleService;
import egovframework.example.sample.service.SampleDefaultVO;
import egovframework.example.sample.service.SampleVO;

import egovframework.rte.fdl.property.EgovPropertyService;
import egovframework.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springmodules.validation.commons.DefaultBeanValidator;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * @Class Name : EgovSampleController.java
 * @Description : EgovSample Controller Class
 * @Modification Information
 * @
 * @  수정일      수정자              수정내용
 * @ ---------   ---------   -------------------------------
 * @ 2009.03.16           최초생성
 *
 * @author 개발프레임웍크 실행환경 개발팀
 * @since 2009. 03.16
 * @version 1.0
 * @see
 *
 *  Copyright (C) by MOPAS All right reserved.
 */

@Controller
@MultipartConfig(
	    maxFileSize = 1024 * 1024 * 25, // 최대 25MB 파일 크기
	    maxRequestSize = 1024 * 1024 * 25, // 최대 25MB 요청 크기
	    fileSizeThreshold = 1024 * 1024 // 1MB 이상부터 디스크에 저장
	)
public class EgovSampleController {
	private static final Logger logger = LogManager.getLogger(EgovSampleController.class);
	private final String UPLOAD_DIR = "uploads";
	/** EgovSampleService */
	@Resource(name = "sampleService")
	private EgovSampleService sampleService;

	/** EgovPropertyService */
	@Resource(name = "propertiesService")
	protected EgovPropertyService propertiesService;

	/** Validator */
	@Resource(name = "beanValidator")
	protected DefaultBeanValidator beanValidator;

	/**
	 * 글 목록을 조회한다. (pageing)
	 * @param searchVO - 조회할 정보가 담긴 SampleDefaultVO
	 * @param model
	 * @return "egovSampleList"
	 * @exception Exception
	 */
	@RequestMapping(value = "/egovSampleList.do")
	public String selectSampleList(@ModelAttribute("searchVO") SampleDefaultVO searchVO, ModelMap model) throws Exception {

		/** EgovPropertyService.sample */
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		/** pageing setting */
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
		paginationInfo.setRecordCountPerPage(searchVO.getPageUnit());
		paginationInfo.setPageSize(searchVO.getPageSize());

		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		List<?> sampleList = sampleService.selectSampleList(searchVO);
		model.addAttribute("resultList", sampleList);

		int totCnt = sampleService.selectSampleListTotCnt(searchVO);
		paginationInfo.setTotalRecordCount(totCnt);
		model.addAttribute("paginationInfo", paginationInfo);

		return "sample/egovSampleList";
	}

	/**
	 * 글 등록 화면을 조회한다.
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param model
	 * @return "egovSampleRegister"
	 * @exception Exception
	 */
	@RequestMapping(value = "/addSample.do", method = RequestMethod.GET)
	public String addSampleView(@ModelAttribute("searchVO") SampleDefaultVO searchVO, Model model) throws Exception {
		model.addAttribute("sampleVO", new SampleVO());
		return "sample/egovSampleRegister";
	}

	/**
	 * 글을 등록한다.
	 * @param sampleVO - 등록할 정보가 담긴 VO
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param status
	 * @return "forward:/egovSampleList.do"
	 * @exception Exception
	 */
	@RequestMapping(value = "/addSample.do", method = RequestMethod.POST)
	public String addSample(@ModelAttribute("searchVO") SampleDefaultVO searchVO, SampleVO sampleVO, BindingResult bindingResult, Model model, SessionStatus status)
			throws Exception {

		// Server-Side Validation
		beanValidator.validate(sampleVO, bindingResult);

		if (bindingResult.hasErrors()) {
			model.addAttribute("sampleVO", sampleVO);
			return "sample/egovSampleRegister";
		}

		sampleService.insertSample(sampleVO);
		status.setComplete();
		return "forward:/egovSampleList.do";
	}

	/**
	 * 글 수정화면을 조회한다.
	 * @param id - 수정할 글 id
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param model
	 * @return "egovSampleRegister"
	 * @exception Exception
	 */
	@RequestMapping("/updateSampleView.do")
	public String updateSampleView(@RequestParam("selectedId") String id, @ModelAttribute("searchVO") SampleDefaultVO searchVO, Model model) throws Exception {
		SampleVO sampleVO = new SampleVO();
		sampleVO.setId(id);
		// 변수명은 CoC 에 따라 sampleVO
		model.addAttribute(selectSample(sampleVO, searchVO));
		return "sample/egovSampleRegister";
	}

	/**
	 * 글을 조회한다.
	 * @param sampleVO - 조회할 정보가 담긴 VO
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param status
	 * @return @ModelAttribute("sampleVO") - 조회한 정보
	 * @exception Exception
	 */
	public SampleVO selectSample(SampleVO sampleVO, @ModelAttribute("searchVO") SampleDefaultVO searchVO) throws Exception {
		return sampleService.selectSample(sampleVO);
	}

	/**
	 * 글을 수정한다.
	 * @param sampleVO - 수정할 정보가 담긴 VO
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param status
	 * @return "forward:/egovSampleList.do"
	 * @exception Exception
	 */
	@RequestMapping("/updateSample.do")
	public String updateSample(@ModelAttribute("searchVO") SampleDefaultVO searchVO, SampleVO sampleVO, BindingResult bindingResult, Model model, SessionStatus status)
			throws Exception {

		beanValidator.validate(sampleVO, bindingResult);

		if (bindingResult.hasErrors()) {
			model.addAttribute("sampleVO", sampleVO);
			return "sample/egovSampleRegister";
		}

		sampleService.updateSample(sampleVO);
		status.setComplete();
		return "forward:/egovSampleList.do";
	}

	/**
	 * 글을 삭제한다.
	 * @param sampleVO - 삭제할 정보가 담긴 VO
	 * @param searchVO - 목록 조회조건 정보가 담긴 VO
	 * @param status
	 * @return "forward:/egovSampleList.do"
	 * @exception Exception
	 */
	@RequestMapping("/deleteSample.do")
	public String deleteSample(SampleVO sampleVO, @ModelAttribute("searchVO") SampleDefaultVO searchVO, SessionStatus status) throws Exception {
		sampleService.deleteSample(sampleVO);
		status.setComplete();
		return "forward:/egovSampleList.do";
	}
	
	@RequestMapping("/file.do")
	public String fileReg() throws Exception {
		
		return "sample/file";
	}
	
	//static String englishAudioFilePath = "/Users/jiuhyeong/Documents/Handong/capstone1/Dani_california.mp3";
	//static String englishAudioFilePath = "/Users/jiuhyeong/Documents/Handong/capstone1/interview.mp4";
	
	
	//requestparam으로 임시로 저장한 파일의 위치를 string으로 받은 후 whisper에게 전사를 맡김, 임시 파일 삭제?
	@RequestMapping(value = "/file.do", method = RequestMethod.POST)
    public String createTranscription(@RequestParam String absolutePath, Model model) {
		OpenAiService service = new OpenAiService(Keys.OPENAPI_KEY,Duration.ofMinutes(9999));
        CreateTranscriptionRequest createTranscriptionRequest = CreateTranscriptionRequest.builder()
                .model("whisper-1")
                .build();
        

        String text = service.createTranscription(createTranscriptionRequest, absolutePath).getText();
        logger.debug(text);
        model.addAttribute("result", text);
        model.addAttribute("absolutePath", absolutePath);
        
        File fileToDelete = new File(absolutePath);
        
        if (fileToDelete.exists()) {
            if (fileToDelete.delete()) {
            	logger.debug("temp File deleted successfully.");
                
            } else {
            	logger.error("Failed to delete the file.");
                
            }
        } else {
        	logger.debug("temp File not found");
        }
        return "sample/file";
    }

	//jsp에 저장버튼 추가 후 restapi로 보내기
	@RequestMapping(value = "/summarize.do", method = RequestMethod.POST)
    public String showSummaryResult(@RequestParam String transcription_result, Model model) {
        OpenAiService service = new OpenAiService(Keys.OPENAPI_KEY,Duration.ofMinutes(9999));
        List<ChatMessage> message = new ArrayList<ChatMessage>();
    	message.add(new ChatMessage("user", "텍스트의 주제를 파악해서 해당 언어로 다섯줄 내외 요약해줘 \""+transcription_result+"\""));
        
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
        	.messages(message)
            .model("gpt-3.5-turbo") 
            
            .maxTokens(1500)
            .temperature((double) 0.5f)
            .build();
        String summary_restult=service.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent();
        model.addAttribute("summary_result",summary_restult);
        
        return "sample/summarize";
    }
	
	
	//파일을 임시저장 후 file.do에 경로를 보냄.
	@RequestMapping(value = "/postfile.do", method = RequestMethod.POST)
	public String handleFile(@RequestParam(value = "file", required = false) MultipartFile file, Model model, HttpServletRequest request) throws IOException{
		ServletContext context = request.getSession().getServletContext();
        String projectPath = context.getRealPath("/");
        
        System.out.println("Project Path: " + projectPath);
		if (file.isEmpty()) {
            return "redirect:/file.do"; // 파일이 선택되지 않았을 경우 폼으로 리다이렉트
        }

		try {
            byte[] bytes = file.getBytes();
            Path directoryPath = Paths.get(projectPath+UPLOAD_DIR);
            
            // 디렉토리가 존재하지 않으면 생성
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
            
            Path filePath = directoryPath.resolve(file.getOriginalFilename());
            Files.write(filePath, bytes);
            Path absolutePath = filePath.toAbsolutePath();
            String absolutePathString = absolutePath.toString();
            logger.debug("AbsolutePathString received"+absolutePathString);
            model.addAttribute("absolutePath", absolutePathString);
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		model.addAttribute("inputFile", file.getOriginalFilename());
		
		return "sample/file";
	}
	@RequestMapping(value = "/save-result.do", method = RequestMethod.POST)
	public String saveFile(@RequestParam(value = "dir", required = false) MultipartFile dir, @RequestParam String summ_result, Model model, HttpServletRequest request) throws IOException{
		
		return "redirect:/summary.do";
	}
	
	

}
