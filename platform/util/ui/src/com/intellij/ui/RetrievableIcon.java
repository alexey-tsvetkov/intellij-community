// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ui;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;


/**
 * An icon wrapping and painting another icon.
 */
public interface RetrievableIcon extends Icon {
  /**
   * Returns the wrapped icon.
   */
  @Nullable
  Icon retrieveIcon();
}