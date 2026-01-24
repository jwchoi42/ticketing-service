Frontend Specific Agent Rules
===

Core Tech Stack
---
- **Framework**: Next.js (App Router)
- **Styling**: Tailwind CSS
- **State Management**: React Query (TanStack Query), Zustand
- **Components**: Radix UI, Lucide Icons

Frontend Research & Documentation Rule
---
- **Search Process**: `frontend/` 폴더 관련 작업(에러 해결, 기능 구현 등) 수행 시 다음 순서를 엄격히 따릅니다.
  1. **Check Existing Skills**: 먼저 프로젝트 내 기존 스킬(특히 `vercel-react-best-practices` 등)을 검토합니다.
  2. **Official Docs Priority**: 기존 스킬에 내용이 없거나 최신 규격과 차이가 있을 경우, 반드시 **Next.js 공식 문서(nextjs.org/docs)**를 검색하여 최신 가이드를 따릅니다.
  3. **Update `my-next-js`**: 공식 문서를 통해 새롭게 알게 된 지식, 최신 규격, 또는 트러블슈팅 방법은 반드시 `frontend/.agent/skills/my-next-js/SKILL.md`에 정리하여 프로젝트 지식을 축적합니다.
- **Priority**: 사용자 정의 스킬(`my-next-js`)은 외부 스킬보다 우선순위가 높습니다.

UI/UX Principles
---
- **Mobile First**: 모든 디자인은 가로 폭 480px 이하의 모바일 환경을 최우선으로 고려합니다.
- **Aesthetics**: 모던하고 프리미엄한 디자인(Rich Gradients, Glassmorphism, Smooth Micro-animations)을 추구합니다.
- **Components**: 가능한 경우 기존 공통 컴포넌트를 재사용하며, 새로운 컴포넌트 생성 시 `components/` 하위 적절한 폴더에 위치시킵니다.
