## 유형별 PR 템플릿

PR 생성 URL에 `?template=파일명.md`를 추가하면 해당 유형의 템플릿이 로드됩니다.

| 유형 | 파일명 | 사용법 |
|------|--------|--------|
| 기능 추가 | `feat.md` | `?template=feat.md` |
| 버그 수정 | `fix.md` | `?template=fix.md` |
| 리팩토링 | `refactor.md` | `?template=refactor.md` |
| 성능 개선 | `perf.md` | `?template=perf.md` |
| 문서 | `docs.md` | `?template=docs.md` |
| 설정/빌드/인프라 | `chore.md` | `?template=chore.md` |
| 테스트 | `test.md` | `?template=test.md` |

---

> 아래는 폴백용 기본 템플릿입니다. 위 유형별 템플릿 사용을 권장합니다.

## 변경 요약

<!-- 변경 사항을 간결하게 설명해 주세요 -->

## 변경 유형

- [ ] feat: 기능 추가
- [ ] fix: 버그 수정
- [ ] refactor: 리팩토링
- [ ] perf: 성능 개선
- [ ] docs: 문서
- [ ] chore: 설정/빌드/인프라
- [ ] test: 테스트

## 체크리스트

- [ ] 셀프 리뷰 완료
- [ ] CI 통과 확인
- [ ] 관련 문서 업데이트 (해당 시)
