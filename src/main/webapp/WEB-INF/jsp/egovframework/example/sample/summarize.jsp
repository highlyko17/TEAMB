<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>summarize</title>
</head>
<body>
<fieldset>
	<legend>요약 결과</legend>
	
	<form action="save-result.do" method="post" enctype="text">
    
        
        <br>
        <textarea id="summarize" name="summ_result" rows="25" cols="100">${summary_result}</textarea>
        <input type="dir" id="folderInput" webkitdirectory directory multiple>
        <p><input type="submit" value="선택한 폴더에 저장">&nbsp<input type="reset" value="취소"></p>
       
    
	</form>
</fieldset>
</body>
</html>