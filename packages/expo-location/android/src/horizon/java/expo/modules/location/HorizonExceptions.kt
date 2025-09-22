package expo.modules.location

import expo.modules.kotlin.exception.CodedException

internal class QuestFeatureUnavailableException :
  CodedException("This feature is not supported on Meta Quest devices. Please use a supported device to access this functionality.")

internal class QuestPrebuildEnvironmentException :
  CodedException("This feature is not available in the Quest prebuild environment. To enable this feature on non-Quest devices, please remove the EXPO_HORIZON environment variable and rebuild your app.")
