# 로또 1등 연구소

재미로 하는 로또 번호 연구 앱입니다. 웹(휴대폰 브라우저)과 안드로이드 앱이 있습니다.

- **뽑기**: 랜덤·균형형·구간 분산·핫·콜드·핫+콜드·짝꿍·이월수·미지근·행운(이름·생일)·거울 번호, 세부 필터(홀짝·합계·연속·끝수·구간·직전 회차·저고), 고정수·제외수
- **통계**: 최근 당첨번호, 핫·콜드, 짝꿍 TOP 10, 번호 돋보기, 홀짝·구간·끝수·합계 분포, 번호별 출현 횟수
- **시뮬**: 내 조합을 역대 전 회차에 넣어보기, N년 동안 매주 샀다면, 1등 나올 때까지 사보기
- **내 번호**: 저장·직접 입력, 추첨 뒤 자동 등수 확인과 회수율 (이 기기에만 저장)
- **카페**: 연구 번호·주간 리포트·짝꿍 분석·역대 성적·시뮬 결과·당첨 확인 글을 만들어 복사하고 카페를 엽니다

## 처음 한 번 설정

1. 저장소 **Settings → Pages** 에서 Source를 `Deploy from a branch`, 브랜치 `main` / `(root)` 로 저장
2. **Actions → draws → Run workflow** 로 당첨번호를 처음 받기 (전 회차, 1분 안쪽)
3. `https://<아이디>.github.io/lotto-lab/` 접속 → 카페 글쓰기 탭에 카페 주소 입력

그 뒤로는 매주 토요일 밤 추첨이 끝나면 자동으로 당첨번호가 갱신됩니다.

## 안드로이드 앱

- 코드: `android/` (Kotlin · Jetpack Compose). `core` 는 통계·뽑기·시뮬 로직, `app` 은 화면
- `main` 에 `android/` 가 바뀌면 GitHub Actions가 빌드해서 [Releases → app-latest](https://github.com/chul4749/lotto-lab/releases/tag/app-latest) 에 `lotto-lab.apk` 를 올립니다
- 당첨번호는 앱에 같이 들어 있고, 켤 때마다 `https://chul4749.github.io/lotto-lab/draws.json` 에서 새 회차를 받습니다
- 로직 테스트: `cd android && ./gradlew :core:test`

로또는 매 회차 독립 추첨이라 지난 통계로 당첨 확률이 오르지는 않습니다. 재미로만 즐겨 주세요.
