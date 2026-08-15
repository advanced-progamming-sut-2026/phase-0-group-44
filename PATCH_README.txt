PVZ2 Phase 2 - Main Menu + Floating News patch

Apply:
  Extract this ZIP into the root of pvz2-main and overwrite the matching files.

Implemented:
  - Graphical MainMenuScreen inspired by Phase 2 Figure 1.
  - PVZ2 background/logo/banner and original UI icons extracted from the asset atlases already in the project.
  - Coins and gems shown in the main-menu top bar.
  - News button with an unread-count indicator.
  - Floating, modal, scrollable NewsDialog inspired by Figure 11.
  - Opening News marks displayed unread entries as read and refreshes the badge.
  - Main menu is mapped in PvzGame after successful login.
  - News model getters required by the graphical view.
  - Controller methods for unread count and mark-all-as-read.
  - Unit tests for the new News controller behavior.

Integration note:
  Adventure/Profile/Network/Settings/Leaderboard graphical screens are not present in this branch yet.
  Their Main Menu buttons are deliberately visible but currently show a temporary "not connected yet" toast,
  instead of changing Store to a menu whose graphical Screen does not exist.

Validation note:
  Java syntax/model checks were performed, but a full Gradle build could not be executed in the sandbox because
  the Gradle distribution/dependencies are not available offline here. Run ./gradlew test locally after applying.
