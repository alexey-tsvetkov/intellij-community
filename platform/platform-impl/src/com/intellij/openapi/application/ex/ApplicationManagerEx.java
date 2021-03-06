// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.ex;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

public final class ApplicationManagerEx extends ApplicationManager {
  public static final String IDEA_APPLICATION = "idea";

  public static ApplicationEx getApplicationEx() {
    return (ApplicationEx)ourApplication;
  }

  public static boolean isAppLoaded() {
    Application app = ourApplication;
    return app != null && ((ApplicationEx)app).isLoaded();
  }
}