# Git 컨벤션

다믿어? 백엔드 레포에서 쓰는 브랜치, 커밋, 이슈, PR 규칙입니다.

## 1. 브랜치 전략

```
main  ← 배포 브랜치 (push되면 자동 배포)
 └─ dev  ← 통합 브랜치 (모든 작업 PR이 여기로 감)
     └─ feat/…, fix/…, refactor/…, chore/…  ← 작업 브랜치
```

| 브랜치 | 용도 | 규칙 |
| --- | --- | --- |
| main | 배포 | dev → main PR로만 들어감. 담당자 한 명만 머지함 |
| dev | 개발 통합 | 작업 브랜치 PR로만 들어감. 직접 push 금지 |
| 작업 브랜치 | 기능 하나, 버그 하나 | dev에서 따고, dev로 PR 보냄. 머지 후 삭제 |

직접 push를 금지하는 이유: CI(테스트)는 dev와 main으로 가는 PR에서만 돌고, 배포는 main에 push될 때 일어난다. dev에 직접 push하면 테스트 없이 코드가 들어간다. 규칙만으로는 막히지 않으니 GitHub 브랜치 보호(PR 필수, CI 통과 필수)를 dev와 main에 켠다.

## 2. 브랜치 이름

{타입}/{이슈번호}-{짧은-영어-설명}

| 타입 | 언제 | 예시 |
| --- | --- | --- |
| feat | 새 기능, 새 API | feat/5-daily-answer |
| fix | 버그, 잘못된 동작, 문구 수정 | fix/12-streak-reset |
| refactor | 동작은 그대로, 구조만 정리 | refactor/15-answer-service |
| chore | 빌드, 설정, 배포, 템플릿, 의존성 | chore/1-project-setup |

- 소문자와 하이픈만 쓴다.
- 이슈가 있으면 번호를 꼭 넣는다. 이슈 없이 하는 작은 작업만 번호를 뺀다. 예: fix/typo-readme

## 3. 커밋 메시지

[{타입}] {한국어 요약} (#{이슈번호})

| 타입 | 내용 |
| --- | --- |
| feat | 기능 추가 |
| fix | 버그 수정 |
| test | 테스트 추가, 수정 |
| docs | README, 문서 |
| refactor | 동작 변화 없는 구조 정리 |
| chore | 설정, 빌드, 의존성, 배포 |

예시
```
[feat] 판 답 제출과 정답 공개 API 구현 (#5)
[test] 판 답 제출 API 테스트 추가 (#5)
[docs] README에 답 제출 API 안내 추가 (#5)
[fix] 자정 이후 데일리 연속 기록이 끊기던 문제 수정 (#12)
[chore] 스프링부트 프로젝트 초기 설정과 CI 추가 (#1)
```

규칙
- 타입은 대괄호로 감싼다. 예: [feat], [chore]
- 요약은 "~ 추가", "~ 구현", "~ 수정"처럼 끝내고 마침표는 안 찍는다.
- 코드, 테스트, 문서는 커밋을 나눈다. 한 PR 안에서 보통 feat → test → docs 순서가 된다.
- 커밋 하나는 그 시점에 빌드가 깨지지 않게 묶는다. 예: 엔티티 변경과 그걸 쓰는 코드는 같은 커밋에 넣는다.

## 4. 이슈

제목: [Feat] 기능 이름, [Fix] 문제 요약, [Refactor] 대상, [Chore] 작업
라벨: ✨ feature, 🐛 fix, ♻️ refactor, 🔧 chore
본문: 템플릿의 첫 칸(한 줄 요약)과 "👀 자세한 내용" 칸을 채운다. 자세한 내용은 4칸 형식으로 쓴다.

```
[작업 목적]
- 왜 하는지

[작업 내용]
- 무엇을 만들거나 고치는지

[완료 조건]
- 어떤 상태가 되면 끝인지

[참고 사항]
- 결정한 것, 다른 파트에 공유할 것
```

이슈 없이 해도 되는 것: 오타, 문구 한 줄, 데이터 몇 줄 추가처럼 작고 설명이 필요 없는 작업. 그래도 브랜치와 PR은 쓴다.

## 5. Pull Request

제목: 이슈 제목과 같게 쓴다. 예: [Feat] 판 답 제출과 정답 공개 API
본문: PR 템플릿 헤더를 그대로 살린다.

```
## ❗️ 관련 이슈 링크
Close #5

## 🔁 작업 내용
- 무엇을 바꿨는지 (API, 테이블, 규칙 단위로)
- 어떻게 확인했는지 (테스트, 실제 DB 적용 등)

## 📸 스크린샷 (Optional)

## 👀 기타 더 이야기해볼 점 (Optional)
- 이슈와 달라진 결정과 그 이유
- 다른 파트(FE, 다른 BE)가 알아야 할 변경
```

규칙
- Close #번호를 쓰면 머지할 때 이슈가 자동으로 닫힌다. 이슈가 없으면 "없음"이라고 쓴다.
- CI가 초록불이 된 다음에 머지한다.
- 머지 전에 dev가 앞서 나갔으면 작업 브랜치에서 git pull origin dev로 합치고, 충돌을 풀고, 테스트를 다시 돌린 뒤 push한다.
- 머지 방식은 Merge commit을 쓴다. 작업 브랜치의 커밋이 그대로 남아서 나중에 기능별로 추적하기 쉽다.
- 머지한 뒤 작업 브랜치는 삭제한다.

## 6. 작업 한 번의 명령어 흐름

시작
```
cd UniD_8_BE
git checkout dev
git pull origin dev
git checkout -b feat/5-daily-answer
```

마무리
```
cd UniD_8_BE
./gradlew test
git add src/main
git commit -m "[feat] 무엇을 구현 (#5)"
git add src/test
git commit -m "[test] 무엇 테스트 추가 (#5)"
git add README.md
git commit -m "[docs] README에 무엇 안내 추가 (#5)"
git push -u origin feat/5-daily-answer
```

push 전에 dev가 바뀐 경우
```
git pull origin dev
./gradlew test
git push
```

- zsh에서는 명령어 줄 끝에 # 주석을 붙이지 않는다.
- 테스트가 실패하면 커밋하지 않는다.

## 7. 레포에 넣지 않는 것

- 비밀값: .env, 키 파일(*.pem), API 키, DB 비밀번호. .env.example에는 키 이름만 넣는다.
- 배포 환경변수는 GitHub 시크릿(예: ENV_FILE)으로 관리한다.
- 빌드 결과물: build/, .gradle/, IDE 설정(.idea/)

## 8. 되돌리면 안 되는 것

- 이미 머지된 DB 마이그레이션 파일(V1__…, V2__…)은 수정하지 않는다. 한 글자라도 바뀌면 Flyway 체크섬이 달라져 서버가 시작을 못 한다. 고칠 게 있으면 새 번호 파일을 만든다.
- 공유 브랜치(dev, main)에는 push --force를 하지 않는다.
- 머지된 커밋을 고쳐야 하면 revert 커밋이나 새 수정 PR로 처리한다.
