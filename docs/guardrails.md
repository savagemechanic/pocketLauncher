# Guardrails

> This document is the constant contextual guide for all development in pocketLauncher.
> Every code change, refactor, feature, and review must pass through these guardrails.

---

## 1. Idiomatic Kotlin — No Exceptions

Write Kotlin the way the language designers intended. Every construct has a purpose; use the right one.

### Data & State
- **`data class`** for value objects. Never use a regular class when you need `equals`, `hashCode`, or `copy`.
- **`sealed class` / `sealed interface`** for representing finite state machines, UI states, and result types. Prefer `sealed interface` when you don't need shared state.
- **`value class`** (inline) for type-safe wrappers around primitives (`AppPackageName`, `SearchQuery`).
- **`object`** for true singletons. Never hand-roll a singleton pattern.
- **`enum class`** for fixed, exhaustive sets. Use `when` exhaustively — no `else` branch on enums or sealed types.

### Null Safety
- Leverage the type system. `String` means non-null; `String?` means nullable. Never use `!!` except in tests.
- Prefer `?.let {}`, `?:` (Elvis), and `?.run {}` over null checks with `if`.
- Design APIs to minimize nullability. Push null handling to the boundary, not through the core.

### Scope Functions
Use them idiomatically:
- `let` — transform nullable or scoped values
- `apply` — configure an object during construction
- `run` — execute a block on an object, return result
- `also` — side effects (logging, analytics)
- `with` — operate on an object without chaining

### Collections & Sequences
- Use `map`, `filter`, `flatMap`, `groupBy`, `associate`, `partition` — never manual loops for transformations.
- Use `Sequence` for large or lazily-evaluated chains (3+ operations on large collections).
- Destructuring: `for ((key, value) in map)`, `val (first, second) = pair`.

### Coroutines
- **Structured concurrency always.** Every coroutine must have a well-defined scope (`viewModelScope`, `lifecycleScope`, or a supervised scope you own).
- Use `Flow` for reactive streams. Prefer `StateFlow` and `SharedFlow` over `LiveData` in new code.
- `withContext(Dispatchers.IO)` for disk/network. Never block the main thread.
- Handle cancellation properly — check `isActive` in long loops, use `ensureActive()`.

### Extension Functions
- Use them to add behavior where inheritance is wrong.
- Keep them close to their usage. Don't create a `StringExtensions.kt` grab bag — put extensions in the file that uses them, or in a clearly scoped utility.

---

## 2. Clean Architecture

Every layer has a job. Nothing crosses boundaries without going through the proper interface.

```
UI (Fragments/Compose) → ViewModel → UseCase/Repository → DataSource
```

### Layer Responsibilities

| Layer | Owns | Depends On | Never Touches |
|-------|------|------------|---------------|
| **UI** | Views, Compose screens, navigation | ViewModel only | Repositories, database, network |
| **ViewModel** | UI state, user intent mapping | UseCases / Repositories | Context, Views, Fragments |
| **Domain** | Business logic, models | Nothing (pure Kotlin) | Android framework, database |
| **Data** | Persistence, caching, API calls | Domain interfaces | UI, ViewModel |

### Dependency Rule
Dependencies point **inward**. The domain layer has zero Android imports. If you need `Context` in business logic, you've made a design mistake — inject an interface instead.

### Repository Pattern
- One repository per data domain (`AppRepository`, `SettingsRepository`, `WidgetRepository`).
- Repository is the **single source of truth**. It decides whether to read from cache, database, or network.
- Expose `Flow<T>` from repositories, not raw data types.

### ViewModel Discipline
- ViewModels expose `StateFlow<UiState>` where `UiState` is a sealed interface or data class.
- No `MutableLiveData` exposed publicly. Use `private val _state` / `val state: StateFlow<T> = _state.asStateFlow()`.
- ViewModels never hold references to Views, Fragments, or Context.
- One public function per user action. Name it after the intent: `onSearchQueryChanged()`, `onAppSelected()`, `onSwipeGesture()`.

---

## 3. SOLID Principles — Applied, Not Academic

### Single Responsibility
If you can't describe what a class does in one sentence without "and", split it. `SettingsFragment` managing 157KB of UI is a known debt — new settings go into dedicated sub-fragments or Compose screens.

### Open/Closed
Extend behavior through sealed types and strategy patterns, not by modifying existing classes. Adding a new gesture? Add a case to the sealed class, not an `if` branch.

### Liskov Substitution
Every subtype must be usable wherever its parent type is expected. If your override throws `UnsupportedOperationException`, the design is wrong.

### Interface Segregation
Small, focused interfaces. `AppRepository` shouldn't force implementors to handle widgets. Separate concerns into separate contracts.

### Dependency Inversion
Depend on abstractions. ViewModels take repository interfaces, not concrete implementations. This is what makes testing possible.

---

## 4. Naming Conventions

Names are documentation. If you need a comment to explain what something is, rename it.

