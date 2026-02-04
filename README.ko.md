# IssueLinker

[![Build](https://github.com/rojae/issueLinker/workflows/Build/badge.svg)](https://github.com/rojae/issueLinker/actions)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

[English](README.md)

<!-- Plugin description -->
**IssueLinker**는 Git 브랜치 이름에서 이슈 키를 자동으로 추출하여 Jira, GitHub Issues, GitLab Issues 등 이슈 트래커로 바로 연결해주는 IntelliJ IDEA 플러그인입니다.

설정 가능한 정규식 패턴으로 브랜치 이름을 파싱하고, 키보드 단축키 한 번으로 이슈 페이지를 열 수 있습니다.
<!-- Plugin description end -->

## 주요 기능

- **자동 이슈 감지** - 설정 가능한 정규식으로 Git 브랜치 이름에서 이슈 키 추출
- **원클릭 접근** - 단축키 `Cmd+Alt+J` (Mac) / `Ctrl+Alt+J` (Windows/Linux)로 이슈 페이지 열기
- **상태 바 위젯** - IDE 하단 상태 바에서 현재 이슈 키 확인
- **내부 브라우저** - IDE를 벗어나지 않고 IntelliJ 내장 브라우저에서 이슈 확인
- **유연한 설정** - 커스텀 URL 패턴으로 모든 이슈 트래커 지원
- **다중 캡처 그룹** - 정규식 그룹으로 브랜치 이름에서 여러 값 추출 가능

## 데모

```
브랜치: feature/PROJ-123-add-user-login
           ↓ (정규식으로 PROJ-123 추출)
열기: https://jira.company.com/browse/PROJ-123
```

## 설치 방법

### JetBrains Marketplace에서 설치 (권장)

1. IntelliJ IDEA 실행
2. **Settings/Preferences** → **Plugins** → **Marketplace** 이동
3. "IssueLinker" 검색
4. **Install** 클릭
5. IDE 재시작

### 수동 설치

1. [Releases](https://github.com/rojae/issueLinker/releases)에서 최신 버전 다운로드
2. **Settings/Preferences** → **Plugins** → **⚙️** → **Install Plugin from Disk...** 이동
3. 다운로드한 `.zip` 파일 선택
4. IDE 재시작

## 설정 방법

**Settings/Preferences** → **Tools** → **IssueLinker**로 이동

| 설정 | 설명 | 기본값 |
|------|------|--------|
| **Host URL** | 이슈 트래커 기본 URL | `https://jira.company.com` |
| **URL Path Pattern** | 캡처 그룹 플레이스홀더 `{0}`, `{1}`, `{2}`를 사용한 경로 패턴 | `/browse/{0}` |
| **Branch Parsing Regex** | 이슈 키 추출용 정규식 패턴 (캡처 그룹 사용) | `([A-Z][A-Z0-9]+-\d+)` |
| **Use Internal Browser** | IntelliJ 내장 브라우저에서 이슈 열기 | `true` |

### 설정 예시

#### Jira
```
Host URL: https://jira.yourcompany.com
URL Path Pattern: /browse/{0}
Branch Regex: ([A-Z][A-Z0-9]+-\d+)

브랜치: feature/PROJ-123-add-login → 열기: https://jira.yourcompany.com/browse/PROJ-123
```

#### GitHub Issues
```
Host URL: https://github.com
URL Path Pattern: /yourorg/yourrepo/issues/{0}
Branch Regex: (\d+)

브랜치: feature/123-add-login → 열기: https://github.com/yourorg/yourrepo/issues/123
```

#### GitLab Issues
```
Host URL: https://gitlab.com
URL Path Pattern: /yourgroup/yourproject/-/issues/{0}
Branch Regex: (\d+)

브랜치: 456-fix-bug → 열기: https://gitlab.com/yourgroup/yourproject/-/issues/456
```

#### Linear
```
Host URL: https://linear.app
URL Path Pattern: /yourteam/issue/{0}
Branch Regex: ([A-Z]+-\d+)

브랜치: feature/ENG-456-new-feature → 열기: https://linear.app/yourteam/issue/ENG-456
```

#### 다중 캡처 그룹 활용
```
Host URL: https://tracker.company.com
URL Path Pattern: /project/{0}/issue/{1}
Branch Regex: ([a-z]+)/([A-Z]+-\d+)

브랜치: feature/PROJ-123 → 열기: https://tracker.company.com/project/feature/issue/PROJ-123
```

## 사용 방법

### 키보드 단축키
| 플랫폼 | 단축키 |
|--------|--------|
| macOS | `Cmd + Alt + J` |
| Windows/Linux | `Ctrl + Alt + J` |

### 상태 바 위젯
IDE 하단 우측 상태 바에 표시되는 이슈 키를 클릭하면 해당 이슈 페이지가 열립니다.

### 컨텍스트 메뉴
에디터 또는 프로젝트 뷰에서 우클릭 후 **Open Issue in Browser** 선택

### Tools 메뉴
**Tools** → **Open Issue in Browser** 선택

## 요구 사항

- IntelliJ IDEA 2024.3 이상 (또는 다른 JetBrains IDE)
- Git 연동 활성화
- Java 21+ 런타임

## 소스에서 빌드하기

```bash
# 저장소 클론
git clone https://github.com/rojae/issueLinker.git
cd issueLinker

# 플러그인 빌드
./gradlew build

# 샌드박스 IDE에서 테스트 실행
./gradlew runIde

# 배포용 zip 파일 빌드
./gradlew buildPlugin
# 결과물: build/distributions/issueLinker-*.zip
```

## JetBrains Marketplace 배포 방법

### 1. 릴리스 준비

`gradle.properties`에서 버전 업데이트:
```properties
pluginVersion=1.0.0
```

### 2. 배포 파일 빌드

```bash
# IntelliJ IDEA를 먼저 종료한 후 실행:
./gradlew buildPlugin
```

플러그인 zip 파일이 `build/distributions/issueLinker-{version}.zip`에 생성됩니다.

### 3. Marketplace에 업로드

1. [JetBrains Marketplace](https://plugins.jetbrains.com/) 접속
2. JetBrains 계정으로 로그인
3. **Upload plugin** 클릭
4. `build/distributions/`의 zip 파일 업로드
5. 플러그인 정보 입력 후 리뷰 제출

### 4. 자동 배포 (CI/CD)

GitHub Secrets에 `PUBLISH_TOKEN`으로 JetBrains Marketplace 토큰 추가 후:

```bash
./gradlew publishPlugin
```

토큰은 [JetBrains Hub](https://hub.jetbrains.com/users/me?tab=authentification)에서 발급받을 수 있습니다.

## 기여하기

기여는 언제나 환영합니다! Pull Request를 자유롭게 제출해 주세요.

1. 저장소 Fork
2. 기능 브랜치 생성 (`git checkout -b feature/amazing-feature`)
3. 변경사항 커밋 (`git commit -m 'Add amazing feature'`)
4. 브랜치에 Push (`git push origin feature/amazing-feature`)
5. Pull Request 생성

## 라이선스

이 프로젝트는 MIT 라이선스로 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 감사의 글

- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template) 기반으로 제작
- 코드와 이슈 트래커 간의 빠른 이동에 대한 필요성에서 영감을 받아 개발

---

**Made with ❤️ by [rojae](https://github.com/rojae)**
