# Profile Page Redesign Plan

This plan outlines the visual and functional improvements to the Profile Page while maintaining all existing logic and QuizMASTER branding.

## User Review Required

> [!NOTE]
> I will be adding logic to `ProfileActivity.kt` to actually calculate and display the statistics (Quizzes, Avg Score, Best Score) from the existing `HistoryManager`. Currently, these values are hardcoded in the layout.

## Proposed Changes

### [Layout & Design]

#### [MODIFY] [activity_profile.xml](file:///C:/Users/sonu%20kumar%20jha/AndroidStudioProjects/QuizMASTER/app/src/main/res/layout/activity_profile.xml)
- Wrap content in a `NestedScrollView` for better responsiveness.
- Redesign the header with a modern gradient and curved bottom.
- Enhance the profile avatar section with a white border and shadow.
- Create a premium-looking card for statistics with clear hierarchy.
- Add an "Account" section to display email elegantly.
- Add a "Progress" section with a visual indicator based on the user's average score.
- Update the Logout button to a modern full-width pill design.

#### [NEW] [bg_profile_header.xml](file:///C:/Users/sonu%20kumar%20jha/AndroidStudioProjects/QuizMASTER/app/src/main/res/drawable/bg_profile_header.xml)
- Create a custom drawable for the header background with a gradient and rounded bottom corners.

### [Logic]

#### [MODIFY] [ProfileActivity.kt](file:///C:/Users/sonu%20kumar%20jha/AndroidStudioProjects/QuizMASTER/app/src/main/java/com/example/quizmaster/ProfileActivity.kt)
- Bind the new statistics TextViews (`tvTotalQuizzes`, `tvAvgScore`, `tvBestScore`).
- Initialize `HistoryManager`.
- Calculate real statistics from the user's quiz history:
  - Total quizzes count.
  - Average percentage score.
  - Highest absolute score achieved.
- Bind the progress bar to the average score percentage.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.
- Verify that `HistoryManager` calculations handle empty history cases safely.

### Manual Verification
- Deploy to an emulator/device.
- Verify the layout on different screen sizes.
- Confirm the "Back" button functions correctly.
- Confirm "Logout" redirects to the Login screen.
- Verify that stats reflect the actual quiz history.
