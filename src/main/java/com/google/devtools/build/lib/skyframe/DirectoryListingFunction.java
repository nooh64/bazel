// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.skyframe;

import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import java.io.IOException;

import javax.annotation.Nullable;

/**
 * A {@link SkyFunction} for {@link DirectoryListingValue}s.
 */
final class DirectoryListingFunction implements SkyFunction {

  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws DirectoryListingFunctionException {
    RootedPath dirRootedPath = (RootedPath) skyKey.argument();

    FileValue dirFileValue = (FileValue) env.getValue(FileValue.key(dirRootedPath));
    if (dirFileValue == null) {
      return null;
    }

    RootedPath realDirRootedPath = dirFileValue.realRootedPath();
    if (!dirFileValue.isDirectory()) {
      // Recall that the directory is assumed to exist (see DirectoryListingValue#key).
      throw new DirectoryListingFunctionException(skyKey,
          new InconsistentFilesystemException(dirRootedPath.asPath()
              + " is no longer an existing directory. Did you delete it during the build?"));
    }

    DirectoryListingStateValue directoryListingStateValue;
    try {
      directoryListingStateValue = (DirectoryListingStateValue) env.getValueOrThrow(
          DirectoryListingStateValue.key(realDirRootedPath), IOException.class);
    } catch (IOException e) {
      throw new DirectoryListingFunctionException(skyKey, e);
    }
    if (directoryListingStateValue == null) {
      return null;
    }

    return DirectoryListingValue.value(dirRootedPath, dirFileValue, directoryListingStateValue);
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  /**
   * Used to declare all the exception types that can be wrapped in the exception thrown by
   * {@link DirectoryListingFunction#build}.
   */
  private static final class DirectoryListingFunctionException extends SkyFunctionException {
    public DirectoryListingFunctionException(SkyKey key, IOException e) {
      super(key, e);
    }

    public DirectoryListingFunctionException(SkyKey key, InconsistentFilesystemException e) {
      super(key, e);
    }
  }
}