# peanuts_backend

땅콩 백엔드

---
## 📚 STACKS
<div>
  <img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white">
  <img src="https://img.shields.io/badge/spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
  <img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
  <br>
  <img src="https://img.shields.io/badge/amazonaws-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white">
  <img src="https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github&logoColor=white">
  <img src="https://img.shields.io/badge/git-F05032?style=for-the-badge&logo=git&logoColor=white">
  <img src="https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">
</div>

---



## 📘 API 사용법 
**요청 메인 URL: `http://kongback.kro.kr:8080/`**

> 요청 URL 예시 `http://kongback.kro.kr:8080/user/{userKey}`

### WebSocket API

> websoket URL 예시 `ws://kongback.kro.kr:8080/ws/video`
>
---
## 📌 API 목록

### 🧍‍♂️사용자
## POST
+ 회원가입
    + `POST` : `http://kongback.kro.kr:8080/user/signup`
+ 로그인
    + `POST` : `http://kongback.kro.kr:8080/user/login`
+ 로그아웃
    + `POST` : `http://kongback.kro.kr:8080/{userEmail}`
## GET
+ 환자 정보 조회
    + `GET` : `http://kongback.kro.kr:8080/user/{userKey}/patients`
+ 비상상황 알림 히스토리
    + `GET` : `http://kongback.kro.kr:8080/user/alerts/{userKey}`
## PATCH
+ 주소수정
    + `PATCH` : `http://kongback.kro.kr:8080/user/address/{userKey}`
+ 전화번호 수정
    + `PATCH` : `http://kongback.kro.kr:8080/user/{userKey}/number`
+ 비밀번호 수정
    + `PATCH` : `http://kongback.kro.kr:8080/user/{userKey}/password`
## PUT
+ 환자 정보 수정
    + `PUT` : `http://kongback.kro.kr:8080/user/{userKey}/patients`
## DELETE

---
### 🧑‍💼 관리자
## POST
+ 관리자 회원가입
  +  `POST` : `http://kongback.kro.kr:8080/admin/signup`
+ 관리자 로그인
  +  `POST` : `http://kongback.kro.kr:8080/admin/login`
+ 관리자 로그아웃
  + `POST` : `http://kongback.kro.kr:8080/admin/logout/{adminId}`
## GET
+ 전체 사용자 목록 조회
  + `GET` : `http://kongback.kro.kr:8080/admin/users`
+ 특정 사용자 정보 조회
  + `GET` : `http://kongback.kro.kr:8080/admin/users/{userKey}`
+ 모든 사용자의 알림 히스토리 조회
  + `GET` : `http://kongback.kro.kr:8080/admin/users/alerts`
+ 특정 사용자의 알림 히스토리 조회
  + `GET` : `http://kongback.kro.kr:8080/admin/users/alerts/{userKey}`
## PATCH
## PUT
## DELETE
+ 특정 사용자 정보 삭제
  + `DELETE` : `http://kongback.kro.kr:8080/admin/users/{userKey}`
---
### WebSocket
+ 실시간 사용자의 영상 송출
  + 요청 URL : `ws://kongback.kro.kr:8080/ws/video`
+ 사용자의 실시간 영상 모니터링
  + 요청 URL : `ws://kongback.kro.kr:8080/admin/monitor`

  ```javascript
  작성예시
  const canvas = document.getElementById('videoCanvas');
  const ctx = canvas.getContext('2d');
  const ws = new WebSocket("ws://kongback.kro.kr:8080//ws/admin/monitor");

  ws.binaryType = "arraybuffer";

  ws.onmessage = function (event) {
      const blob = new Blob([event.data], { type: 'image/jpeg' });
      const img = new Image();
      img.onload = function () {
          canvas.width = img.width;
          canvas.height = img.height;
          ctx.drawImage(img, 0, 0);
      };
      img.src = URL.createObjectURL(blob);
  };
  
  <style>
    video, canvas {
        max-width: 100%;
        border: 1px solid #ccc;
    }
  </style>
  ```
