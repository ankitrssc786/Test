/*
 * The MIT License
 * Copyright © 2014-2019 Ilkka Seppälä
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.test.caching;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheStore.class);

  private static LruCache cache;

  private CacheStore() {
  }


  public static void initCapacity(int capacity) {
    if (cache == null) {
      cache = new LruCache(capacity);
    } else {
      cache.setCapacity(capacity);
    }
  }

  public static UserAccount readThrough(String userId) {
    if (cache.contains(userId)) {
      LOGGER.info("# Cache Hit!");
      return cache.get(userId);
    }
    LOGGER.info("# Cache Miss!");
    UserAccount userAccount = DbManager.readFromDb(userId);
    cache.set(userId, userAccount);
    return userAccount;
  }

  public static void writeThrough(UserAccount userAccount) {
    if (cache.contains(userAccount.getUserId())) {
      DbManager.updateDb(userAccount);
    } else {
      DbManager.writeToDb(userAccount);
    }
    cache.set(userAccount.getUserId(), userAccount);
  }

  public static void writeAround(UserAccount userAccount) {
    if (cache.contains(userAccount.getUserId())) {
      DbManager.updateDb(userAccount);
      cache.invalidate(userAccount.getUserId());
    } else {
      DbManager.writeToDb(userAccount);
    }
  }

  public static UserAccount readThroughWithWriteBackPolicy(String userId) {
    if (cache.contains(userId)) {
      return cache.get(userId);
    }
    UserAccount userAccount = DbManager.readFromDb(userId);
    if (cache.isFull()) {
      UserAccount toBeWrittenToDb = cache.getLruData();
      DbManager.upsertDb(toBeWrittenToDb);
    }
    cache.set(userId, userAccount);
    return userAccount;
  }

  public static void writeBehind(UserAccount userAccount) {
    if (cache.isFull() && !cache.contains(userAccount.getUserId())) {
      UserAccount toBeWrittenToDb = cache.getLruData();
      DbManager.upsertDb(toBeWrittenToDb);
    }
    cache.set(userAccount.getUserId(), userAccount);
  }

  public static void clearCache() {
    if (cache != null) {
      cache.clear();
    }
  }

  public static void flushCache() {
    LOGGER.info("# flushCache...");
    Optional.ofNullable(cache)
        .map(LruCache::getCacheDataInListForm)
        .orElse(List.of())
        .forEach(DbManager::updateDb);
  }

  public static String print() {
    return Optional.ofNullable(cache)
        .map(LruCache::getCacheDataInListForm)
        .orElse(List.of())
        .stream()
        .map(userAccount -> userAccount.toString() + "\n")
        .collect(Collectors.joining("", "\n--CACHE CONTENT--\n", "----\n"));
  }

  public static UserAccount get(String userId) {
    return cache.get(userId);
  }

  public static void set(String userId, UserAccount userAccount) {
    cache.set(userId, userAccount);
  }

  public static void invalidate(String userId) {
    cache.invalidate(userId);
  }
}
