---
name: my-next-js
description: Next.js 공식 문서(nextjs.org)를 바탕으로 한 프로젝트 전용 최신 규격 및 트러블슈팅 가이드
author: user
priority: 1
---

# My Next.js Guide

프로젝트 진행 중 Next.js 공식 문서에서 확인된 최신 변경 사항과 자주 발생하는 에러 해결 방법을 정리합니다.

## 1. Metadata & Viewport 분리 (Next.js 14+)

### 개요
성능 최적화와 초기 렌더링 제어를 위해 `viewport` 관련 설정이 `metadata` 객체에서 분리되었습니다.

### 권장 패턴
```tsx
import type { Metadata, Viewport } from "next";

export const metadata: Metadata = {
  title: "...",
  description: "...",
  // viewport 속성은 여기서 제외
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
};
```

### 참고 링크
- [Official Viewport API Reference](https://nextjs.org/docs/app/api-reference/functions/generate-viewport)
- [Next.js 14 Release Blog](https://nextjs.org/blog/next-14#metadata-improvements)

---
*새로운 지식이 습득될 때마다 이 아래에 추가합니다.*
