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
        function submitTimestampForm() {
            var searchforInput = document.getElementById('searchforInput');
            var pythonPath = document.getElementById('pythonPath');
            var form = document.getElementById('uploadForm');

            form.elements['searchfor'].value = searchforInput.value;
            form.elements['pythonPath'].value = pythonPath.value;
            form.action = 'timestamp.do';

            form.submit();
        }
        document.querySelector('form').addEventListener('submit', function (e) {
            e.preventDefault();
            var formData = new FormData(this);
            
            var buttonClicked = document.activeElement.value;

            var endpoint;
            if (buttonClicked === '타임스탬프 추출') {
                var searchQuery = prompt("Enter search query for timestamp extraction:");
                formData.append('searchfor', searchQuery);
                endpoint = '/timestamp.do';
            } else if (buttonClicked === '요약') {
                endpoint = '/summarize_vid.do';
            } else if (buttonClicked === '태그 추출') {
                endpoint = '/extract-tag.do';
            } else {
                console.error('Unknown button clicked:', buttonClicked);
                return;
            }
            fetch(endpoint, {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                // JSON 데이터를 처리하고 웹 페이지에 표시
                var resultDiv = document.querySelector('#result');
                resultDiv.style.display = 'block'; // 결과 보이기
                resultDiv.textContent = JSON.stringify(data, null, 2);
            })
            .catch(error => {
                console.error('에러 발생:', error);
            });
        });
    </script>
</head>
<body>

<form method="post" enctype="multipart/form-data" id="uploadForm">
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
        <p>
        	<input type="submit" value="요약" onclick="javascript: form.action='summarize_vid.do'">
        	&nbsp<input type="submit" value="태그 추출" onclick="javascript: form.action='extract-tag.do'">
        	&nbsp<input type="submit" value="설치 위치" onclick="javascript: form.action='install-guide.do'">
        </p>
        <p>
            검색 내용 : <input type="text" name="searchfor" id="searchforInput">
            파이썬 경로 : <input type="text" name="locOfPython" id="pythonPath">
            <input type="button" value="타임스탬프 추출" onclick="submitTimestampForm()">
        </p>
        <p><input type="reset" value="취소"></p>
    </fieldset>
</form>
<br><hr><br>

<div id="result" style="display: none;">
    <textarea rows="10" cols="50"></textarea>
</div>

</body>
</html>
