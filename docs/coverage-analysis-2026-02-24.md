# JaCoCo Test Coverage Analysis Report
Generated: 2026-02-24 20:27:36

---

## 1. OVERALL PROJECT COVERAGE SUMMARY

| Metric | Coverage | Status |
|--------|----------|--------|
| **Instructions** | 83% | ✓ Good |
| **Branches** | 69% | ⚠ Fair |
| **Lines** | 5,683 | ✓ Excellent |
| **Methods** | 1,407 | ✓ Excellent |
| **Classes** | 387 | ✓ Excellent |

**Raw Stats:**
- Total Instructions: 4,297 covered / 25,771 total
- Total Branches: 478 covered / 1,549 total

---

## 2. TOP 10 PACKAGES (Highest Instruction Coverage)

| Rank | Package | Instruction | Branch |
|------|---------|-------------|--------|
| 1 | com.cotalk.application.service.friend | 100% | 100% |
| 2 | com.cotalk.domain.validator | 100% | 100% |
| 3 | com.cotalk.application.service.admin | 100% | 100% |
| 4 | com.cotalk.domain.port.inbound.message | 100% | 100% |
| 5 | com.cotalk.adapter.inbound.rest.dto.profile | 100% | 100% |
| 6 | com.cotalk.adapter.inbound.rest.dto.report | 100% | 0% |
| 7 | com.cotalk.adapter.inbound.rest.dto.notification | 100% | 0% |
| 8 | com.cotalk.application.service.report | 100% | 100% |
| 9 | com.cotalk.domain.model | 100% | 100% |
| 10 | com.cotalk.adapter.inbound.rest.dto.user | 100% | 0% |

---

## 3. BOTTOM 10 PACKAGES (Lowest Instruction Coverage)

| Rank | Package | Instruction | Branch | Priority |
|------|---------|-------------|--------|----------|
| 1 | com.cotalk.infrastructure.messaging | 59% | 39% | HIGH |
| 2 | com.cotalk.adapter.outbound.persistence.message | 57% | 50% | HIGH |
| 3 | com.cotalk.domain.converter | 55% | 50% | MEDIUM |
| 4 | com.cotalk.adapter.inbound.websocket.dto | 40% | 0% | MEDIUM |
| 5 | com.cotalk (root) | 37% | 0% | LOW |
| 6 | com.cotalk.infrastructure.ratelimit | 25% | 14% | **CRITICAL** |
| 7 | com.cotalk.adapter.inbound.rest.dto.common | 15% | 0% | LOW |
| 8 | com.cotalk.infrastructure.persistence.converter | 13% | 7% | **CRITICAL** |
| 9 | com.cotalk.adapter.outbound.persistence.profile | 0% | 0% | CRITICAL |
| 10 | com.cotalk.infrastructure.util | 0% | 0% | LOW |

---

## 4. COVERAGE FOR CHANGED FILES (Latest Refactoring PR #121)

| Rank | Class | Instruction | Branch | Status |
|------|-------|-------------|--------|--------|
| 1 | GetReceivedFriendRequestsService | 100% | n/a | ✓ Excellent |
| 2 | GetSentFriendRequestsService | 100% | n/a | ✓ Excellent |
| 3 | HtmlSanitizer | 100% | 73% | ✓ Excellent |
| 4 | StrongPasswordValidator | 100% | 100% | ✓ Perfect |
| 5 | TermsController | 100% | 75% | ✓ Excellent |
| 6 | UserController | 91% | 100% | ✓ Good |
| 7 | FriendController | 91% | 66% | ✓ Good |
| 8 | MinioFileStorage | 90% | 83% | ✓ Good |
| 9 | FriendRequestRepositoryAdapter | 67% | n/a | ⚠ Fair |
| 10 | ConsoleEmailSender | 57% | n/a | ⚠ Needs Improvement |

**Aggregate Stats for Changed Files:**
- Average Instruction Coverage: 89.6%
- Average Branch Coverage: 82.8% (where available)
- Fully Covered Classes (100%): 5 of 10
- High Coverage (90%+): 8 of 10
- Low Coverage (<70%): 2 of 10

---

## 5. ASSESSMENT & KEY FINDINGS

### Strengths
✓ **Overall Instruction Coverage**: 83% meets industry standard  
✓ **Application Services**: 100% coverage for core business logic  
✓ **Domain Models**: Excellent coverage (100%) for value objects  
✓ **Changed Files Quality**: 89.6% average - well-tested refactoring  
✓ **Controllers**: 91-100% instruction coverage across REST endpoints  

### Weaknesses
⚠ **Branch Coverage**: 69% is below target - need more edge case testing  
⚠ **Infrastructure Layer**: Low coverage in messaging (59%), ratelimit (25%)  
⚠ **Converters/DTOs**: 0-13% branch coverage - critical data transformation paths  
⚠ **Two Changed Files**: ConsoleEmailSender (57%) and FriendRequestRepositoryAdapter (67%)  

### Critical Issues
🔴 **Rate Limiting**: 25% coverage - security/performance component undertested  
🔴 **Persistence Converters**: 13% coverage - data integrity risk  
🔴 **Profile Persistence**: 0% coverage - user data at risk  
🔴 **Utility Functions**: 0% coverage - support code untested  

---

## 6. RECOMMENDATIONS

### Priority 1: Immediate Action
1. **ConsoleEmailSender (57%)**
   - Add tests for error scenarios
   - Test email formatting logic
   - Test header/body construction

2. **FriendRequestRepositoryAdapter (67%)**
   - Add persistence layer tests
   - Test JPA entity mapping
   - Test query scenarios

### Priority 2: Medium Term
1. **Infrastructure Layer**
   - Add integration tests for messaging (59%)
   - Improve ratelimit test coverage (25% → 60%+)
   - Fix converter tests (13% → 50%+)

2. **Branch Coverage**
   - FriendController: Add tests for error branches (66% → 80%+)
   - HtmlSanitizer: Test null/empty input branches (73% → 85%+)
   - Add conditional branch tests across DTOs (0% → 30%+)

### Priority 3: Long Term
1. Achieve 85%+ instruction coverage target
2. Achieve 75%+ branch coverage target
3. Eliminate 0% coverage packages
4. Add performance/stress tests for messaging and ratelimit

---

## 7. CI/CD IMPACT

**Current Constraint**: Project requires 60% JaCoCo coverage
- ✓ **Current Status**: 83% - **23% ABOVE THRESHOLD**
- ✓ **Status**: PASSES CI checks

**Recommendation**: Consider raising threshold to 70% as codebase matures

---

## Files Included in Analysis
- Main Report: `/Users/nhn/Desktop/DEV/cursor-workspace/with-co-talk/co-talk/build/reports/jacoco/test/html/index.html`
- 72 packages analyzed
- 10 changed files deep-dived
