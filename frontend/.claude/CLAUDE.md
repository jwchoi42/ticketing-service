---
trigger: always_on
---

Frontend Configuration
===

Core Personality & Tech Stack
---
- **Framework**: Next.js (App Router)
- **Styling**: Tailwind CSS
- **State Management**: React Query (TanStack Query), Zustand
- **Components**: Radix UI, Lucide Icons
- **Agent Rules**: Refer to [../.agent/AGENT.md](../.agent/AGENT.md) for detailed frontend-specific guidelines

Research & Documentation Protocol
---
**CRITICAL**: When working on frontend tasks (bug fixes, features, etc.), follow this strict order:
1. **Check Existing Skills**: Review project skills first (especially `.agent/skills/my-next-js`)
2. **Official Docs Priority**: If skills are outdated or incomplete, **MUST** search **Next.js official docs (nextjs.org/docs)** for latest guidance
3. **Update Knowledge**: New learnings from official docs **MUST** be added to `.agent/skills/my-next-js/SKILL.md`

**Priority**: User-defined skills (`.agent/skills/my-next-js`) override external skills.

UI/UX Principles
---
- **Mobile First**: All designs prioritize mobile (≤480px width)
- **Aesthetics**: Modern, premium design (Rich Gradients, Glassmorphism, Smooth Micro-animations)
- **Components**: Reuse existing shared components; new components go in appropriate `components/` subfolder

Project Resources
---
- **Frontend Agent Rules**: [../.agent/AGENT.md](../.agent/AGENT.md)
- **Next.js Knowledge Base**: [../.agent/skills/my-next-js/SKILL.md](../.agent/skills/my-next-js/SKILL.md)
- **Project Documentation**: [../../docs](../../docs) (refer to frontend-related sections)