```kotlin
// Classes: PascalCase, noun phrases
class AppDrawerFragment
class VoiceCommandParser
data class SearchResult

// Functions: camelCase, verb phrases
fun launchApp(packageName: String)
fun parseVoiceCommand(input: String): VoiceCommand

// Properties: camelCase, noun phrases
val installedApps: StateFlow<List<AppModel>>
private val _searchQuery = MutableStateFlow("")

// Constants: SCREAMING_SNAKE_CASE
companion object {
    private const val MAX_RECENT_APPS = 10
    private const val SEARCH_DEBOUNCE_MS = 300L
}

// Packages: lowercase, no underscores
com.github.codeworkscreativehub.mlauncher.data.repository
com.github.codeworkscreativehub.mlauncher.ui.drawer
```

### Boolean Naming
Booleans read as questions: `isVisible`, `hasPermission`, `shouldShowDialog`, `canLaunch`.

### Callback Naming
Prefix with `on`: `onAppSelected`, `onSwipeDetected`, `onVoiceCommandReceived`.

---

## 5. Error Handling

### Use the Type System
```kotlin
// Prefer sealed results over exceptions for expected failures
sealed interface AppLaunchResult {
    data class Success(val app: AppModel) : AppLaunchResult
    data class NotFound(val query: String) : AppLaunchResult
    data class PermissionDenied(val reason: String) : AppLaunchResult
}
```

### Exception Rules
- **Never catch `Exception` or `Throwable`** generically. Catch specific exceptions.
- **Never swallow exceptions silently.** At minimum, log them.
- Use `runCatching` sparingly and only when you genuinely handle both paths.
- Coroutine exceptions flow through `CoroutineExceptionHandler` — set one up in every scope.

---

## 6. Testing

### What to Test
- **Domain logic**: 100% coverage goal. Pure Kotlin, no Android dependencies, fast.
- **ViewModels**: Test state transitions given user actions. Use `Turbine` for Flow testing.
- **Repositories**: Test caching logic, data source coordination.

### How to Test
```kotlin
// Given-When-Then structure
@Test
fun `search returns fuzzy matches when exact match not found`() {
    // Given
    val repository = FakeAppRepository(apps = listOf(mockApp("WhatsApp")))

    // When
    val results = repository.search("watsap")

    // Then
    assertThat(results).hasSize(1)
    assertThat(results.first().name).isEqualTo("WhatsApp")
}
```

- Use fakes over mocks. A `FakeAppRepository` is more readable and maintainable than a Mockito setup.
- Test names describe behavior: `` `function does X when Y` ``.
- No logic in tests. No `if`, no loops. Each test asserts one behavior.

---

## 7. Android-Specific Guardrails

### Fragment Lifecycle
- Observe data in `onViewCreated`, not `onCreateView`.
- Use `viewLifecycleOwner` for LiveData/Flow collection, never `this`.
- Access views only between `onViewCreated` and `onDestroyView`.

### Compose Integration
- New UI should prefer Compose. Existing Views are maintained but not extended.
- Compose screens use `collectAsStateWithLifecycle()` for Flow collection.
- No business logic in `@Composable` functions. They render state, nothing more.

### Resource Management
- Strings in `strings.xml`, never hardcoded. This is a launcher — it must be localizable.
- Dimensions in `dimens.xml` for values used more than once.
- Colors defined in theme, not inline. Support Material You dynamic theming.

### Performance
- This is a **launcher**. It must be fast. Target <100ms for home screen render.
- No work on the main thread that takes >16ms.
- Use `DiffUtil` for RecyclerView updates, never `notifyDataSetChanged()`.
- Profile before optimizing. Measure with Android Profiler, not assumptions.

---

## 8. Git & Code Review

### Commit Messages
```
type(scope): concise description

feat(voice): add wake word detection
fix(drawer): prevent crash on empty app list
refactor(settings): extract biometric logic to dedicated fragment
```

### PR Checklist
Before merging, every PR must satisfy:
- [ ] Compiles without warnings (treat warnings as errors in spirit)
- [ ] No `!!` outside of test code
- [ ] New public APIs have KDoc
- [ ] Follows the layer boundaries defined above
- [ ] No God classes — if a file exceeds 300 lines, justify it or split it
- [ ] No hardcoded strings in UI code

---

## 9. Architectural Debt Acknowledgment

The following are known violations of these guardrails that exist in the current codebase. They are not to be extended — only reduced:

- `MainViewModel` is oversized and handles too many concerns. New features must not add to it; extract dedicated ViewModels.
- `SettingsFragment` at 157KB is a monolith. New settings screens use Compose.
- Some `LiveData` usage remains. New reactive streams use `StateFlow`/`SharedFlow`.
- `Prefs` couples serialization with storage. New preferences go through a repository.

---

## 10. The Prime Directive

> Every line of code in this project should look like it was written by one senior Kotlin developer who cares deeply about clarity, correctness, and the user holding their phone.

When in doubt: **simpler is better, explicit is better, idiomatic is better.**
