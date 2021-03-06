/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.idea.devkit.dom.impl;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.PropertiesReferenceManager;
import com.intellij.lang.properties.ResourceBundleReference;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Iconable;
import com.intellij.patterns.DomPatterns;
import com.intellij.patterns.XmlAttributeValuePattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.patterns.XmlTagPattern;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.dom.Extension;
import org.jetbrains.idea.devkit.dom.IdeaPlugin;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class I18nReferenceContributor extends PsiReferenceContributor {

  private static final String[] EXTENSION_TAG_NAMES = new String[]{
    "localInspection", "globalInspection",
    "configurable", "applicationConfigurable", "projectConfigurable"
  };

  private static final String[] TYPE_NAME_TAG = new String[]{"typeName"};
  private static final String INTENTION_ACTION_TAG = "intentionAction";
  private static final String INTENTION_ACTION_BUNDLE_TAG = "bundleName";

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registerKeyProviders(registrar);

    registerBundleNameProviders(registrar);
  }

  private static void registerKeyProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(extensionAttributePattern(EXTENSION_TAG_NAMES, "key", "groupKey"),
                                        new PropertyKeyReferenceProvider(false, "groupKey", "groupBundle"),
                                        PsiReferenceRegistrar.DEFAULT_PRIORITY);

    registrar.registerReferenceProvider(extensionAttributePattern(TYPE_NAME_TAG, "resourceKey"),
                                        new PropertyKeyReferenceProvider(false, "resourceKey", "resourceBundle"),
                                        PsiReferenceRegistrar.DEFAULT_PRIORITY);

    final XmlTagPattern.Capture intentionActionKeyTagPattern =
      XmlPatterns.xmlTag().withLocalName("categoryKey").
        withParent(DomPatterns.tagWithDom(INTENTION_ACTION_TAG, Extension.class));
    registrar.registerReferenceProvider(intentionActionKeyTagPattern,
                                        new PropertyKeyReferenceProvider(true, null, INTENTION_ACTION_BUNDLE_TAG));
  }

  private static void registerBundleNameProviders(PsiReferenceRegistrar registrar) {
    final PsiReferenceProvider bundleReferenceProvider = new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        return new PsiReference[]{new MyResourceBundleReference(element)};
      }
    };

    final XmlTagPattern.Capture resourceBundleTagPattern =
      XmlPatterns.xmlTag().withLocalName("resource-bundle").
        withParent(DomPatterns.tagWithDom(IdeaPlugin.TAG_NAME, IdeaPlugin.class));
    registrar.registerReferenceProvider(resourceBundleTagPattern, bundleReferenceProvider);

    XmlAttributeValuePattern bundlePattern = extensionAttributePattern(EXTENSION_TAG_NAMES, "bundle", "groupBundle");
    registrar.registerReferenceProvider(bundlePattern, bundleReferenceProvider,
                                        PsiReferenceRegistrar.DEFAULT_PRIORITY);

    XmlAttributeValuePattern typeNameBundlePattern = extensionAttributePattern(TYPE_NAME_TAG, "resourceBundle");
    registrar.registerReferenceProvider(typeNameBundlePattern, bundleReferenceProvider,
                                        PsiReferenceRegistrar.DEFAULT_PRIORITY);

    final XmlTagPattern.Capture intentionActionBundleTagPattern =
      XmlPatterns.xmlTag().withLocalName(INTENTION_ACTION_BUNDLE_TAG).
        withParent(DomPatterns.tagWithDom(INTENTION_ACTION_TAG, Extension.class));
    registrar.registerReferenceProvider(intentionActionBundleTagPattern, bundleReferenceProvider,
                                        PsiReferenceRegistrar.DEFAULT_PRIORITY);
  }

  private static XmlAttributeValuePattern extensionAttributePattern(String[] tagNames, String... attributeNames) {
    return XmlPatterns.xmlAttributeValue(attributeNames)
      .withSuperParent(2, DomPatterns.tagWithDom(tagNames, DomPatterns.domElement(Extension.class)));
  }


  private static class MyResourceBundleReference extends ResourceBundleReference implements EmptyResolveMessageProvider {

    private MyResourceBundleReference(PsiElement element) {
      super(element, false);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
      final Project project = myElement.getProject();
      PropertiesReferenceManager referenceManager = PropertiesReferenceManager.getInstance(project);
      final List<LookupElement> variants = new ArrayList<>();
      referenceManager.processPropertiesFiles(GlobalSearchScopesCore.projectProductionScope(project), (baseName, propertiesFile) -> {
        final Icon icon = propertiesFile.getContainingFile().getIcon(Iconable.ICON_FLAG_READ_STATUS);
        final String relativePath = ProjectUtil.calcRelativeToProjectPath(propertiesFile.getVirtualFile(), project);
        variants.add(LookupElementBuilder.create(propertiesFile, baseName)
                       .withIcon(icon)
                       .withTailText(" (" + relativePath + ")", true));
        return true;
      }, this);
      return variants.toArray(LookupElement.EMPTY_ARRAY);
    }

    @NotNull
    @Override
    public String getUnresolvedMessagePattern() {
      return "Cannot resolve property bundle";
    }
  }
}