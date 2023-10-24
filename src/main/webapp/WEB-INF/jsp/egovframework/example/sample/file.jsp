<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<link rel="stylesheet" href="sample.css">
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <meta charset="UTF-8">
    <title>form</title>
    <script>
        function updateFileInfo() {
            var inputFile = document.querySelector('input[type="file"]');
            var fileInfoTable = document.querySelector('#fileInfoTable');
            var fileTypeCell = fileInfoTable.rows[1].cells[1];
            var fileSizeCell = fileInfoTable.rows[2].cells[1];

            if (inputFile.files.length > 0) {
                var selectedFile = inputFile.files[0];
                var fileName = selectedFile.name;
                var fileExtension = fileName.split('.').pop(); // Get file extension
                var fileSizeMB = (selectedFile.size / (1024 * 1024)).toFixed(2); 

                fileTypeCell.textContent = fileExtension;
                fileSizeCell.textContent = fileSizeMB + " MB";
            } else {
                fileTypeCell.textContent = "";
                fileSizeCell.textContent = "";
            }
        }
    </script>
</head>
<body>

<form action="upload.do" method="post" enctype="multipart/form-data">
    <h3>whisper model</h3>
    <fieldset>
        <legend>파일 업로드</legend>
        <p>파일명 : <input type="file" name="file" onchange="updateFileInfo()"></p>
        <table id="fileInfoTable" border="1">
        <tr>
            <th>&nbsp File &nbsp</th>
            <th>&nbsp Info &nbsp</th>
        </tr>
        <tr>
            <td>&nbsp Type &nbsp</td>
            <td id="fileTypeCell"></td>
        </tr>
        <tr>
            <td>&nbsp Size &nbsp</td>
            <td id="fileSizeCell"></td>
        </tr>
   		</table>
        <p><input type="submit" value="업로드">&nbsp<input type="reset" value="취소"></p>
    </fieldset>
</form>
<br><hr><br>

<form action="file.do" method="post" enctype="text">
    <fieldset>
        <legend>파일 임시 저장 위치</legend>	
        <br>
        <textarea id="message" name="absolutePath" rows="5" cols="150" readonly>${absolutePath}</textarea>
        <p><input type="submit" value="전사">&nbsp<input type="reset" value="취소"></p>
    </fieldset>
</form>

<br><hr><br>
<fieldset>
	<legend>Transcription 결과</legend>
	<br>
	<form action="summarize.do" method="post" enctype="text">
	    <textarea id="message" name="transcription_result" rows="20" cols="150">${result}</textarea>
	    <br>
	    <button type="submit">요약</button>&nbsp<input type="reset" value="취소">
  </form>
</fieldset>

</body>
</html>
